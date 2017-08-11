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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * Records OS level information, as reported by the OperatingSystemMXBean.
 *
 */
public class OperatingSystemSensor implements Sensor {

    /**
     * The system load average for the last minute.
     */
    public static final Metric SYSTEM_LOAD = Metric.define("SystemLoadAvg");

    /**
     * Percentage of maximum file descriptors currently in use
     */
    public static final Metric FILE_DESCRIPTORS_USED = Metric.define("FileDescriptorUse");

    /**
     * Number of currently open file descriptors
     */
    public static final Metric FILE_DESCRIPTORS_OPEN = Metric.define("OpenFileDescriptors");

    /**
     * Maximum available file descriptors
     */
    public static final Metric FILE_DESCRIPTORS_MAX = Metric.define("TotalFileDescriptors");


    @Override
    public void sense(final MetricRecorder.Context metricContext)
    throws SenseException
    {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();

        double load = mxBean.getSystemLoadAverage();
        if (load >= 0) {
            metricContext.record(SYSTEM_LOAD, load, Unit.NONE);
        }


        // This is the same bean, but the attributes we want aren't on the
        // OperatingSystemMXBean interface.
        ObjectName on;
        try {
            on = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            // This should never happen
            throw new IllegalStateException("JVM-provided constant name is malformed");
        }

        final MBeanServerConnection connection = ManagementFactory.getPlatformMBeanServer();
        try {
            //TODO: find a better way to do this
            Long max = (Long) connection.getAttribute(on, "MaxFileDescriptorCount");
            metricContext.record(FILE_DESCRIPTORS_MAX, max, Unit.NONE);

            Long open = (Long) connection.getAttribute(on, "OpenFileDescriptorCount");
            metricContext.record(FILE_DESCRIPTORS_OPEN, open, Unit.NONE);

            if (open >= 0) {
                double use_percent = 100.0 * ((double) open / (double) max);
                metricContext.record(FILE_DESCRIPTORS_USED, use_percent, Unit.PERCENT);
            }
        }
        catch (MBeanException|AttributeNotFoundException|InstanceNotFoundException|ReflectionException|IOException e)
        {
            throw new SenseException("Failure reading OpenFiledDescriptorCount from OS MBean", e);
        }

    }

}
