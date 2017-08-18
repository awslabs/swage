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
package software.amazon.swage.metrics.jmx;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.StandardContext;
import software.amazon.swage.metrics.jmx.sensor.SenseException;
import software.amazon.swage.metrics.jmx.sensor.Sensor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides MBeans integration for metrics, polling a set of M(X)Beans and
 * sending data to the configured metric sink.
 * This provides a mechanism to expose periodic health and utilization statistics in your
 * standard metrics logging.
 *
 * MXBeanPoller will poll a set of {@code Sensor} objects at a specified
 * interval and record the measurements.  Each Sensor is associated with an
 * MBean, specialized to read and interpret the values on the bean.
 *
 * Sensors are not required to pull data from MBeans, but are designed to do so.
 *
 */
class MXBeanPoller {

    private static final Logger log = LogManager.getLogger(MXBeanPoller.class);


    // How long to wait for graceful shutdown before axing the sensor thread, in milliseconds
    private static final long SHUTDOWN_TIMEOUT = 60 * 1000;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor;

    private final TypedMap updaterContext;
    private final MetricRecorder metricRecorder;
    private final List<Sensor> sensors;

    /**
     * Convenience factory method to create an MXBeanPoller and register a JVM
     * shutdown hook to automatically shut it down on application exit.
     * The created poller will use its own default scheduled executor.
     *
     * @param metricRecorder the metrics recorder to record measurements against
     * @param pollingIntervalSeconds the number of seconds between measurements. Must be positive.
     * @param sensors the set of sensors to measure at each interval
     * @return an instance of MXBeanPoller that will have {@link #shutdown} called on JVM exit
     */
    public static final MXBeanPoller withAutoShutdown(
            final MetricRecorder metricRecorder,
            int pollingIntervalSeconds,
            List<Sensor> sensors)
    {
        MXBeanPoller updater = new MXBeanPoller(
                metricRecorder,
                pollingIntervalSeconds,
                sensors);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            updater.shutdown();
        }));

        return updater;
    }

    /**
     * Instantiates an MXBeanPoller.
     *
     * @param metricRecorder the metrics recorder to record measurements against
     * @param pollingIntervalSeconds the number of seconds between measurements. Must be positive.
     * @param sensors the set of sensors to measure at each interval
     */
    public MXBeanPoller(final MetricRecorder metricRecorder,
                        final int pollingIntervalSeconds,
                        final List<Sensor> sensors)
    {
        this(Executors.newSingleThreadScheduledExecutor(), metricRecorder, pollingIntervalSeconds, sensors);
    }

    /**
     * Instantiates an MXBeanPoller which uses the provided executor.
     * The passed executor will be shutdown when this MXBeanPoller is shutdown,
     * so be careful when sharing executors across concerns.
     *
     * @param executor
     * @param metricRecorder the metrics recorder to record measurements against
     * @param pollingIntervalSeconds the number of seconds between measurements. Must be positive.
     * @param sensors the set of sensors to measure at each interval
     */
    public MXBeanPoller(final ScheduledExecutorService executor,
                        final MetricRecorder metricRecorder,
                        final int pollingIntervalSeconds,
                        final List<Sensor> sensors)
    {
        this.executor = executor;
        this.metricRecorder = metricRecorder;
        this.sensors = Collections.unmodifiableList(new ArrayList<>(sensors));

        // Name of the JVM, as stand-in for PID
        final String runtimeName = ManagementFactory.getRuntimeMXBean().getName();

        // Set up the context to use for the recorded metric events.
        // Each sensor may add additional context
        TypedMap contextData = ContextData.withId(runtimeName)
                                          .add(StandardContext.OPERATION, "JMX")
                                          .build();

        for (Sensor s : sensors) {
            contextData = s.addContext(contextData);
        }
        this.updaterContext = contextData;
        // Context set

        this.running.set(true);
        this.executor.scheduleAtFixedRate(
                () -> runSensors(),
                0,
                pollingIntervalSeconds,
                TimeUnit.SECONDS);
    }

    public void shutdown() {
        running.set(false);

        // Shut down the publish thread, waiting for in flight sensors to finish.
        executor.shutdown();
        try {
            executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }


    private void runSensors() {

        //TODO: should there be per-emit context data?
        //TODO: or should we have one Context and only close at the very end?
        MetricRecorder.Context metricContext = metricRecorder.context(this.updaterContext);

        //TODO: emit each sensor on its own scheduled thread instead of all synchronously?
        for(Sensor sensor : sensors) {
            try {
                sensor.sense(metricContext);
            } catch (SenseException e) {
                // Problem with particular sensor, continue on to next
                log.warn("Sensor {} failure", sensor.getClass().getCanonicalName(), e);
            }
        }

        metricContext.close();
    }


}
