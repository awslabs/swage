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

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the MetricRecorder scaffolding and included Context object
 */
public class MetricRecorderTest {
    private static final Metric M_TIME = Metric.define("Time");
    private static final Metric M_PERC = Metric.define("PercentSomething");
    private static final Metric M_FAIL = Metric.define("Failure");

    // Helper to represent a metric event, for ease comparing input and output
    private static class Event {
        public final Metric metric;
        public final Number value;
        public final Unit unit;
        public final Instant timestamp;
        public final TypedMap metadata;

        private Event(Metric metric, Number value, Unit unit, Instant timestamp, TypedMap metadata) {
            this.metric = metric;
            this.value = value;
            this.unit = unit;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }
        private Event(Metric name, long delta, TypedMap metadata) {
            this.metric = name;
            this.value = Long.valueOf(delta);
            this.unit = Unit.NONE;
            this.timestamp = null;
            this.metadata = metadata;
        }
    }

    // Simple impl that echos things out to a list
    private MetricRecorder testRecorder(List<Event> output) {
        return new MetricRecorder() {
            @Override
            protected void record(Metric label, Number value, Unit unit, Instant time, TypedMap metadata) {
                output.add(new Event(label, value, unit, time, metadata));
            }

            @Override
            protected void count(Metric metric, long delta, TypedMap metadata) {
                output.add(new Event(metric, delta, metadata));
            }
        };
    }

    @Test
    public void sample() throws Exception {
        final List<Event> output = new ArrayList<>(2);
        final MetricRecorder mr = testRecorder(output);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = makeContext(id.toString());
        final MetricRecorder.Context context = mr.context(metadata);

        final Number time = Integer.valueOf(23);
        final Instant timestamp = Instant.now();

        context.record(M_TIME, time, Unit.MILLISECOND, timestamp);

        assertEquals("Unexpected number of metrics output", 1, output.size());
        final Event e1 = output.get(0);
        assertEquals("Wrong metric", M_TIME, e1.metric);
        assertEquals("Wrong value", time, e1.value);
        assertEquals("Wrong unit", Unit.MILLISECOND, e1.unit);
        assertTrue("Wrong timestamp", e1.timestamp.equals(timestamp));
        assertEquals("Wrong benchmark", metadata, e1.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e1.metadata.get(ContextData.ID));

        final Number load = Integer.valueOf(87);
        context.record(M_PERC, load, Unit.PERCENT, timestamp);

        assertEquals("Unexpected number of metrics output", 2, output.size());
        final Event e2 = output.get(1);
        assertEquals("Wrong metric", M_PERC, e2.metric);
        assertEquals("Wrong value", load, e2.value);
        assertEquals("Wrong unit", Unit.PERCENT, e2.unit);
        assertTrue("Wrong timestamp", e2.timestamp.equals(timestamp));
        assertEquals("Wrong benchmark", metadata, e2.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e2.metadata.get(ContextData.ID));
    }

    @Test
    public void count() throws Exception {
        final List<Event> output = new ArrayList<>(2);
        final MetricRecorder mr = testRecorder(output);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = makeContext(id.toString());
        final MetricRecorder.Context context = mr.context(metadata);


        context.count(M_FAIL, 1);

        assertEquals("Unexpected number of metrics output", 1, output.size());
        final Event e1 = output.get(0);
        assertEquals("Wrong metric", M_FAIL, e1.metric);
        assertEquals("Wrong value", 1L, e1.value);
        assertEquals("Wrong benchmark", metadata, e1.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e1.metadata.get(ContextData.ID));

        context.count(M_FAIL, 3);

        assertEquals("Unexpected number of metrics output", 2, output.size());
        final Event e2 = output.get(1);
        assertEquals("Wrong metric", M_FAIL, e2.metric);
        assertEquals("Wrong value", 3L, e2.value);
        assertEquals("Wrong benchmark", metadata, e2.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e2.metadata.get(ContextData.ID));
    }

    @Test
    public void sample_and_count() throws Exception {
        final List<Event> output = new ArrayList<>(2);
        final MetricRecorder mr = testRecorder(output);

        final UUID id = UUID.randomUUID();
        final TypedMap metadata = makeContext(id.toString());
        final MetricRecorder.Context context = mr.context(metadata);

        final Number amount = Integer.valueOf(37);

        context.record(M_TIME, amount, Unit.MILLISECOND);
        assertEquals("Unexpected number of metrics output", 1, output.size());
        final Event e1 = output.get(0);
        assertEquals("Wrong metric", M_TIME, e1.metric);
        assertEquals("Wrong value", amount, e1.value);
        assertEquals("Wrong unit", Unit.MILLISECOND, e1.unit);
        assertEquals("Wrong benchmark", metadata, e1.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e1.metadata.get(ContextData.ID));

        context.count(M_FAIL, 11);
        assertEquals("Unexpected number of metrics output", 2, output.size());
        final Event e2 = output.get(1);
        assertEquals("Wrong metric", M_FAIL, e2.metric);
        assertEquals("Wrong value", 11L, e2.value);
        assertEquals("Wrong benchmark", metadata, e2.metadata);
        assertEquals("Wrong REQUEST_ID", id.toString(), e2.metadata.get(ContextData.ID));
    }

    @Test
    public void close() throws Exception {
        final Map<TypedMap, Boolean> closed = new HashMap<>();

        MetricRecorder mr = new MetricRecorder() {
            @Override
            protected void record(Metric a, Number b, Unit c, Instant d, TypedMap e) { }
            @Override
            protected void count(Metric a, long b, TypedMap c) { }

            @Override
            protected void close(TypedMap metadata) {
                closed.put(metadata, true);
            }
        };

        final TypedMap ctxt_1 = makeContext(UUID.randomUUID().toString());
        final TypedMap ctxt_2 = makeContext(UUID.randomUUID().toString());
        closed.put(ctxt_1, false);
        closed.put(ctxt_2, false);

        final MetricRecorder.Context r_ctxt_1 = mr.context(ctxt_1);
        final MetricRecorder.Context r_ctxt_2 = mr.context(ctxt_2);
        r_ctxt_1.record(M_TIME, 1, Unit.MILLISECOND);
        r_ctxt_2.record(M_TIME, 2, Unit.MILLISECOND);

        r_ctxt_1.close();

        assertTrue("Context not closed", closed.get(ctxt_1));
        assertFalse("Unassociated benchmark unexpectedly closed", closed.get(ctxt_2));
    }

    private TypedMap makeContext(final String id) {
        return ImmutableTypedMap.Builder.with(ContextData.ID, id).build();
    }
}
