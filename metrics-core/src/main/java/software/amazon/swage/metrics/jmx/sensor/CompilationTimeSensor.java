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

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

/**
 * Records compilation time metrics as reported by the CompilationMXBean.
 *
 * Useful for seeing how much latency is attributable due to the JVM still
 * interpreting bytecode on-the-fly.  Will emit a metric for total time spent
 * in compilation and amount of compilation time spent since the last time
 * this {@link Sensor} was run.
 * 
 * Most Java services see a significant impact in latency when they bounce the
 * JVM.  Though this warm-up can be due to service level caching, there is a
 * large chunk of it that is due to JIT warm-up and compilation time.  With
 * this metric, service owners can get more visibility into how much JVM
 * compilation time is impacting their top-level latencies.  This visibility
 * can provide service owners with a quick time to resolution of latency
 * investigations.
 * 
 */
public class CompilationTimeSensor implements Sensor
{

    /**
     * The amount of time spent in compilation since the last time the senor ran
     */
    public static final Metric COMPILATION_TIME = Metric.define("CompilationTime");

    /**
     * The approximate accumulated elapsed time spent in compilation, summed for
     * all threads
     */
    public static final Metric TOTAL_COMPILATION_TIME = Metric.define("TotalCompilationTime");

    private long prevTotal = 0L;

    @Override
    public void sense(final MetricContext metricContext)
    {
        CompilationMXBean mxBean = ManagementFactory.getCompilationMXBean();

        // Compilation time may not be supported on some platforms, skip if so.
        if (!mxBean.isCompilationTimeMonitoringSupported()) {
            return;
        }

        long total = mxBean.getTotalCompilationTime();
        metricContext.record(TOTAL_COMPILATION_TIME, total, Unit.MILLISECOND);
        metricContext.record(COMPILATION_TIME, total - prevTotal, Unit.MILLISECOND);

        this.prevTotal = total;
    }

}
