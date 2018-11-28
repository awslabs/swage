/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.swage.metrics.record.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.Unit;
import software.amazon.swage.metrics.record.MetricRecorder;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MetricRecorder that sends all metric events to CloudWatch.
 * <p>
 * This recorder attempts to adhere to CloudWatch best practices by aggregating
 * metrics client side and publishing periodically to CloudWatch in batches.
 * <p>
 * Metrics are aggregated by metric name, unit, and context id.
 * Calls to {@link #record} and {@link #count} with the same {@link Metric}
 * label, the same {@link Unit}, and the same Context.ID value will be added
 * together to form a single {@link StatisticSet}. The timestamps of individual
 * metric events are lost in the aggregation.
 * <p>
 * The aggregated metrics are periodically collected and published to
 * CloudWatch using the client provided in the constructor. The publishing
 * period is configurable at construction time, defaulting to 60 seconds.
 * <p>
 * CloudWatch does not recommend publishing at frequencies less than 1 minute,
 * since they don't do aggregation at any granularity smaller than that.
 * However, publishing at frequencies greater than 1 minute will cause 1 minute
 * aggregates to be lost. As such it is highly recommended that a value of 60
 * be used for the publish frequency
 * <p>
 * A random jitter is applied for the initial delay, bounded by maxJitter.
 * This helps prevent all of the hosts/processes in an application from
 * publishing at the same instant, spreading the requests more evenly over
 * time.
 * <p>
 * Note that we use a single thread executor to perform the publishing. That
 * means if a single publish attempt takes a long time, i.e., because of a lot
 * of metrics or because of retries due to throttling/outage, subsequent
 * attempts will be delayed. This is generally a good thing, especially if the
 * delay is due to throttling. During the delay, {@link CloudWatchRecorder}
 * will continue to collect metrics and add values to be aggregated so that
 * subsequent calls will have more metrics aggregated so we can make fewer
 * calls to CloudWatch.
 */
public class CloudWatchRecorder extends MetricRecorder<MetricRecorder.RecorderContext> {

    private static final Logger log = LogManager.getLogger(CloudWatchRecorder.class);

    // Count of metric datum to batch together in one CloudWatch call
    private static final int BATCH_SIZE = 20;

    // How long to wait for graceful shutdown before axing the publish thread
    private static final long SHUTDOWN_TIMEOUT = 60 * 1000;

    // Maximum bound for random jitter at startup, in seconds
    private static final int DEFAULT_JITTER = 60;

    // How often to publish to CloudWatch, in seconds
    private static final int DEFAULT_PUBLISH_FREQ = 60;

    // Dimensions to use for metrics if no custom mapping is provided
    private static final DimensionMapper DEFAULT_DIMENSIONS;
    static {
        DEFAULT_DIMENSIONS = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();
    }

    private final AmazonCloudWatch metricsClient;
    private final String namespace;

    private final ScheduledExecutorService publishExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final MetricDataAggregator aggregator;

    public static final class Builder {
        private String namespace;
        private boolean autoShutdown = false;
        private AmazonCloudWatch client;
        private DimensionMapper dimensionMapper = DEFAULT_DIMENSIONS;
        private int maxJitter = DEFAULT_JITTER;
        private int publishFrequency = DEFAULT_PUBLISH_FREQ;
        private ScheduledExecutorService scheduledExecutorService;

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder dimensionMapper(DimensionMapper dimensionMapper) {
            this.dimensionMapper = dimensionMapper;
            return this;
        }

        public Builder client(AmazonCloudWatch client) {
            this.client = client;
            return this;
        }

        public Builder maxJitter(int maxJitter) {
            this.maxJitter = maxJitter;
            return this;
        }

        public Builder publishFrequency(int publishFrequency) {
            this.publishFrequency = publishFrequency;
            return this;
        }

        public Builder autoShutdown(boolean autoShutdown) {
            this.autoShutdown = autoShutdown;
            return this;
        }

        public Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        public CloudWatchRecorder build() {
            if (scheduledExecutorService == null) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
            if (client == null) {
                client = AmazonCloudWatchClientBuilder.defaultClient();
            }
            CloudWatchRecorder recorder = new CloudWatchRecorder(
                    client,
                    namespace,
                    maxJitter,
                    publishFrequency,
                    dimensionMapper,
                    scheduledExecutorService
            );

            if (this.autoShutdown) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    recorder.shutdown();
                }));
            }

            return recorder;
        }
    }

    /**
     * Convenience factory method to create a CloudWatchRecorder and register a JVM
     * shutdown hook to automatically shut it down on application exit.
     * The created recorder will use its own internal client to CloudWatch,
     * using the default jitter and publish settings,  and a default dimension
     * mapping that includes just the metric context ID.
     *
     * @param namespace CloudWatch namespace to publish under.
     * @return A CloudWatchRecorder instance that will automatically shutdown on JVM exit.
     */
    public static final CloudWatchRecorder withAutoShutdown(
            final String namespace)
    {
        return new Builder()
                .autoShutdown(true)
                .namespace(namespace)
                .build();
    }

    /**
     * Convenience factory method to create a CloudWatchRecorder and register a JVM
     * shutdown hook to automatically shut it down on application exit.
     * The created recorder will use its own internal client to CloudWatch,
     * using the default jitter and publish settings.
     *
     * @param namespace CloudWatch namespace to publish under.
     * @param dimensionMapper Configuration specifying which attributes to sent
     *                        to CloudWatch for each metric event.
     * @return A CloudWatchRecorder instance that will automatically shutdown on JVM exit.
     */
    public static final CloudWatchRecorder withAutoShutdown(
            final String namespace,
            final DimensionMapper dimensionMapper)
    {
        return new Builder()
                .autoShutdown(true)
                .namespace(namespace)
                .dimensionMapper(dimensionMapper)
                .build();
    }

    /**
     * Create a new recorder instance.
     * This recorder will periodically send aggregated metric events to CloudWatch
     * via the provided client.
     *
     * The {@link #shutdown} method must be called when the application is done
     * with the recorder in order to flush and stop the reporting thread.
     *
     * This is equivalent to constructing a CloudWatch recorder with a
     * maxJitter of 60, a publish frequency of 60, and a default dimension
     * mapping that includes just the metric context ID.
     *
     * @param client Client to use for connecting to CloudWatch
     * @param namespace CloudWatch namespace to publish under
     */
    public CloudWatchRecorder(
            final AmazonCloudWatch client,
            final String namespace)
    {
        this(client, namespace, DEFAULT_JITTER, DEFAULT_PUBLISH_FREQ, DEFAULT_DIMENSIONS);
    }

    /**
     * Create a new recorder instance.
     * This recorder will periodically send aggregated metric events to CloudWatch
     * via the provided client. Requests will be queued and sent using a
     * single-threaded ScheduledExecutorService every publishFrequency (in seconds).
     * The initial submission to CloudWatch will be delayed by a random amount
     * on top of the publish frequency, bounded by maxJitter.
     *
     * The {@link #shutdown} method must be called when the application is done
     * with the recorder in order to flush and stop the reporting thread.
     *
     * @param client Client to use for connecting to CloudWatch
     * @param namespace CloudWatch namespace to publish under
     * @param maxJitter Maximum delay before counting publish frequency for
     *                  initial request, in seconds.
     *                  A value of 0 will provide no jitter.
     * @param publishFrequency Batch up and publish at this interval, in seconds.
     *                         Suggested value of 60, for one minute aggregation.
     * @param dimensionMapper Configuration specifying which attributes to sent
     *                        to CloudWatch for each metric event.
     */
    public CloudWatchRecorder(
            final AmazonCloudWatch client,
            final String namespace,
            final int maxJitter,
            final int publishFrequency,
            final DimensionMapper dimensionMapper) {
        this(
                client,
                namespace,
                maxJitter,
                publishFrequency,
                dimensionMapper,
                Executors.newSingleThreadScheduledExecutor()
        );
    }

    /**
     * Create a new recorder instance.
     * This recorder will periodically send aggregated metric events to CloudWatch
     * via the provided client. Requests will be queued and sent using a
     * single-threaded ScheduledExecutorService every publishFrequency (in seconds).
     * The initial submission to CloudWatch will be delayed by a random amount
     * on top of the publish frequency, bounded by maxJitter.
     *
     * The {@link #shutdown} method must be called when the application is done
     * with the recorder in order to flush and stop the reporting thread.
     *
     * @param client Client to use for connecting to CloudWatch
     * @param namespace CloudWatch namespace to publish under
     * @param maxJitter Maximum delay before counting publish frequency for
     *                  initial request, in seconds.
     *                  A value of 0 will provide no jitter.
     * @param publishFrequency Batch up and publish at this interval, in seconds.
     *                         Suggested value of 60, for one minute aggregation.
     * @param dimensionMapper Configuration specifying which dimensions to sent
     *                        to CloudWatch for each metric event.
     * @param scheduledExecutorService Executor to schedule metric publishing at a fixed rate.
     */
    public CloudWatchRecorder(
            final AmazonCloudWatch client,
            final String namespace,
            final int maxJitter,
            final int publishFrequency,
            final DimensionMapper dimensionMapper,
            final ScheduledExecutorService scheduledExecutorService)
    {
        if (client == null) {
            throw new IllegalArgumentException("AmazonCloudWatch must be provided");
        }
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must be provided");
        }
        if (dimensionMapper == null) {
            throw new IllegalArgumentException("DimensionMapper must be provided");
        }

        this.metricsClient = client;
        this.namespace = namespace;
        this.publishExecutor = scheduledExecutorService;
        this.aggregator = new MetricDataAggregator(dimensionMapper);

        start(maxJitter, publishFrequency);
    }

    private void start(final int maxJitter, final int publishFrequencySeconds) {
        this.running.set(true);

        int jitter = (maxJitter == 0) ? 0 : ThreadLocalRandom.current().nextInt(maxJitter);

        int initialDelay = (jitter * 1000) + (publishFrequencySeconds * 1000);

        publishExecutor.scheduleAtFixedRate(this::sendAggregatedData,
                                            initialDelay,
                                            publishFrequencySeconds * 1000,
                                            TimeUnit.MILLISECONDS);
    }

    /**
     * Signal that the recorder should shutdown.
     * Queued up metric events will be flushed, and this method will block
     * until all pending ones are sent or it loses patience and times out.
     *
     * Any new metric record/count calls on this recorder after shutdown is
     * called will be ignored. Some calls to record/count that are in flight
     * may be dropped if this executes while in the middle of the call.
     *
     */
    public void shutdown() {
        boolean wasRunning = running.getAndSet(false);
        if (!wasRunning){
            // shutdown already called
            return;
        }

        // Schedule one last flush to make sure everything gets sent; this will
        // either execute immediately or queue up behind an in-flight flush
        // (and be cancelled). Worst case it gets executed immediately after
        // a previous one completes, which is fine.
        publishExecutor.execute(this::sendAggregatedData);

        // And shut down the publish thread, waiting to make sure our last
        // flush executes.
        publishExecutor.shutdown();
        try {
            publishExecutor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            publishExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected RecorderContext newRecorderContext(TypedMap attributes) {
        return new RecorderContext(attributes);
    }

    @Override
    protected void record(
            final Metric label,
            final Number value,
            final Unit unit,
            final Instant time,
            final RecorderContext context)
    {
        if (!running.get()) {
            log.debug("record called on shutdown recorder");
            //TODO: something besides silently ignore, perhaps IllegalStateException?
            return;
        }

        // Metric events will be aggregated, with the individual time of each
        // event lost. Rather than having one timestamp apply to all, we just
        // drop the time information and use the timestamp of aggregation.

        aggregator.add(context,
                       label,
                       value.doubleValue(),
                       unitMapping.get(unit));
    }

    @Override
    public void count(final Metric label, final long delta, final RecorderContext context)
    {
        if (!running.get()) {
            log.debug("count called on shutdown recorder");
            //TODO: something besides silently ignore, perhaps IllegalStateException?
            return;
        }

        aggregator.add(context,
                       label,
                       Long.valueOf(delta).doubleValue(),
                       StandardUnit.Count);
    }

    //TODO: stop propagating new Unit abstractions everywhere
    private static final Map<Unit, StandardUnit> unitMapping = new HashMap<>();
    static {
        unitMapping.put(Unit.SECOND, StandardUnit.Seconds);
        unitMapping.put(Unit.MILLISECOND, StandardUnit.Milliseconds);
        unitMapping.put(Unit.MICROSECOND, StandardUnit.Microseconds);
        unitMapping.put(Unit.BYTE, StandardUnit.Bytes);
        unitMapping.put(Unit.KILOBYTE, StandardUnit.Kilobytes);
        unitMapping.put(Unit.MEGABYTE, StandardUnit.Megabytes);
        unitMapping.put(Unit.GIGABYTE, StandardUnit.Gigabytes);
        unitMapping.put(Unit.TERABYTE, StandardUnit.Terabytes);
        unitMapping.put(Unit.BIT, StandardUnit.Bits);
        unitMapping.put(Unit.KILOBIT, StandardUnit.Kilobits);
        unitMapping.put(Unit.MEGABIT, StandardUnit.Megabits);
        unitMapping.put(Unit.GIGABIT, StandardUnit.Gigabits);
        unitMapping.put(Unit.TERABIT, StandardUnit.Terabits);
        unitMapping.put(Unit.PERCENT, StandardUnit.Percent);
        unitMapping.put(Unit.BYTE_PER_SEC, StandardUnit.BytesSecond);
        unitMapping.put(Unit.KB_PER_SEC, StandardUnit.KilobytesSecond);
        unitMapping.put(Unit.MB_PER_SEC, StandardUnit.MegabytesSecond);
        unitMapping.put(Unit.GB_PER_SEC, StandardUnit.GigabytesSecond);
        unitMapping.put(Unit.TB_PER_SEC, StandardUnit.TerabytesSecond);
        unitMapping.put(Unit.BIT_PER_SEC, StandardUnit.BitsSecond);
        unitMapping.put(Unit.KBIT_PER_SEC, StandardUnit.KilobitsSecond);
        unitMapping.put(Unit.MBIT_PER_SEC, StandardUnit.MegabitsSecond);
        unitMapping.put(Unit.GBIT_PER_SEC, StandardUnit.GigabitsSecond);
        unitMapping.put(Unit.TBIT_PER_SEC, StandardUnit.TerabitsSecond);
        unitMapping.put(Unit.PER_SEC, StandardUnit.CountSecond);
        unitMapping.put(Unit.NONE, StandardUnit.None);
    }

    private void sendAggregatedData() {

        // Grab all the current aggregated attributes, resetting
        // the aggregator to empty in the process
        List<MetricDatum> metricData = aggregator.flush();
        if(metricData.isEmpty()) {
            return;
        }

        // Send the attributes in batches to adhere to CloudWatch limitation on the
        // number of MetricDatum objects per request, see:
        // http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_limits.html
        int begin = 0;
        while(begin < metricData.size()) {
            int end = begin + BATCH_SIZE;

            sendData(metricData.subList(begin, Math.min(end, metricData.size())));

            begin = end;
        }

    }

    //TODO: ensure "Each PutMetricData request is limited to 40 KB in size for HTTP POST requests."
    private void sendData( Collection<MetricDatum> metricData ) {
        PutMetricDataRequest request = new PutMetricDataRequest();
        request.setNamespace( namespace );
        request.setMetricData( metricData );
        metricsClient.putMetricData( request );
    }

}
