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
package software.amazon.swage.metrics.scoped;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;

import org.mockito.InOrder;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;


public class ScopedMetricsTest {
    private static final Metric M_TIME = Metric.define("Time");
    private static final Metric M_PERC = Metric.define("PercentSomething");
    private static final Metric M_FAIL = Metric.define("Failure");

    // Sneaky class to expose protected methods to test against, because lazy.
    // Using this with spy to avoid complex mocking required for Context behavior.
    private static class TestRecorder extends MetricRecorder {
        @Override
        protected void record(Metric m, Number v, Unit u, Instant t, MetricContext c) {}
        @Override
        protected void count(Metric m, long d, MetricContext c) {}
    }

    @Test
    public void sample() throws Exception {
        final TestRecorder mr = spy(new TestRecorder());
        InOrder inOrder = inOrder(mr);

        ScopedMetrics.init(mr);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = ContextData.withId(id.toString()).build();

        try (AutoCloseable ig = ScopedMetrics.open(metadata)) {
            final long time = 23L;
            final Instant timestamp = Instant.now();

            final double load = 87;

            ScopedMetrics.record(M_TIME, time, Unit.MILLISECOND);
            ScopedMetrics.record(M_PERC, load, Unit.PERCENT, timestamp);

            inOrder.verify(mr).record(eq(M_TIME),
                              eq(time),
                              eq(Unit.MILLISECOND),
                              argThat(t -> Instant.now().plusNanos(1).isAfter(t)),
                              argThat(t -> metadata.equals(t.dimensions())));
            inOrder.verify(mr).record(eq(M_PERC), eq(load), eq(Unit.PERCENT), eq(timestamp), argThat(t -> metadata.equals(t.dimensions())));

            final TypedMap subctx = ContextData.withId("deadbeef").build();
            try (AutoCloseable sub = ScopedMetrics.open(subctx)) {
                final int emit = 9001;
                ScopedMetrics.record(M_TIME, emit, Unit.SECOND);

                inOrder.verify(mr).record(eq(M_TIME),
                                 eq(emit),
                                 eq(Unit.SECOND),
                                 argThat(t -> Instant.now().plusNanos(1).isAfter(t)),
                                 argThat(t -> subctx.equals(t.dimensions())));
            }
        }

    }

    @Test
    public void count() throws Exception {
        final TestRecorder mr = spy(new TestRecorder());
        InOrder inOrder = inOrder(mr);

        ScopedMetrics.init(mr);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = ContextData.withId(id.toString()).build();

        try (AutoCloseable ig = ScopedMetrics.open(metadata)) {
            ScopedMetrics.count(M_FAIL);
        }
        inOrder.verify(mr).count(eq(M_FAIL), eq(1L), argThat(t -> t.dimensions().equals(metadata)));

        final TypedMap diffdata = ContextData.withId(UUID.randomUUID().toString()).build();
        try (AutoCloseable ig = ScopedMetrics.open(diffdata)) {
            ScopedMetrics.count(M_FAIL, 3);
            inOrder.verify(mr).count(eq(M_FAIL), eq(3L), argThat(t -> t.dimensions().equals(diffdata)));

            final TypedMap subctx = ContextData.withId("deadbeef").build();
            try (AutoCloseable sub = ScopedMetrics.open(subctx)) {
                ScopedMetrics.count(M_TIME);
                inOrder.verify(mr).count(eq(M_TIME), eq(1L), argThat(t -> t.dimensions().equals(subctx)));
            }
        }
    }

    @Test
    public void sample_and_count() throws Exception {
        final TestRecorder mr = spy(new TestRecorder());
        InOrder inOrder = inOrder(mr);

        ScopedMetrics.init(mr);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = ContextData.withId(id.toString()).build();

        final Instant timestamp = Instant.now();
        final long amount = 37L;

        try (AutoCloseable ig = ScopedMetrics.open(metadata)) {
            ScopedMetrics.record(M_TIME, amount, Unit.MILLISECOND, timestamp);
            ScopedMetrics.count(M_FAIL, 11);
        }
        inOrder.verify(mr).record(eq(M_TIME), eq(amount), eq(Unit.MILLISECOND), eq(timestamp), argThat(t -> metadata.equals(t.dimensions())));
        inOrder.verify(mr).count(eq(M_FAIL), eq(11L), argThat(t -> metadata.equals(t.dimensions())));
    }

}
