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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * Some JMH benchmarks on formatting numbers to strings.
 */
public class FormatBenchmarks {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"1", "5", "25", "100"})
        public int numStringBuilders;

        public List<StringBuilder> stringBuilders;

        @Setup
        public void setup() {
            stringBuilders = new ArrayList<>(numStringBuilders);
            IntStream.range(0, numStringBuilders).forEach(
                    i -> {
                        stringBuilders.add(new StringBuilder());
                    }
            );
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {

        @Param({"1", "5", "25", "100"})
        public int numRecordings;

        // Want the same numbers every run, so not using ThreadLocalRandom
        public final Random rand = new Random(8675309);

    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
    public void benchmarkDoubleFormat(BenchmarkState sbState, ThreadState tState) {
        test(sbState, tState, (sb, val) -> DoubleFormat.format(sb, val.doubleValue()));
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
    public void benchmarkStringBuilder(BenchmarkState sbState, ThreadState tState) {
        test(sbState, tState, (sb, val) -> sb.append(val.doubleValue()));
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
    public void benchmarkStringValueOf(BenchmarkState sbState, ThreadState tState) {
        test(sbState, tState, (sb, val) -> sb.append(String.valueOf(val.doubleValue())));
    }

    private void test(BenchmarkState sbState, ThreadState tState, BiConsumer<StringBuilder, Double> formatter)
    {
        sbState.stringBuilders.forEach(sb -> {
                   IntStream.range(0, tState.numRecordings).forEach(
                           i -> {
                               formatter.accept(sb, tState.rand.nextDouble());
                           }
                   );
                   String s = sb.toString();
               }
        );
    }
}
