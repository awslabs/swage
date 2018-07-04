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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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

            // Check that multiset(metricData) == multiset(request.metricData).

            final Map<MetricDatum, Long> expected = multiset(metricData);
            final Map<MetricDatum, Long> actual = multiset(request.getMetricData());

            return expected.equals(actual);
        }

        private static Map<MetricDatum, Long> multiset(final List<MetricDatum> data) {
            return data.stream()
                    .map(datum -> datum.clone().withTimestamp(null))
                    .collect(Collectors.groupingBy(datum -> datum, Collectors.counting()));
        }
    }

    @Test
    public void record_and_shutdown() throws Exception {
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
        context.record(M_TIME, time, Unit.MILLISECOND, Instant.now());
        context.close();

        // shutdown right away, before first scheduled publish
        recorder.shutdown();

        final List<MetricDatum> expected = new ArrayList<>();
        expected.add(makeRecordDatum(id, M_TIME.toString(), time, StandardUnit.Milliseconds));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }


    @Test
    public void no_aggregation() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();
        final Integer time = Integer.valueOf(23);
        final Integer load = Integer.valueOf(87);

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

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

        final List<MetricDatum> expected = new ArrayList<>();
        expected.add(makeRecordDatum(id, M_TIME.toString(), time, StandardUnit.Milliseconds));
        expected.add(makeRecordDatum(id, M_PERC.toString(), load, StandardUnit.Percent));
        expected.add(makeAggregateDatum(id, M_FAIL.toString(), 1.0));

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    @Test
    public void aggregation() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final String namespace = "testytesttest";
        final String id = UUID.randomUUID().toString();

        final double[] timeVals = { 867.0, 5309.0 };
        final double[] percVals = { 0.01, 97.3, 3.1415 };
        final int[] failCnts = { 1, 3, 0, 42, 3, 0, 0 };

        final List<MetricDatum> expected = new ArrayList<>();
        for (final double val : timeVals) {
            expected.add(makeRecordDatum(
                    id,
                    M_TIME.toString(),
                    val,
                    StandardUnit.Milliseconds));
        }
        for (final double val : percVals) {
            expected.add(makeRecordDatum(
                    id,
                    M_PERC.toString(),
                    val,
                    StandardUnit.Percent));
        }
        expected.add(makeAggregateDatum(id,
                M_FAIL.toString(),
                (double) Arrays.stream(failCnts).min().getAsInt(),
                (double) Arrays.stream(failCnts).max().getAsInt(),
                (double) Arrays.stream(failCnts).sum(),
                failCnts.length));

        final AmazonCloudWatch client = mock(AmazonCloudWatch.class);

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
            context.count(M_FAIL, failCnts[4]);
            context.count(M_FAIL, failCnts[5]);
            context.count(M_FAIL, failCnts[6]);
            context.close();

            // allow time for one publish
            Thread.sleep(1024L);
        } finally {
            recorder.shutdown();
        }

        verify(client).putMetricData(argThat(new RequestMatcher(namespace, expected)));
    }

    // Helper to create a MetricDatum instance for count().
    private MetricDatum makeAggregateDatum(
            final String id,
            final String name,
            final double singleValue) {
        return makeAggregateDatum(id, name, singleValue, singleValue, singleValue, 1);
    }

    private MetricDatum makeAggregateDatum(
            final String id,
            final String name,
            final double min,
            final double max,
            final double sum,
            final int count) {
        final Dimension trace = new Dimension().withName(ContextData.ID.name).withValue(id);

        final MetricDatum datum = new MetricDatum()
                .withDimensions(Collections.singletonList(trace))
                .withMetricName(name)
                .withUnit(StandardUnit.Count);
        if (count == 1) {
            return datum.withValue((double) sum);
        } else {
            return datum.withStatisticValues(new StatisticSet()
                    .withMinimum(min)
                    .withMaximum(max)
                    .withSum(sum)
                    .withSampleCount((double) count));
        }
    }

    // Helper to create a MetricDatum instance for record().
    private MetricDatum makeRecordDatum(
            final String id,
            final String name,
            final double value,
            final StandardUnit unit) {
        final Dimension trace = new Dimension().withName(ContextData.ID.name).withValue(id);

        return new MetricDatum()
                .withDimensions(Collections.singletonList(trace))
                .withMetricName(name)
                .withUnit(unit)
                .withValue(value);
    }
}
