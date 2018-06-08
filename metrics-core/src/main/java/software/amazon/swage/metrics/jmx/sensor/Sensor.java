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
import software.amazon.swage.metrics.MetricRecorder;

/**
 * Pull metadata of one particular type from one particular M(X)Bean and send to a
 * MetricRecorder.
 *
 * Sensors are not required to be thread safe.
 */
public interface Sensor {

    /**
     * Add any additional context metadata to the existing context, return a new
     * instance of context metadata.  Implementations that have no metadata to add
     * return the existing context unchanged.
     *
     * @param existing Context metadata already known
     * @return A TypedMap containing all entries of the existing context with
     *         sensor-specific metadata added
     */
    default public TypedMap addContext(final TypedMap existing)
    {
        return existing;
    }

    /**
     * Perform some set of measurements against the provided {@code connection} and record
     * results to {@code metrics}.  Must NOT close the {@code Context} object as other
     * {@code Sensor} may also contribute to it.
     *
     * @param metricContext Metrics context which will be used to record metadata
     * @throws SenseException When a problem occurred collecting measurements
     */
    public void sense(final MetricRecorder.Context metricContext) throws SenseException;

}
