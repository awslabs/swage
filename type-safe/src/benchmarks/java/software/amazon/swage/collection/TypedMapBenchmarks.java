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
package com.amazon.collection;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * A set of JMH benchmarks to compare various implementations of TypedMap.
 */
public class TypedMapBenchmarks {

    // Want some semblance of repeatablity, so not using ThreadLocalRandom
    private static final ThreadLocal<Random> rand = ThreadLocal.withInitial(() -> new Random(8675309));

    @State(Scope.Benchmark)
    public static class AppState {

        @Param({"0", "1", "5", "25", "100"})
        public int numDataEntries;

        List<TypedMap.Key> dataKeys;

        DataConsumer sink;

        @Setup
        public void setup() {
            this.sink = new DataConsumer();

            dataKeys = new ArrayList<>(numDataEntries);
            IntStream.range(0, numDataEntries).forEach(
                    i -> {
                        dataKeys.add(TypedMap.key("Key" + rand.get().nextInt(), String.class));
                    }
            );
        }
    }

    @State(Scope.Thread)
    public static class ContextState {

        @Param({"1", "5", "25", "100"})
        public int numRecordings;

        //TODO: test some data other than strings
        List<String> datas;

        @Setup
        public void setup(BenchmarkParams params) {
            int numDatas = Integer.valueOf(params.getParam("numDataEntries"));

            datas = new ArrayList<>(numDatas);
            IntStream.range(0, numDatas).forEach(
                    i -> {
                        datas.add("rando"+rand.get().nextInt());
                    }
            );
        }
    }

    @Benchmark
    public void benchmarkImmutableTypedMap(AppState appState, ContextState ctxState)
    {
        TypedMap data;
        if (appState.numDataEntries == 0) {
            data = TypedMap.empty();
        } else {
            final ImmutableTypedMap.Builder builder = ImmutableTypedMap.Builder
                    .with(appState.dataKeys.get(0), ctxState.datas.get(0));

            IntStream.range(1, appState.numDataEntries).forEach(
                    i -> {
                        builder.add(appState.dataKeys.get(i), ctxState.datas.get(i));
                    }
            );
            data = builder.build();
        }

        IntStream.range(0, ctxState.numRecordings).forEach(
                i -> {
                    appState.sink.useData(data);
                }
        );
    }

    @Benchmark
    public void benchmarkListTypedMap(AppState appState, ContextState ctxState)
    {
        TypedMap data;
        if (appState.numDataEntries == 0) {
            data = TypedMap.empty();
        } else {
            final ListTypedMap.Builder builder = ListTypedMap.Builder
                    .with(appState.dataKeys.get(0), ctxState.datas.get(0));
            IntStream.range(1, appState.numDataEntries).forEach(
                    i -> {
                        builder.add(appState.dataKeys.get(i), ctxState.datas.get(i));
                    }
            );
            data = builder.build();
        }

        IntStream.range(0, ctxState.numRecordings).forEach(
                i -> {
                    appState.sink.useData(data);
                }
        );
    }

    @Benchmark
    public void benchmarkGuava(AppState appState, ContextState ctxState) {
        TypedMap data;
        if (appState.numDataEntries == 0) {
            data = TypedMap.empty();
        } else {
            final GuavaTypedMap.Builder builder = GuavaTypedMap.Builder
                    .with(appState.dataKeys.get(0), ctxState.datas.get(0));
            IntStream.range(1, appState.numDataEntries).forEach(
                    i -> {
                        builder.add(appState.dataKeys.get(i), ctxState.datas.get(i));
                    }
            );
            data = builder.build();
        }

        IntStream.range(0, ctxState.numRecordings).forEach(
                i -> {
                    appState.sink.useData(data);
                }
        );
    }


    private static class DataConsumer
    {
        void useData(final TypedMap data) {
            StringBuilder dump = new StringBuilder();
            data.forEach(
                    (e) -> {
                        String k = e.getKey().name;
                        String v = e.getValue().toString();
                        dump.append("key:").append(k).append(",value:").append(v).append("\n");
                    }
            );
            // and just drop it on the floor
        }
    }

}
