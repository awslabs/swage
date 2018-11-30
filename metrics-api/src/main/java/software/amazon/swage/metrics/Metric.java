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
package software.amazon.swage.metrics;

/**
 * A specific metric type.
 * Analogous to the name of a metric without constraint. Metric names may
 * still be constrained by the MetricRecorder implementation.
 *
 * This class is final as there should be no need to add logic.
 * Metrics should be created as static constants, created via the define method.
 * Multiple instances created with the same name value will be considered equal
 * to reduce surprise, but it is not expected that one would use such.
 *
 * TODO: different types for sample vs. count metrics?
 * TODO: unit of metric captured here?
 */
public final class Metric {

    private final String name;

    private Metric(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Metric name may not be blank");
        }

        this.name = name;
    }

    /**
     * Factory method to define a new metric with the given name.
     * Metrics should be defined once for the lifetime of an application.
     * The name will be directly used in reporting and aggregation, so must be
     * be in an accepted format.
     *
     * <p>Names are unconstrained here, but they may still be constrained by
     * the MetricRecorder implementation.</p>
     *
     * @param name The string to use for reporting and aggregating this Metric.
     * @return A new Metric object identified by the given name.
     */
    public static Metric define(final String name) {
        return new Metric(name);
    }

    /**
     * String representation of the Metric.
     * How it will appear in the metric reporting system, such as CloudWatch.
     */
    @Override
    public final String toString() {
        return this.name;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return name.equals(((Metric) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
