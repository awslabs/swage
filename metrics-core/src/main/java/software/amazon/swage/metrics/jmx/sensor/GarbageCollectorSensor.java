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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Records garbage collection metrics as reported by the GarbageCollectorMXBean.
 *
 */
public class GarbageCollectorSensor implements Sensor {

    /**
     * Total amount of garbage collections that have occurred since this sensor
     * was last run
     */
    public static final Metric COLLECTION_COUNT = Metric.define("GarbageCollectionCount");

    /**
     * Approximate accumulated collection elapsed time since this sensor was
     * last run
     */
    public static final Metric COLLECTION_TIME = Metric.define("GarbageCollection");

    /**
     * Total amount of garbage collections that have occurred for the JVM
     */
    public static final Metric COLLECTION_COUNT_TOTAL = Metric.define("GarbageCollectionCountTotal");

    /**
     * Approximate accumulated collection elapsed time for the JVM
     */
    public static final Metric COLLECTION_TIME_TOTAL = Metric.define("GarbageCollectionTotal");


    private long prevTotalCollections = 0L;
    private long prevTotalTime = 0L;

    @Override
    public void sense(final MetricRecorder.Context metricContext)
    {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // sum up metrics for all the garbage collectors
        //TODO: individual metrics per gc?
        long totalCollections = 0L;
        long totalTime = 0L;
        for (GarbageCollectorMXBean mxBean : gcBeans) {
            totalCollections += mxBean.getCollectionCount();
            totalTime += mxBean.getCollectionTime();
        }

        metricContext.record(COLLECTION_COUNT_TOTAL, totalCollections, Unit.NONE);
        metricContext.record(COLLECTION_TIME_TOTAL, totalTime, Unit.MILLISECOND);

        metricContext.record(COLLECTION_COUNT, (totalCollections - prevTotalCollections), Unit.NONE);
        metricContext.record(COLLECTION_TIME, (totalTime - prevTotalTime), Unit.MILLISECOND);

        prevTotalCollections = totalCollections;
        prevTotalTime = totalTime;
    }

}
