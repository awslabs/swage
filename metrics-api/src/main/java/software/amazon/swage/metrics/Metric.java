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
 * Analogous to the name of a metric, constrained to valid values.
 *
 * Metric names are constrained to alpha-numeric strings of less than 250
 * characters.  Names are allowed to contain periods ('.'), colons (':'),
 * dashes ('-'), slashes ('/'), underscores ('_'), or at-symbols ('@') to
 * support organization/namespacing in downstream systems that filter solely on
 * a string name.
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
        // Check that the given string is valid for use as a metric name.
        // These constraints are taken from existing metrics backend systems.
        if (name == null || !name.matches("[a-zA-Z0-9.:/_@\\-]{0,250}")) {
            throw new IllegalArgumentException("Invalid metric name '"+name+"'");
        }

        this.name = name;
    }

    /**
     * Factory method to define a new metric with the given name.
     * Metrics should be defined once for the lifetime of an application.
     * The name will be directly used in reporting and aggregation, so must be
     * be in an accepted format.
     * Limited to 250 characters, restricted to A-Z, a-z, 0-9, and . : @ _ - or /
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
