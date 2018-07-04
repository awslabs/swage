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

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.record.MetricRecorder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Aggregates metric values into {@link StatisticSet}s, constructing a list of
 * {@link MetricDatum} appropriate to send to CloudWatch.
 *
 * Allows adding metrics for a period of time, then flushing all the aggregated
 * data, then accumulating more.
 * <p>
 * Package-private as this is intended purely as an implementation detail of
 * the CloudWatchRecorder. The implementation is specific to how it is used
 * by the recorder, and does not currently support "general purpose"
 * aggregation of metric data/MetricDatums.
 * <p>
 * Metrics are aggregated by namespace, metricName, attributes, and unit.
 * Matching metrics events will be added together to form a single value. No
 * aggregation across disparate dimensions is supported. Performance metrics
 * (i.e. @link{MetricRecorder#record} was used) are not aggregated, preserving
 * precise timestamps and allowing for percentile graphing/alarming.
 * </p>
 * <p>
 * Dimensions to use for a metric are determined by a {@link DimensionMapper},
 * with values pulled from the current metric context.
 * </p>
 * <p>
 * This class is thread-safe.
 */
class MetricDataAggregator {

    private final DimensionMapper dimensionMapper;
    // (dimensions, metric, unit) -> [datum, ...]
    // This queue is actually unbounded, so it never blocks on insert.
    private final BlockingQueue<MetricDatum> records;
    // (dimensions, metric, unit) -> (min, max, sum, count)
    private final ConcurrentMap<DatumKey, StatisticSet> statisticsMap;

    public MetricDataAggregator(final DimensionMapper dimensionMapper)
    {
        this.dimensionMapper = dimensionMapper;
        this.records = new LinkedBlockingQueue<>();
        this.statisticsMap = new ConcurrentHashMap<>();
    }

    /**
     * Add a metric event to be aggregated.
     * Events with the same name, unit, and attributes will have their values
     * aggregated into {@link StatisticSet}s, with the aggregated data
     * available via {@link #flush}.
     *
     * @param context Metric context to use for dimension information
     * @param name Metric name
     * @param value Recorded value for the metric event
     * @param unit Unit for interpreting the value
     */
    public void addAggregated(
            final MetricRecorder.RecorderContext context,
            final Metric name,
            final double value,
            final StandardUnit unit)
    {
        //TODO: avoid doing this every time for a context - caching, or?
        final List<Dimension> dimensions = dimensionMapper.getDimensions(name, context);
        final DatumKey key = new DatumKey(dimensions, name.toString(), unit);

        statisticsMap.compute(key, (entryKey, entryValue) -> {
            if (entryValue == null) {
                return new StatisticSet()
                        .withMaximum(value)
                        .withMinimum(value)
                        .withSampleCount(1.0)
                        .withSum(value);
            }
            // Otherwise update the old entry value.
            if (entryValue.getMaximum() < value) {
                entryValue.setMaximum(value);
            } else {
                entryValue.setMinimum(value);
            }
            return entryValue
                    .withSampleCount(entryValue.getSampleCount() + 1.0)
                    .withSum(entryValue.getSum() + value);
        });
    }

    /**
     * Add a metric event without aggregation.
     * They are available via {@link #flush}.
     *
     * @param context Metric context to use for dimension information
     * @param name Metric name
     * @param value Recorded value for the metric event
     * @param unit Unit for interpreting the value
     * @param time Timestamp for the event
     */
    public void addRecording(
            final TypedMap context,
            final Metric name,
            final double value,
            final StandardUnit unit,
            final Instant time)
    {
        //TODO: avoid doing this every time for a context - caching, or?
        final List<Dimension> dimensions = dimensionMapper.getDimensions(name, context);
        final DatumKey key = new DatumKey(dimensions, name.toString(), unit);

        // Will always insert and return immediately since the queue is unbounded.
        records.offer(key.toDatum().withTimestamp(Date.from(time)).withValue(value));
    }

    /**
     * Flush all the current aggregated MetricDatum and return as a list.
     * This is safe to call concurrently with {@link #add}.
     * All data added prior to a flush call will be included in the returned aggregate.
     * Any data added after the flush call returns will be included in a subsequent flush.
     * Data added while a flush call is processing may be included in the current flush
     * or a subsequent flush, but will not be included twice.
     *
     * The timestamp on the aggregated data will be the time it was flushed,
     * not the time of any of the original metric events.
     *
     * @return list of all data aggregated since the last flush
     */
    public List<MetricDatum> flush() {
        if (statisticsMap.isEmpty() && records.isEmpty()) {
            return Collections.emptyList();
        }

        // Capture all the current metrics, as represented by the set of keys
        // at this time in the statisticsMap.
        // Note that this iterates over the key set of the underlying map, and
        // removes keys from the map at the same time. It is possible keys may
        // be added during this iteration, or data for keys modified between
        // a key being chosen for iteration and being removed from the map.
        // This is ok.  Any new keys will be picked up on subsequent flushes.
        //TODO: use two maps and swap between, to ensure 'perfect' segmentation?
        List<MetricDatum> metricData = new ArrayList<>();
        final Date now = new Date();
        for (DatumKey key : statisticsMap.keySet()) {
            StatisticSet value = statisticsMap.remove(key);
            if (value == null) {
                // Somehow no longer in the map.
                continue;
            }

            //TODO: better to have no timestamp at all?
            final MetricDatum metricDatum = key.toDatum().withTimestamp(now);
            if (value.getSampleCount() == 1.0) {
                // set the value field for a smaller data structure
                metricDatum.setValue(value.getSum());
            } else {
                // set the statisticValues field instead
                metricDatum.setStatisticValues(value);
            }

            metricData.add(metricDatum);
        }

        // This never blocks (esp. if the queue is empty).
        records.drainTo(metricData);

        return metricData;
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
                final List<Dimension> dimensions,
                final String name,
                final StandardUnit unit) {
            this.name = name;
            this.unit = unit;
            this.dimensions = dimensions;
        }

        public MetricDatum toDatum() {
            return new MetricDatum()
                    .withDimensions(dimensions)
                    .withMetricName(name)
                    .withUnit(unit);
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
