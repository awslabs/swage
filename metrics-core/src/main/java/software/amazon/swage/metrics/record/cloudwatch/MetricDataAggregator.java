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
import software.amazon.swage.metrics.Metric;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates metric values into {@link StatisticSet}s, constructing a list of
 * {@link MetricDatum} appropriate to send to CloudWatch.
 *
 * Allows adding metrics for a period of time, then flushing all the aggregated
 * metadata, then accumulating more.
 * <p>
 * Package-private as this is intended purely as an implementation detail of
 * the CloudWatchRecorder. The implementation is specific to how it is used
 * by the recorder, and does not currently support "general purpose"
 * aggregation of metric metadata/MetricDatums.
 * <p>
 * Metrics are aggregated by namespace, metricName, dimensions, and unit.
 * Matching metrics events will be added together to form a single
 * {@link StatisticSet}.
 * No aggregation across disparate dimensions is supported.
 * <p>
 * Dimensions to use for a metric are determined by a {@link DimensionMapper},
 * with values pulled from the current metric context.
 * </p>
 * <p>
 * This class is thread-safe.
 */
class MetricDataAggregator {

    private final ConcurrentHashMap<DatumKey, StatisticSet> statisticsMap;
    private final DimensionMapper dimensionMapper;

    public MetricDataAggregator(final DimensionMapper dimensionMapper)
    {
        this.statisticsMap = new ConcurrentHashMap<>();
        this.dimensionMapper = dimensionMapper;
    }

    /**
     * Add a metric event to be aggregated.
     * Events with the same name, unit, and dimensions will have their values
     * aggregated into {@link StatisticSet}s, with the aggregated metadata
     * available via {@link #flush}.
     *
     * @param context Metric context to use for dimension information
     * @param name Metric name
     * @param value Recorded value for the metric event
     * @param unit Unit for interpreting the value
     */
    public void add(
            final TypedMap context,
            final Metric name,
            final double value,
            final StandardUnit unit)
    {
        //TODO: avoid doing this every time for a context - caching, or?
        List<Dimension> dimensions = dimensionMapper.getDimensions(name, context);

        DatumKey key = new DatumKey(name.toString(), unit, dimensions);
        statisticsMap.merge(
                key,
                new StatisticSet()
                        .withMaximum(value)
                        .withMinimum(value)
                        .withSampleCount(1D)
                        .withSum(value),
                MetricDataAggregator::sum);
    }

    /**
     * Flush all the current aggregated MetricDatum and return as a list.
     * This is safe to call concurrently with {@link #add}.
     * All metadata added prior to a flush call will be included in the returned aggregate.
     * Any metadata added after the flush call returns will be included in a subsequent flush.
     * Data added while a flush call is processing may be included in the current flush
     * or a subsequent flush, but will not be included twice.
     *
     * The timestamp on the aggregated metadata will be the time it was flushed,
     * not the time of any of the original metric events.
     *
     * @return list of all metadata aggregated since the last flush
     */
    public List<MetricDatum> flush() {
        if (statisticsMap.size() == 0) {
            return Collections.emptyList();
        }

        // Capture all the current metrics, as represented by the set of keys
        // at this time in the statisticsMap.
        // Note that this iterates over the key set of the underlying map, and
        // removes keys from the map at the same time. It is possible keys may
        // be added during this iteration, or metadata for keys modified between
        // a key being chosen for iteration and being removed from the map.
        // This is ok.  Any new keys will be picked up on subsequent flushes.
        //TODO: use two maps and swap between, to ensure 'perfect' segmentation?
        List<MetricDatum> metricData = new ArrayList<>();
        for (DatumKey key : statisticsMap.keySet()) {
            StatisticSet value = statisticsMap.remove(key);
            //TODO: better to have no timestamp at all?
            MetricDatum metricDatum = key.getDatum().withTimestamp(Date.from(Instant.now()))
                                                    .withStatisticValues(value);
            metricData.add(metricDatum);
        }

        return metricData;
    }


    private static StatisticSet sum( StatisticSet v1, StatisticSet v2 ) {
        //TODO: reuse one of the passed sets, and pollute a MetricDatum?
        StatisticSet stats = new StatisticSet();

        stats.setMaximum(Math.max(v1.getMaximum(), v2.getMaximum()));
        stats.setMinimum(Math.min(v1.getMinimum(), v2.getMinimum()));
        stats.setSampleCount(v1.getSampleCount() + v2.getSampleCount());
        stats.setSum(v1.getSum() + v2.getSum());

        return stats;
    }

    /**
     * A wrapper for MetricDatum instances to use as a key in our aggregating map.
     * MetricDatum objects are treated the same, and aggregated together, if they share
     * the same:
     *  namespace
     *  metric name
     *  dimensions
     *  unit
     */
    private static final class DatumKey {
        private final String name;
        private final StandardUnit unit;
        private final List<Dimension> dimensions;

        public DatumKey(
                final String name,
                final StandardUnit unit,
                final List<Dimension> dimensions)
        {
            this.name = name;
            this.unit = unit;
            this.dimensions = dimensions;
        }

        public MetricDatum getDatum() {
            MetricDatum md = new MetricDatum();
            md.setMetricName(name);
            md.setUnit(unit);
            md.setDimensions(dimensions);
            return md;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final DatumKey datumKey = (DatumKey) o;
            return Objects.equals(name, datumKey.name) &&
                   unit == datumKey.unit &&
                   Objects.equals(dimensions, datumKey.dimensions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, unit, dimensions);
        }
    }

}
