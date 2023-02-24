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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;
import org.mockito.ArgumentMatcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 */
class CloudWatchRecorderTest {

    private static final Metric M_TIME = Metric.define("Time");
    private static final Metric M_PERC = Metric.define("PercentSomething");
    private static final Metric M_FAIL = Metric.define("Failure");

    // Mockito matcher that compares the pieces of a CloudWatch request we care about
    private static final class RequestMatcher implements ArgumentMatcher<PutMetricDataRequest> {

        private final String namespace;
        private final List<MetricDatum> metricData;

        RequestMatcher(String namespace, List<MetricDatum> metricData) {
            this.namespace = namespace;
            this.metricData = metricData;
        }

        @Override
        public boolean matches(final PutMetricDataRequest request) {
            if (!request.namespace().equals(namespace)) {
                return false;
            }

            List<MetricDatum> data = request.metricData();
            if (data.size() != metricData.size()) {
                return false;
            }

            List<MetricDatum> matches = new ArrayList<>(data.size());
            for (MetricDatum actual : data) {
                for (MetricDatum expected : this.metricData) {
                    // Ignore timestamps and compare the rest of the datum
                    if (actual.toBuilder().timestamp(null).build()
                            .equals(expected.toBuilder().timestamp(null).build())) {
                        matches.add(expected);
                    }
                }
            }

            return (matches.size() == metricData.size());
        }
    }

    @Test
    void record_and_shutdown() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "spacename";

        final String id = UUID.randomUUID().toString();
        final TypedMap data = ContextData.withId(id).build();
        final double time = 123.45;

        final CloudWatchAsyncClient client = mock(CloudWatchAsyncClient.class);

        final CloudWatchRecorder recorder = new CloudWatchRecorder.Builder()
                .client(client)
                .namespace(namespace)
                .publishFrequency(120)
                .maxJitter(60)
                .dimensionMapper(mapper)
                .build();

        final MetricContext context = recorder.context(data);
        context.record(M_TIME, time, Unit.MILLISECOND, Instant.now());
        context.close();

        // shutdown right away, before first scheduled publish
        recorder.shutdown();

        final List<MetricDatum> expected = new ArrayList<>(1);
        expected.add(makeDatum(id, M_TIME.toString(), time, time, time, 1, StandardUnit.MILLISECONDS));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }


    @Test
    void no_aggregation() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();
        final Integer time = Integer.valueOf(23);
        final Integer load = Integer.valueOf(87);

        final CloudWatchAsyncClient client = mock(CloudWatchAsyncClient.class);

        CloudWatchRecorder recorder = null;
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

            final Instant timestamp = Instant.now();

            context.record(M_TIME, time, Unit.MILLISECOND, timestamp);
            context.count(M_FAIL, 1);
            context.record(M_PERC, load, Unit.PERCENT, timestamp);
            context.close();

            // allow time for one publish
            Thread.sleep(1024);
        } finally {
            recorder.shutdown();
        }

        final List<MetricDatum> expected = new ArrayList<>(2);
        expected.add(makeDatum(id, M_TIME.toString(), time, time, time, 1, StandardUnit.MILLISECONDS));
        expected.add(makeDatum(id, M_PERC.toString(), load, load, load, 1, StandardUnit.PERCENT));
        expected.add(makeDatum(id, M_FAIL.toString(), 1, 1, 1, 1, StandardUnit.COUNT));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    @Test
    void aggregation() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();

        final double[] timeVals = {867, 5309};
        final double[] percVals = {0.01, 97.3, 3.1415};
        final int[] failCnts = {1, 3, 0, 42};

        final List<MetricDatum> expected = new ArrayList<>(3);
        expected.add(makeDatum(id,
                               M_TIME.toString(),
                               Arrays.stream(timeVals).sum(),
                               Arrays.stream(timeVals).min().getAsDouble(),
                               Arrays.stream(timeVals).max().getAsDouble(),
                               timeVals.length,
                               StandardUnit.MILLISECONDS));
        expected.add(makeDatum(id,
                               M_PERC.toString(),
                               Arrays.stream(percVals).sum(),
                               Arrays.stream(percVals).min().getAsDouble(),
                               Arrays.stream(percVals).max().getAsDouble(),
                               percVals.length,
                               StandardUnit.PERCENT));
        expected.add(makeDatum(id,
                               M_FAIL.toString(),
                               Arrays.stream(failCnts).sum(),
                               Arrays.stream(failCnts).min().getAsInt(),
                               Arrays.stream(failCnts).max().getAsInt(),
                               failCnts.length,
                               StandardUnit.COUNT));


        final CloudWatchAsyncClient client = mock(CloudWatchAsyncClient.class);

        CloudWatchRecorder recorder = null;
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

            context.count(M_FAIL, failCnts[0]);
            context.record(M_PERC, percVals[0], Unit.PERCENT, Instant.now());
            context.record(M_TIME, timeVals[0], Unit.MILLISECOND, Instant.now());
            context.count(M_FAIL, failCnts[1]);
            context.record(M_PERC, percVals[1], Unit.PERCENT, Instant.now());
            context.record(M_TIME, timeVals[1], Unit.MILLISECOND, Instant.now());
            context.count(M_FAIL, failCnts[2]);
            context.record(M_PERC, percVals[2], Unit.PERCENT, Instant.now());
            context.count(M_FAIL, failCnts[3]);
            context.close();

            // allow time for one publish
            Thread.sleep(1024);
        } finally {
            recorder.shutdown();
        }

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
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
        StatisticSet statSet = StatisticSet.builder()
                .sampleCount(Double.valueOf(count))
                .sum(sum)
                .minimum(min)
                .maximum(max)
                .build();
        Dimension trace = Dimension.builder().name(ContextData.ID.name).value(id).build();
        return MetricDatum.builder()
                .metricName(name)
                .unit(unit)
                .statisticValues(statSet)
                .dimensions(trace)
                .build();
    }



}
