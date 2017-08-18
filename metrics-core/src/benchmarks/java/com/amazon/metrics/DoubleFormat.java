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
package com.amazon.metrics;

import java.io.IOException;

/**
 * DoubleFormat provides a performance-oriented double-to-String conversion,
 * customized for a specific metric log output.
 *
 * Empirical tests show this is at minimum twice as fast as calling
 * append(double) on StringBuilder, which is itself much faster than using
 * the standard library's NumberFormat.
 *
 * A test to validate this is in DoubleFormatTest.  Please revisit occasionally.
 *
 */
public final class DoubleFormat {

    // How many digits of precision after the decimal point
    private static final int MAX_FRACTIONAL_DIGITS = 6;

    // Lookup table for powers of 10
    private final static double[] POW_10 = new double[]{
            1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };

    // Maximum value for fractional part of number when converted to integer
    private static final double MAX = POW_10[MAX_FRACTIONAL_DIGITS];

    // Whole number value beyond which this formatter cannot function
    // (Will fallback to slower Java API)
    private final static double TOO_BIG_THRESHOLD = (double)(0x7FFFFFFF);


    // Prevent instantiation
    private DoubleFormat() {}

    /**
     * Formats a double and appends it to an existing Appendable object,
     * generally a StringBuilder.
     *
     * To conform to the interface of other format methods, any IOException
     * thrown by calls to the Appendable will be considered in improper state,
     * and will throw an IllegalStateException.
     * This is not ideal.
     *
     * @param sb
     * @param value
     */
    public static void format(Appendable sb, double value) {
        try {
            format_exception(sb, value);
        } catch (IOException e) {
            throw new IllegalStateException("Appendable must not throw IOException", e);
        }
    }

    // Actual work of format, wrapped above to capture/convert exception
    private static void format_exception(Appendable sb, double value) throws IOException {
        // store the sign of the input, get the input as a positive number w/o sign
        boolean negative = (value < 0);
        double positiveValue = Math.abs(value);

        // if value is too large for us to handle, revert to builtin formatter
        if (positiveValue > TOO_BIG_THRESHOLD) {
            handleInputTooBig(sb, value);
            return;
        }

        // Separate the number into whole number and fraction components.
        // Scale the fraction component based upon the max number of digits to display.
        // Additionally, store remaining (unprinted) fraction amount for use in rounding
        // decisions.
        int whole = (int) positiveValue;
        double tmp = (positiveValue - whole) * MAX;
        int fraction = Math.abs((int) tmp);
        double diff = tmp - fraction;

        int comp = Double.compare(diff, 0.5);
        if(comp > 0) {
            // round up
            fraction++;
        } else if(comp == 0 && ((fraction == 0) || (fraction & 1) != 0)) {
            // round up
            fraction++;
        }

        // if rounding has pushed us up to the next whole number, adjust:
        // clear fraction amount and increment whole number
        if(fraction >= MAX) {
            fraction = 0;
            whole++;
        }

        int leadingZeros = countLeadingZeros(fraction);
        fraction = trimTrailingZeros(fraction);

        // Once the double value has been appropriately split and rounded, write as a string
        if(negative) {
            sb.append('-');
        }
        sb.append(Integer.toString(whole));

        if(fraction != 0) {
            sb.append('.');
            for(int i = 0; i < leadingZeros; i++) {
                sb.append('0');
            }
            sb.append(Integer.toString(fraction));
        }
    }

    // Counts the number of digits between the decimal point and the first non-zero
    // in the fraction.  As this is done in integer arithmetic, we simply move the
    // decimal point right one place at a time until the fraction exceeds '1' in
    // the space of the original number (before splitting whole from fraction).
    private static int countLeadingZeros(int fraction) {
        int max = (int) MAX;

        // detect this error because otherwise you get infinite looping...
        if(fraction < 0) {
            throw new IllegalArgumentException("Negative fraction: " + fraction);
        }

        // if the fraction is zero, specify minimumFractionDigits-1 as the zero count
        // otherwise no fraction will be displayed.
        if(fraction == 0) {
            return -1;
         }

        int count = 0;
        while( (fraction *= 10) < max ) {
            count++;
        }

        return count;
    }

    // Removes trailing zeros from the fraction
    private static int trimTrailingZeros(int fraction) {

        for(int i = 0; i < MAX_FRACTIONAL_DIGITS; i++) {
            if(fraction % 10 != 0) {
                return fraction;
            }
            else {
                fraction /= 10;
            }
        }

        return fraction;
    }

    // DoubleFormat can only handle values whose whole number component can be
    // stored in a 32-bit integer.  Anything larger than this must fallback to
    // the java API's formatter, which is much slower.
    private static void handleInputTooBig(Appendable sb, double value) throws IOException {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance();
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(MAX_FRACTIONAL_DIGITS);
        format.setGroupingUsed(false);

        sb.append(format.format(value));
    }

}
