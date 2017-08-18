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
 * The unit associated with a metric.
 *
 * <p>
 * Conceptually the same as a JSR-363 Unit, but significantly simpler.  This
 * type is provided as a quick marker for metric recording functionality where
 * conversions and other operations across Units are not required.
 * This should potentially be revisited once JSR-363 is mainstreamed.
 *
 * <p>
 * The values are taken from those supported by existing metrics systems.
 *
 */
public enum Unit {

    /**
     * Dimensionless, aka 'one'
     */
    NONE(""),

    SECOND("s"),
    MILLISECOND("ms"),
    MICROSECOND("µs"),

    PERCENT("%"),

    BYTE("B"),
    KILOBYTE("kB"),
    MEGABYTE("MB"),
    GIGABYTE("GB"),
    TERABYTE("TB"),

    BIT("b"),
    KILOBIT("kb"),
    MEGABIT("Mb"),
    GIGABIT("Gb"),
    TERABIT("Tb"),

    BYTE_PER_SEC("Bps"),
    KB_PER_SEC("kBps"),
    MB_PER_SEC("MBps"),
    GB_PER_SEC("GBps"),
    TB_PER_SEC("TBps"),
    BIT_PER_SEC("bps"),
    KBIT_PER_SEC("kbps"),
    MBIT_PER_SEC("Mbps"),
    GBIT_PER_SEC("Gbps"),
    TBIT_PER_SEC("Tbps"),

    /**
     * Dimensionless count per second
     */
    PER_SEC("ps");

    private final String symbol;

    private Unit(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return this.symbol;
    }

    public static Unit forSymbol(String symbol) {
        for (Unit u : Unit.values()) {
            if (u.symbol.equals(symbol)) {
                return u;
            }
        }
        throw new IllegalArgumentException("No Unit for symbol '"+symbol+"'");
    }

}
