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
package software.amazon.swage.metrics.jmx.sensor;

import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;


/**
 * Parses memory information out of the system MemoryMXBean, reporting heap and
 * non-heap memory usage along with max values.
 */
public class MemorySensor implements Sensor {

    private static final long M = 1024*1024;

    public static final Metric HEAP = Metric.define("HeapMemory");
    public static final Metric NON_HEAP = Metric.define("NonHeapMemory");
    public static final Metric HEAP_MAX = Metric.define("HeapMemoryMax");
    public static final Metric NON_HEAP_MAX = Metric.define("NonHeapMemoryMax");
    public static final Metric HEAP_USED = Metric.define("HeapMemoryUse");
    public static final Metric NON_HEAP_USED = Metric.define("NonHeapMemoryUse");

    @Override
    public void sense(final MetricContext metricContext)
    {
        MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();

        reportHeapUsage(mxBean, metricContext);
        reportNonHeapUsage(mxBean, metricContext);
    }

    private void reportHeapUsage(MemoryMXBean memoryMxBean, MetricContext metricContext) {
        MemoryUsage usage = memoryMxBean.getHeapMemoryUsage();

        long used = usage.getUsed();
        metricContext.record(HEAP, used / M, Unit.MEGABYTE);

        long max = usage.getMax();
        if (max >= 0) {
            metricContext.record(HEAP_MAX, max / M, Unit.MEGABYTE);

            double used_percent = 100.0 * ((double)used/(double)max);
            metricContext.record(HEAP_USED, used_percent, Unit.PERCENT);
        }
    }

    private void reportNonHeapUsage(MemoryMXBean memoryMxBean, MetricContext metricContext) {
        MemoryUsage usage = memoryMxBean.getNonHeapMemoryUsage();

        long used = usage.getUsed();
        metricContext.record(NON_HEAP, used / M, Unit.MEGABYTE);

        long max = usage.getMax();
        if (max >= 0) {
            metricContext.record(NON_HEAP_MAX, max / M, Unit.MEGABYTE);

            double used_percent = 100.0 * ((double)used/(double)max);
            metricContext.record(NON_HEAP_USED, used_percent, Unit.PERCENT);
        }
    }

}
