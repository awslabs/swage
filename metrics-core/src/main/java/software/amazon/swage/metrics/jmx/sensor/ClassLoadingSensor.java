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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

/**
 * Records information about class loading in the JVM, as reported by the
 * ClassLoadingMXBean.
 */
public class ClassLoadingSensor implements Sensor {

    /**
     * The number of classes that are currently loaded in the JVM
     */
    public static final Metric CURRENT_COUNT = Metric.define("LoadedClassCount");

    /**
     * The total number of classes that have been loaded since the JVM started
     */
    public static final Metric TOTAL_COUNT = Metric.define("LoadedClassCountTotal");

    /**
     * The total number of classes that have been unloaded since the JVM started
     */
    public static final Metric UNLOADED_COUNT = Metric.define("UnLoadedClassCountTotal");

    @Override
    public void sense(final MetricRecorder.Context metricContext)
    {
        ClassLoadingMXBean mxBean = ManagementFactory.getClassLoadingMXBean();

        metricContext.record(CURRENT_COUNT, mxBean.getLoadedClassCount(), Unit.NONE);
        metricContext.record(TOTAL_COUNT, mxBean.getTotalLoadedClassCount(), Unit.NONE);
        metricContext.record(UNLOADED_COUNT, mxBean.getUnloadedClassCount(), Unit.NONE);
    }

}
