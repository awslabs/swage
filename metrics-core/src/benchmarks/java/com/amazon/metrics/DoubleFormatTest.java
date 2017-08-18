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

import com.amazon.metrics.DoubleFormat;
import org.junit.Test;

import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DoubleFormatTest {

    private void test(double val) throws Exception {
        // Should behave exactly the same as standard number format
        // with no extra trailing zeros, six max trailing zeros.
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);

        String expected = nf.format(val);

        StringBuilder sb = new StringBuilder();

        DoubleFormat.format(sb, val);

        assertEquals(expected, sb.toString());
    }

    @Test
    public void positive() throws Exception {
        test(12345.6789);
    }

    @Test
    public void negative() throws Exception {
        test(-12345.6789);
    }

    @Test
    public void lessThreshold() throws Exception {
        test(9.8765432123);
    }

    @Test
    public void roundUp() throws Exception {
        test(0.999999996);
    }

    @Test
    public void edge() throws Exception {
        test(0.999999995);
    }

    @Test
    public void roundDown() throws Exception {
        test(0.999999994);
    }

    @Test
    public void zero() throws Exception {
        test(0.0000);
    }

    @Test
    public void fraction() throws Exception {
        test(0.123);
    }

    @Test
    public void integer() throws Exception {
        test(1.0000);
    }

    @Test
    public void leadingZerosInDecimal() throws Exception {
        test(1.000123);
    }

    @Test
    public void tooBig() throws Exception {
        test(Double.MAX_VALUE);
    }

    @Test
    public void leadingZerosIntegerOverflow() throws Exception {
        test(0.6972461198092492);
        test(0.3957474025628186);
        test(0.22607775429405264);
        test(0.3291082710169503);
        test(0.32921492069249947);
        test(0.18868304981374573);
        test(0.10704062259380864);
        test(0.5517452296495586);
        test(0.6763267440490248);
        test(0.6644974991174629);
        test(0.0058853381799743865);
        test(0.23972173153373955);
        test(0.3000722792766619);
        test(0.37517415521245345);
        test(0.012308646565343029);
        test(0.06245844233466158);
        test(0.8257932602390585);
        test(0.3542880017571749);
        test(0.08525139832896267);
        test(0.13705051900190313);
        test(0.7028752585515867);
        test(0.7179510797870567);
        test(0.7601627632324963);
        test(0.5748998978654571);
        test(0.16737961281134406);
        test(0.7665142427379052);
        test(0.3717073105453246);
        test(0.6835382112082834);
        test(0.28727209275905263);
    }

    /**
     * This test runs a comparision of the performance of various string
     * formatting methods and validates that DoubleFormat is the fastest.
     * Not a replacement for in-depth analysis.
     *
     * If this test is failing, either DoubleFormat has a performance
     * regression or the built-in methods have improved enough to reconsider.
     *
     * The test can take multiple seconds to run.
     */
    @Test
    public void benchmark() throws Exception {
        final long iterations = 1000000;

        long duration_StringFormat = run_benchmark(iterations, (a) -> String.format("%.6f", a));

        NumberFormat javaFormatter = NumberFormat.getInstance();
        javaFormatter.setMinimumFractionDigits(0);
        javaFormatter.setMaximumFractionDigits(6);
        javaFormatter.setGroupingUsed(false);
        long duration_JavaFormat = run_benchmark(iterations, (a) -> javaFormatter.format(a));

        long duration_JavaStringBuilder = run_benchmark(iterations, (a) -> (new StringBuilder()).append(a));

        long duration_DoubleFormat = run_benchmark(iterations, (a) -> DoubleFormat.format(new StringBuilder(), a));

        System.out.println("DoubleFormat Performance comparison: " + iterations +" iterations");
        System.out.println("\tJava String.format: " + duration_StringFormat + " ms");
        System.out.println("\tJava NumberFormat:  " + duration_JavaFormat + " ms");
        System.out.println("\tJava StringBuilder: " + duration_JavaStringBuilder + " ms");
        System.out.println("\tDoubleFormat:       " + duration_DoubleFormat + " ms");

        assertTrue(duration_DoubleFormat < duration_StringFormat);
        assertTrue(duration_DoubleFormat < duration_JavaFormat);
        assertTrue(duration_DoubleFormat < duration_JavaStringBuilder);
    }

    private long run_benchmark(long iterations, DoubleConsumer test) {
        long seed = 12345;
        Random rand = new Random();
        rand.setSeed(seed);

        long startTime = System.nanoTime();
        for(int i = 0; i < iterations; i++) {
            test.accept(rand.nextDouble());
        }

        long endTime = System.nanoTime();
        return TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
    }

}
