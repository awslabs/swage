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

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Records system information, as reported by the RuntimeMXBean.
 *
 */
public class RuntimeSensor implements Sensor {

    public static final Metric UPTIME = Metric.define("UpTime");

    @Override
    public TypedMap addContext(final TypedMap existing)
    {
        // TODO: pull out jvm runtime attributes into context
        return existing;
    }

    @Override
    public void sense(final MetricRecorder.Context metricContext)
    {
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();

        metricContext.record(UPTIME, mxBean.getUptime(), Unit.MILLISECOND);
    }

}
