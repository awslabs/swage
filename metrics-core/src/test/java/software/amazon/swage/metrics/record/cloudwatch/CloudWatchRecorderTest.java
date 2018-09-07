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

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 */
public class CloudWatchRecorderTest {

    private static final Metric M_TIME = Metric.define("Time");
    private static final Metric M_PERC = Metric.define("PercentSomething");
    private static final Metric M_FAIL = Metric.define("Failure");

    // Mockito matcher that compares the pieces of a CloudWatch request we care about
    private static final class RequestMatcher implements ArgumentMatcher<PutMetricDataRequest> {

        private final String namespace;
        private final List<MetricDatum> metricData;

        public RequestMatcher(String namespace, List<MetricDatum> metricData) {
            this.namespace = namespace;
            this.metricData = metricData;
        }

        @Override
        public boolean matches(final PutMetricDataRequest request) {
            if (!request.getNamespace().equals(namespace)) {
                return false;
            }

            List<MetricDatum> data = request.getMetricData();
            if (data.size() != metricData.size()) {
                return false;
            }

            List<MetricDatum> matches = new ArrayList<>(data.size());
            for (MetricDatum actual : data) {
                for (MetricDatum expected : this.metricData) {
                    if (actual.getMetricName().equals(M_FAIL.toString())) {
                        // count metrics take the timestamp of when the metric is closed
                        // Ignore timestamps and compare the rest of the datum
                        if (actual.clone().withTimestamp(null).equals(expected.clone().withTimestamp(null))) {
                            matches.add(expected);
                        }
                    } else {
                        if (actual.equals(expected)) {
                            matches.add(expected);
                        }
                    }
                }
            }

            return (matches.size() == metricData.size());
        }
    }

    @Test
    public void shutdown_before_publish() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "spacename";

        final String id = UUID.randomUUID().toString();
        final TypedMap data = ContextData.withId(id).build();
        final double time = 123.45;

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

        final CloudWatchRecorder recorder = new CloudWatchRecorder.Builder()
                .client(client)
                .namespace(namespace)
                .publishFrequency(120)
                .maxJitter(60)
                .dimensionMapper(mapper)
                .build();

        final MetricContext context = recorder.context(data);
        Instant timestamp = Instant.now();
        context.record(M_TIME, time, Unit.MILLISECOND, timestamp);
        context.close();

        // shutdown right away, before first scheduled publish
        recorder.shutdown();

        final List<MetricDatum> expected = new ArrayList<>(1);
        expected.add(makeDatum(id, M_TIME, time, StandardUnit.Milliseconds, timestamp));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    private MetricDatum makeDatum(String id, Metric metric, double value, StandardUnit unit, Instant timestamp) {
        return new MetricDatum()
                .withMetricName(metric.toString())
                .withDimensions(new Dimension()
                                .withName(ContextData.ID.name)
                                .withValue(id)
                ).withValue(value)
                .withUnit(unit)
                .withTimestamp(Date.from(timestamp));
    }


    @Test
    public void contextClosedBeforePublish() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();
        final Integer time = Integer.valueOf(23);
        final Integer load = Integer.valueOf(87);
        final Integer load2 = Integer.valueOf(50);

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

        CloudWatchRecorder recorder = null;
        final Instant timestamp = Instant.now();
        try {
            // no jitter, publish soon
            recorder = new CloudWatchRecorder.Builder()
                    .client(client)
                    .namespace(namespace)
                    .maxJitter(0)
                    .publishFrequency(1)
                    .dimensionMapper(mapper)
                    .build();

            final TypedMap data = ContextData.withId(id).build();
            final MetricContext context = recorder.context(data);


            context.record(M_TIME, time, Unit.MILLISECOND, timestamp);
            context.count(M_FAIL, 1);
            context.count(M_FAIL, 1);
            context.record(M_PERC, load, Unit.PERCENT, timestamp);
            context.record(M_PERC, load2, Unit.PERCENT, timestamp);
            context.close();

            // allow time for one publish
            Thread.sleep(1024);
        } finally {
            recorder.shutdown();
        }

        final List<MetricDatum> expected = new ArrayList<>(4);
        expected.add(makeDatum(id, M_TIME, time, StandardUnit.Milliseconds, timestamp));
        expected.add(makeDatum(id, M_FAIL, 2, StandardUnit.Count, timestamp));
        expected.add(makeDatum(id, M_PERC, load, StandardUnit.Percent, timestamp));
        expected.add(makeDatum(id, M_PERC, load2, StandardUnit.Percent, timestamp));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    @Test
    // TODO: Currently, if a context is not closed before shutdown, no counts will be emitted.
    //  Have the recorder keep track of open contexts and close them on shutdown?
    public void contextNotClosedBeforePublish() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();
        final Integer time = Integer.valueOf(23);
        final Integer load = Integer.valueOf(87);
        final Integer load2 = Integer.valueOf(50);

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

        CloudWatchRecorder recorder = null;
        final Instant timestamp = Instant.now();
        try {
            // no jitter, publish soon
            recorder = new CloudWatchRecorder.Builder()
                    .client(client)
                    .namespace(namespace)
                    .maxJitter(0)
                    .publishFrequency(1)
                    .dimensionMapper(mapper)
                    .build();

            final TypedMap data = ContextData.withId(id).build();
            final MetricContext context = recorder.context(data);


            context.record(M_TIME, time, Unit.MILLISECOND, timestamp);
            context.count(M_FAIL, 1);
            context.count(M_FAIL, 1);
            context.record(M_PERC, load, Unit.PERCENT, timestamp);
            context.record(M_PERC, load2, Unit.PERCENT, timestamp);

            // allow time for one publish
            Thread.sleep(1024);
        } finally {
            recorder.shutdown();
        }

        final List<MetricDatum> expected = new ArrayList<>(3);
        expected.add(makeDatum(id, M_TIME, time, StandardUnit.Milliseconds, timestamp));
        expected.add(makeDatum(id, M_PERC, load, StandardUnit.Percent, timestamp));
        expected.add(makeDatum(id, M_PERC, load2, StandardUnit.Percent, timestamp));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    @Test
    public void contextClosedAfterPublish() throws Exception {
         final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();
        final Integer time = Integer.valueOf(23);
        final Integer load = Integer.valueOf(87);
        final Integer load2 = Integer.valueOf(50);

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

        CloudWatchRecorder recorder = null;
        final Instant timestamp = Instant.now();
        try {
            // no jitter, publish soon
            recorder = new CloudWatchRecorder.Builder()
                    .client(client)
                    .namespace(namespace)
                    .maxJitter(0)
                    .publishFrequency(1)
                    .dimensionMapper(mapper)
                    .build();

            final TypedMap data = ContextData.withId(id).build();
            final MetricContext context = recorder.context(data);


            context.record(M_TIME, time, Unit.MILLISECOND, timestamp);
            context.count(M_FAIL, 1);
            context.count(M_FAIL, 1);
            context.record(M_PERC, load, Unit.PERCENT, timestamp);
            context.record(M_PERC, load2, Unit.PERCENT, timestamp);

            // allow time for one publish
            Thread.sleep(1024);

            context.close();

            // allow time for the next publish
            Thread.sleep(1024);
        } finally {
            recorder.shutdown();
        }

        final List<MetricDatum> expected1 = new ArrayList<>(3);
        expected1.add(makeDatum(id, M_TIME, time, StandardUnit.Milliseconds, timestamp));
        expected1.add(makeDatum(id, M_PERC, load, StandardUnit.Percent, timestamp));
        expected1.add(makeDatum(id, M_PERC, load2, StandardUnit.Percent, timestamp));

        final List<MetricDatum> expected2 = new ArrayList<>(1);
        expected2.add(makeDatum(id, M_FAIL, 2, StandardUnit.Count, timestamp));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected1)));
        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected2)));
    }

    // Helper to create a MetricDatum instance
    private MetricDatum makeDatum(
            final String id,
            final String name,
            final double sum,
            final double min,
            final double max,
            final int count,
            final StandardUnit unit)
    {
        MetricDatum md = new MetricDatum().withMetricName(name).withUnit(unit);

        final StatisticSet statSet = new StatisticSet()
                .withSampleCount(Double.valueOf(count))
                .withSum(sum)
                .withMinimum(min)
                .withMaximum(max);
        md.setStatisticValues(statSet);

        List<Dimension> dimensions = new ArrayList<>(1);
        Dimension trace = new Dimension().withName(ContextData.ID.name).withValue(id);

        dimensions.add(trace);
        md.setDimensions(dimensions);

        return md;
    }



}
