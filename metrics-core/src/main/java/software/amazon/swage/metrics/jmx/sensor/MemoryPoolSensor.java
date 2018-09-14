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
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * Records information about memory pool usage, as reported by
 * the MemoryPoolMXBeans.
 */
public class MemoryPoolSensor implements Sensor {

    private static final long M = 1024 * 1024;

    @Override
    public void sense(final MetricContext metricContext)
    {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean mxBean : pools) {
            reportUsage(mxBean, metricContext);
        }

    }

    private void reportUsage(MemoryPoolMXBean mxBean, MetricContext metricContext)
    {
        // Metric names cannot contain spaces. Pool names are *all* of the form 'Alpha Beta', e.g.
        // 'Code Cache'.
        String name = mxBean.getName().replace(" ", "");
        Metric usedMetric = Metric.define("MemoryPoolUsed_" + name);
        Metric maxMetric = Metric.define("MemoryPoolMax_" + name);
        Metric percMetric = Metric.define("MemoryPoolUsage_" + name);

        MemoryUsage usage = mxBean.getUsage();
        long used = usage.getUsed();
        long max = usage.getMax();

        metricContext.record(usedMetric, used / M, Unit.MEGABYTE);

        // max can be undefined (-1) https://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryUsage.html
        if (max >= 0) {
            metricContext.record(maxMetric, max / M, Unit.MEGABYTE);

            double used_percent = 100.0 * ((double)used/(double)max);
            metricContext.record(percMetric, used_percent, Unit.PERCENT);
        }

    }

}
