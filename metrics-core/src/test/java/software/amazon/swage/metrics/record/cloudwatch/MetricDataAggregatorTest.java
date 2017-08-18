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
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
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

        final TypedMap context = ContextData.withId(UUID.randomUUID().toString()).build();

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        aggregator.add(context, name, value, unit);

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("One metric datum should aggregate to one entry", 1, ags.size());
        assertEquals("Metric datum has wrong name", name.toString(), ags.get(0).getMetricName());
        assertEquals("Metric datum has wrong unit", unit.toString(), ags.get(0).getUnit());

        StatisticSet stats = ags.get(0).getStatisticValues();
        assertEquals("Metric datum has wrong stats value", Double.valueOf(value), stats.getSum());
        assertEquals("Metric datum has wrong stats value", Double.valueOf(value), stats.getMinimum());
        assertEquals("Metric datum has wrong stats value", Double.valueOf(value), stats.getMaximum());
        assertEquals("Metric datum has wrong stats count", Double.valueOf(1), stats.getSampleCount());

        assertTrue("Flush with no data was non-empty", aggregator.flush().isEmpty());
    }


    @Test
    public void non_aggregated() throws Exception {
        final DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        final Metric[] names = {A, B, C, D};
        final double[] values = {3.14, 6.28, 0, 9};
        final StandardUnit[] units = {StandardUnit.Seconds, StandardUnit.Terabits, StandardUnit.Seconds, StandardUnit.Milliseconds};

        final TypedMap context = ContextData.withId(UUID.randomUUID().toString()).build();

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("Metric data hs wrong size", names.length, ags.size());
        for (MetricDatum d : ags) {
            int i = 0;
            while (i < names.length && !names[i].toString().equals(d.getMetricName())) {
                i++;
            }
            assertTrue("Aggregated metrics had unexpected datum", i<names.length);

            assertEquals("Metric datum has wrong name", names[i].toString(), d.getMetricName());
            assertEquals("Metric datum has wrong unit", units[i].toString(), d.getUnit());

            StatisticSet stats = d.getStatisticValues();
            assertEquals("Metric datum has wrong stats value", Double.valueOf(values[i]), stats.getSum());
            assertEquals("Metric datum has wrong stats value", Double.valueOf(values[i]), stats.getMinimum());
            assertEquals("Metric datum has wrong stats value", Double.valueOf(values[i]), stats.getMaximum());
            assertEquals("Metric datum has wrong stats count", Double.valueOf(1), stats.getSampleCount());
        }

        assertTrue("Flush with no data was non-empty", aggregator.flush().isEmpty());
    }

    @Test
    public void aggregate() throws Exception {
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

        final TypedMap context = ContextData.withId(UUID.randomUUID().toString()).build();

        MetricDataAggregator aggregator = new MetricDataAggregator(mapper);
        for (int i=0; i<names.length; i++) {
            aggregator.add(context, names[i], values[i], units[i]);
        }

        List<MetricDatum> ags = aggregator.flush();

        assertEquals("Metric data hs wrong size", 4, ags.size());
        boolean[] seen = {false, false, false, false};
        for (MetricDatum d : ags) {
            if (d.getMetricName().equals(A.toString()) && d.getUnit().equals(StandardUnit.Seconds.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(3.14+0), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(0), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(3.14), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(2), stats.getSampleCount());
                seen[0] = true;
            }
            else if (d.getMetricName().equals(B.toString()) && d.getUnit().equals(StandardUnit.Terabits.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(6.28), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(6.28), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(6.28), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(1), stats.getSampleCount());
                seen[1] = true;
            }
            else if (d.getMetricName().equals(D.toString()) && d.getUnit().equals(StandardUnit.Milliseconds.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(42+9+4.1), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(4.1), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(42), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(3), stats.getSampleCount());
                seen[2] = true;
            }
            else if (d.getMetricName().equals(D.toString()) && d.getUnit().equals(StandardUnit.Percent.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(2), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(2), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(2), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(1), stats.getSampleCount());
                seen[3] = true;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        for (int i=0; i<seen.length; i++) {
            assertTrue("Expected aggregated metric not seen", seen[i]);
        }

        assertTrue("Flush with no data was non-empty", aggregator.flush().isEmpty());

        // Now add more data, but let's just do two this time
        for (int i=0; i<5; i++) {
            aggregator.add(context, A, i, StandardUnit.Count);
        }
        Metric other = Metric.define("Other");
        double osum = 0;
        for (int i=0; i<12; i++) {
            osum += 0.01*i;
            aggregator.add(context, other, 0.01*i, StandardUnit.Terabits);
        }

        ags = aggregator.flush();

        assertEquals("Metric data hs wrong size", 2, ags.size());
        boolean seenA = false;
        boolean seenO = false;
        for (MetricDatum d : ags) {
            if (d.getMetricName().equals(A.toString()) && d.getUnit().equals(StandardUnit.Count.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(0+1+2+3+4), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(0), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(4), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(5), stats.getSampleCount());
                seenA = true;
            }
            else if (d.getMetricName().equals(other.toString()) && d.getUnit().equals(StandardUnit.Terabits.toString())) {
                StatisticSet stats = d.getStatisticValues();
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(osum), stats.getSum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(0), stats.getMinimum());
                assertEquals("Aggregated metric has wrong stats value", Double.valueOf(0.11), stats.getMaximum());
                assertEquals("Aggregated metric has wrong stats count", Double.valueOf(12), stats.getSampleCount());
                seenO = true;
            }
            else {
                fail("Unexpected metric returned in aggregated set");
            }
        }
        assertTrue("Expected aggregated metric not seen", seenA);
        assertTrue("Expected aggregated metric not seen", seenO);

        assertTrue("Flush with no data was non-empty", aggregator.flush().isEmpty());
    }

}
