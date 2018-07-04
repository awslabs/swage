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

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import org.junit.Test;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.record.MetricRecorder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetricDataAggregatorTest {

    private static final Metric A = Metric.define("A");
    private static final Metric B = Metric.define("B");
    private static final Metric C = Metric.define("C");
    private static final Metric D = Metric.define("D");

    @Test
    public void single() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric name = Metric.define("SomeMetric");
        final double value = 3.14;
        final StandardUnit unit = StandardUnit.Terabits;

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        aggregator.addAggregated(context, name, value, unit);

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("One metric datum should aggregate to one entry", 1, ags.size());
        assertEquals("Metric datum has wrong name", name.toString(), ags.get(0).getMetricName());
        assertEquals("Metric datum has wrong unit", unit, getUnit(ags.get(0)));

        assertEquals("Metric datum has wrong stats", makeStatistic(value),
                getStatisticSet(ags.get(0)));

        assertTrue("Flush with no attributes was non-empty", aggregator.flush().isEmpty());
    }

    @Test
    public void non_aggregated() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {A, B, C, D};
        final double[] values = {3.14, 6.28, 0, 9};
        final StandardUnit[] units = {StandardUnit.Seconds, StandardUnit.Terabits, StandardUnit.Seconds, StandardUnit.Milliseconds};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.addAggregated(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("Metric attributes has wrong size", names.length, ags.size());
        for (MetricDatum d : ags) {
            // Find the index within [names] for this datum's metric.
            int i = 0;
            while (i < names.length && !names[i].toString().equals(d.getMetricName())) {
                i++;
            }
            assertTrue("Aggregated metrics had unexpected datum", i<names.length);

            assertEquals("Metric datum has wrong name", names[i].toString(), d.getMetricName());
            assertEquals("Metric datum has wrong unit", units[i], getUnit(d));

            assertEquals("Metric datum has wrong stats", makeStatistic(values[i]),
                    getStatisticSet(d));
        }

        assertTrue("Flush with no attributes was non-empty", aggregator.flush().isEmpty());
    }

    @Test
    public void aggregate() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names  = { D,    A,    B, A, D, D, D, A};
        final double[] values = {42, 3.14, 6.28, 0, 9, 2, 9, 0};
        final StandardUnit[] units = {
                StandardUnit.Milliseconds,
                StandardUnit.Seconds,
                StandardUnit.Terabits,
                StandardUnit.Seconds,
                StandardUnit.Milliseconds,
                StandardUnit.Percent,
                StandardUnit.Milliseconds,
                StandardUnit.Seconds};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.addAggregated(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("Metric attributes has wrong size", 4, ags.size());
        int[] seen = {0, 0, 0, 0};
        int[] expectedSeen = {1, 1, 1, 1};
        for (MetricDatum d : ags) {
            final StatisticSet stats = getStatisticSet(d);

            if (d.getMetricName().equals(A.toString()) && getUnit(d) == StandardUnit.Seconds) {
                final StatisticSet exp = makeStatistic(0, 3.14, 3.14, 3);

                assertEquals(exp, stats);
                seen[0]++;
            }
            else if (d.getMetricName().equals(B.toString()) && getUnit(d) == StandardUnit.Terabits) {
                final StatisticSet exp = makeStatistic(6.28);
                assertEquals(exp, stats);
                seen[1]++;
            }
            else if (d.getMetricName().equals(D.toString()) && getUnit(d) == StandardUnit.Milliseconds) {
                final StatisticSet exp = makeStatistic(9.0, 42.0, 60.0, 3);

                assertEquals(exp, stats);
                seen[2]++;
            }
            else if (d.getMetricName().equals(D.toString()) && getUnit(d) == StandardUnit.Percent) {
                final StatisticSet exp = makeStatistic(2.0);
                assertEquals(exp, stats);
                seen[3]++;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        assertArrayEquals("Metric-seen counts don't match", expectedSeen, seen);

        assertTrue("Flush with no attributes was non-empty", aggregator.flush().isEmpty());

        // Now add more attributes, but let's just do two this time.
        // These will aggregate:
        for (int i=0; i<5; i++) {
            aggregator.addAggregated(context, A, 1, StandardUnit.Count);
        }
        Metric other = Metric.define("Other");
        // These will not:
        for (int i=0; i<12; i++) {
            // Uses a value that is representable in base 2, so multiplication isn't lossy:
            aggregator.addRecording(context, other, 3.0 / 16.0, StandardUnit.Terabits,
                    Instant.now());
        }

        ags = aggregator.flush();

        assertEquals("Metric attributes has wrong size", 1 + 12, ags.size());
        boolean seenA = false;
        int seenO = 0;
        for (MetricDatum d : ags) {
            if (d.getMetricName().equals(A.toString()) && getUnit(d) == StandardUnit.Count) {
                final StatisticSet exp = makeStatistic(1.0, 1.0, 5.0, 5);
                assertEquals(exp, getStatisticSet(d));
                seenA = true;
            }
            else if (d.getMetricName().equals(other.toString()) && getUnit(d) == StandardUnit.Terabits) {
                final StatisticSet exp = makeStatistic(3.0 / 16.0);
                assertEquals(exp, getStatisticSet(d));
                seenO++;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        assertTrue("Metric-seen count doesn't match", seenA);
        assertEquals("Metric-seen count doesn't match", 12, seenO);

        assertTrue("Flush with no attributes was non-empty", aggregator.flush().isEmpty());
    }

    private static StatisticSet getStatisticSet(final MetricDatum datum) {
        final StatisticSet stats = datum.getStatisticValues();
        return stats == null ? makeStatistic(datum.getValue()) : stats;
    }

    private static StandardUnit getUnit(final MetricDatum datum) {
        return StandardUnit.fromValue(datum.getUnit());
    }

    private static StatisticSet makeStatistic(final double singleValue) {
        return makeStatistic(singleValue, singleValue, singleValue, 1);
    }

    private static StatisticSet makeStatistic(final double min, final double max, final double sum,
            final int count) {
        return new StatisticSet()
                .withMinimum(min)
                .withMaximum(max)
                .withSum(sum)
                .withSampleCount((double) count);
    }
}
