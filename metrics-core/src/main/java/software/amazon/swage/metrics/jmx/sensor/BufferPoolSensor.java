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
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Records information about buffer pool usage, as reported by
 * the BufferPoolMXBeans.
 */
public class BufferPoolSensor implements Sensor {

    private static final long M = 1024*1024;

    @Override
    public void sense(final MetricRecorder.Context metricContext)
    {
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

        for (BufferPoolMXBean mxBean : pools) {
            //TODO: something else?
            String name = mxBean.getName();
            Metric countMetric = Metric.define("BufferPoolCount_"+name);
            Metric usedMetric = Metric.define("BufferPoolUsed_"+name);
            Metric maxMetric = Metric.define("BufferPoolMax_"+name);

            metricContext.record(countMetric, mxBean.getCount(), Unit.NONE);
            metricContext.record(usedMetric, mxBean.getMemoryUsed() / M, Unit.MEGABYTE);
            metricContext.record(maxMetric,  mxBean.getTotalCapacity() / M, Unit.MEGABYTE);
        }

    }

}
