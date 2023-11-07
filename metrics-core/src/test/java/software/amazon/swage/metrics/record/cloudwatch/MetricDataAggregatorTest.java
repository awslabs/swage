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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.record.MetricRecorder;

class MetricDataAggregatorTest {

    private static final Metric A = Metric.define("A");
    private static final Metric B = Metric.define("B");
    private static final Metric C = Metric.define("C");
    private static final Metric D = Metric.define("D");

    @Test
    void single() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric name = Metric.define("SomeMetric");
        final double value = 3.14;
        final StandardUnit unit = StandardUnit.Terabits;

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        aggregator.add(context, name, value, unit);

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(1, ags.size(), "One metric datum should aggregate to one entry");
        assertEquals(name.toString(), ags.get(0).getMetricName(), "Metric datum has wrong name");
        assertEquals(unit.toString(), ags.get(0).getUnit(), "Metric datum has wrong unit");

        StatisticSet stats = ags.get(0).getStatisticValues();
        assertEquals(Double.valueOf(value), stats.getSum(), "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(value), stats.getMinimum(),
                "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(value), stats.getMaximum(),
                "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(1), stats.getSampleCount(),
                "Metric datum has wrong stats count");

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }


    @Test
    void non_aggregated() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {A, B, C, D};
        final double[] values = {3.14, 6.28, 0, 9};
        final StandardUnit[] units = {StandardUnit.Seconds, StandardUnit.Terabits,
                StandardUnit.Seconds, StandardUnit.Milliseconds};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i = 0; i < names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(names.length, ags.size(), "Metric attributes hs wrong size");
        for (MetricDatum d : ags) {
            int i = 0;
            while (i < names.length && !names[i].toString().equals(d.getMetricName())) {
                i++;
            }
            assertTrue(i < names.length, "Aggregated metrics had unexpected datum");

            assertEquals(names[i].toString(), d.getMetricName(), "Metric datum has wrong name");
            assertEquals(units[i].toString(), d.getUnit(), "Metric datum has wrong unit");

            StatisticSet stats = d.getStatisticValues();
            assertEquals(Double.valueOf(values[i]), stats.getSum(),
                    "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(values[i]), stats.getMinimum(),
                    "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(values[i]), stats.getMaximum(),
                    "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(1), stats.getSampleCount(),
                    "Metric datum has wrong stats count");
        }

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }

    @Test
    void aggregate() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {D, A, B, A, D, D, D};
        final double[] values = {42, 3.14, 6.28, 0, 9, 2, 4.1};
        final StandardUnit[] units = {
                StandardUnit.Milliseconds,
                StandardUnit.Seconds,
                StandardUnit.Terabits,
                StandardUnit.Seconds,
                StandardUnit.Milliseconds,
                StandardUnit.Percent,
                StandardUnit.Milliseconds};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i = 0; i < names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(4, ags.size(), "Metric attributes hs wrong size");
        boolean[] seen = {false, false, false, false};
        for (MetricDatum d : ags) {
            if (d.getMetricName().equals(A.toString()) && d.getUnit()
                    .equals(StandardUnit.Seconds.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(3.14 + 0), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(3.14), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.getSampleCount(),
                        "Aggregated metric has wrong stats value");
                seen[0] = true;
            } else if (d.getMetricName().equals(B.toString()) && d.getUnit()
                    .equals(StandardUnit.Terabits.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(6.28), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(6.28), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(6.28), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(1), stats.getSampleCount(),
                        "Aggregated metric has wrong stats count");
                seen[1] = true;
            } else if (d.getMetricName().equals(D.toString()) && d.getUnit()
                    .equals(StandardUnit.Milliseconds.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(42 + 9 + 4.1), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(4.1), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(42), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(3), stats.getSampleCount(),
                        "Aggregated metric has wrong stats count");
                seen[2] = true;
            } else if (d.getMetricName().equals(D.toString()) && d.getUnit()
                    .equals(StandardUnit.Percent.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(2), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(1), stats.getSampleCount(),
                        "Aggregated metric has wrong stats count");
                seen[3] = true;
            } else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        for (int i = 0; i < seen.length; i++) {
            assertTrue(seen[i], "Expected aggregated metric not seen");
        }

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");

        // Now add more attributes, but let's just do two this time
        for (int i = 0; i < 5; i++) {
            aggregator.add(context, A, i, StandardUnit.Count);
        }
        Metric other = Metric.define("Other");
        double osum = 0;
        for (int i = 0; i < 12; i++) {
            osum += 0.01 * i;
            aggregator.add(context, other, 0.01 * i, StandardUnit.Terabits);
        }

        ags = aggregator.flush();

        assertEquals(2, ags.size(), "Metric attributes hs wrong size");
        boolean seenA = false;
        boolean seenO = false;
        for (MetricDatum d : ags) {
            if (d.getMetricName().equals(A.toString()) && d.getUnit()
                    .equals(StandardUnit.Count.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(0 + 1 + 2 + 3 + 4), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(4), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(5), stats.getSampleCount(),
                        "Aggregated metric has wrong stats count");
                seenA = true;
            } else if (d.getMetricName().equals(other.toString()) && d.getUnit()
                    .equals(StandardUnit.Terabits.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals(Double.valueOf(osum), stats.getSum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.getMinimum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0.11), stats.getMaximum(),
                        "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(12), stats.getSampleCount(),
                        "Aggregated metric has wrong stats count");
                seenO = true;
            } else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        assertTrue(seenA, "Expected aggregated metric not seen");
        assertTrue(seenO, "Expected aggregated metric not seen");

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }

}
