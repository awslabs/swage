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
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.record.MetricRecorder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MetricDataAggregatorTest {

    private static final Metric A = Metric.define("A");
    private static final Metric B = Metric.define("B");
    private static final Metric C = Metric.define("C");
    private static final Metric D = Metric.define("D");

    @Test
    public void single() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric name = Metric.define("SomeMetric");
        final double value = 3.14;
        final StandardUnit unit = StandardUnit.TERABITS;

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        aggregator.add(context, name, value, unit);

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(1, ags.size(), "One metric datum should aggregate to one entry");
        assertEquals(name.toString(), ags.get(0).metricName(), "Metric datum has wrong name");
        assertEquals(unit, ags.get(0).unit(), "Metric datum has wrong unit");

        StatisticSet stats = ags.get(0).statisticValues();
        assertEquals(Double.valueOf(value), stats.sum(), "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(value), stats.minimum(), "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(value), stats.maximum(), "Metric datum has wrong stats value");
        assertEquals(Double.valueOf(1), stats.sampleCount(), "Metric datum has wrong stats count");

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }


    @Test
    public void non_aggregated() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {A, B, C, D};
        final double[] values = {3.14, 6.28, 0, 9};
        final StandardUnit[] units = {StandardUnit.SECONDS, StandardUnit.TERABITS, StandardUnit.SECONDS, StandardUnit.MILLISECONDS};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(names.length, ags.size(), "Metric attributes hs wrong size");
        for (MetricDatum d : ags) {
            int i = 0;
            while (i < names.length && !names[i].toString().equals(d.metricName())) {
                i++;
            }
            assertTrue(i<names.length, "Aggregated metrics had unexpected datum");

            assertEquals(names[i].toString(), d.metricName(), "Metric datum has wrong name");
            assertEquals(units[i], d.unit(), "Metric datum has wrong unit");

            StatisticSet stats = d.statisticValues();
            assertEquals(Double.valueOf(values[i]), stats.sum(), "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(values[i]), stats.minimum(), "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(values[i]), stats.maximum(), "Metric datum has wrong stats value");
            assertEquals(Double.valueOf(1), stats.sampleCount(), "Metric datum has wrong stats count");
        }

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }

    @Test
    public void aggregate() {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {D, A, B, A, D, D, D};
        final double[] values = {42, 3.14, 6.28, 0, 9, 2, 4.1};
        final StandardUnit[] units = {
                StandardUnit.MILLISECONDS,
                StandardUnit.SECONDS,
                StandardUnit.TERABITS,
                StandardUnit.SECONDS,
                StandardUnit.MILLISECONDS,
                StandardUnit.PERCENT,
                StandardUnit.MILLISECONDS};

        final MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(ContextData.withId(UUID.randomUUID().toString()).build());

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals(4, ags.size(), "Metric attributes hs wrong size");
        boolean[] seen = {false, false, false, false};
        for (MetricDatum d : ags) {
            if (d.metricName().equals(A.toString()) && d.unit().equals(StandardUnit.SECONDS)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(3.14+0), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(3.14), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seen[0] = true;
            }
            else if (d.metricName().equals(B.toString()) && d.unit().equals(StandardUnit.TERABITS)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(6.28), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(6.28), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(6.28), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(1), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seen[1] = true;
            }
            else if (d.metricName().equals(D.toString()) && d.unit().equals(StandardUnit.MILLISECONDS)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(42+9+4.1), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(4.1), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(42), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(3), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seen[2] = true;
            }
            else if (d.metricName().equals(D.toString()) && d.unit().equals(StandardUnit.PERCENT)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(2), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(2), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(1), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seen[3] = true;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        for (int i=0; i<seen.length; i++) {
            assertTrue(seen[i], "Expected aggregated metric not seen");
        }

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");

        // Now add more attributes, but let's just do two this time
        for (int i=0; i<5; i++) {
            aggregator.add(context, A, i, StandardUnit.COUNT);
        }
        Metric other = Metric.define("Other");
        double osum = 0;
        for (int i=0; i<12; i++) {
            osum += 0.01*i;
            aggregator.add(context, other, 0.01*i, StandardUnit.TERABITS);
        }

        ags = aggregator.flush();

        assertEquals(2, ags.size(), "Metric attributes hs wrong size");
        boolean seenA = false;
        boolean seenO = false;
        for (MetricDatum d : ags) {
            if (d.metricName().equals(A.toString()) && d.unit().equals(StandardUnit.COUNT)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(0+1+2+3+4), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(4), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(5), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seenA = true;
            }
            else if (d.metricName().equals(other.toString()) && d.unit().equals(StandardUnit.TERABITS)) {
                StatisticSet stats = d.statisticValues();
                assertEquals(Double.valueOf(osum), stats.sum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0), stats.minimum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(0.11), stats.maximum(), "Aggregated metric has wrong stats value");
                assertEquals(Double.valueOf(12), stats.sampleCount(), "Aggregated metric has wrong stats count");
                seenO = true;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        assertTrue(seenA, "Expected aggregated metric not seen");
        assertTrue(seenO, "Expected aggregated metric not seen");

        assertTrue(aggregator.flush().isEmpty(), "Flush with no attributes was non-empty");
    }

}
