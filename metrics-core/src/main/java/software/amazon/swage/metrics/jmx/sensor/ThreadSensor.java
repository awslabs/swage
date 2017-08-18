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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Reports metrics about threads currently running within the JVM, as reported
 * by the ThreadMXBean.
 */
public class ThreadSensor implements Sensor {

    /**
     * Number of threads currently running
     */
    public static final Metric THREADS = Metric.define("Threads");

    /**
     * Peak number of threads running, across lifetime of the JVM
     */
    public static final Metric PEAK_THREADS = Metric.define("PeakThreads");

    /**
     * Peak number of threads that were running since the last time this sensor was invoked
     */
    public static final Metric PERIODIC_PEAK_THREADS = Metric.define("PeriodicPeakThreads");

    private int historicalPeak = 0;

    @Override
    public void sense(final MetricRecorder.Context metricContext)
    {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        int current = mxBean.getThreadCount();
        int peak = mxBean.getPeakThreadCount();

        if (peak > historicalPeak) {
            historicalPeak = peak;
        }

        metricContext.record(THREADS, current, Unit.NONE);
        metricContext.record(PERIODIC_PEAK_THREADS, peak, Unit.NONE);
        metricContext.record(PEAK_THREADS, historicalPeak, Unit.NONE);

        mxBean.resetPeakThreadCount();
    }

}
