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
package software.amazon.swage.threadcontext;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


public class ThreadContextTest {

    private static final ThreadContext.Key<String> REQUEST_ID = ThreadContext.key(String.class);
    private static final ThreadContext.Key<String> TRACE_ID = ThreadContext.key(String.class);
    private static final ThreadContext.Key<Double> HEIGHT = ThreadContext.key(Double.class);

    @Test
    public void withNoActiveContextAnEmptyContextIsReturned() {
        ThreadContext context = ThreadContext.current();
        assertSame(ThreadContext.emptyContext(), context);
    }

    @Test
    public void contextReturnsValueForKey() {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "hello");
        assertEquals("hello", REQUEST_ID.get(context));
    }

    @Test
    public void contextReturnsNullForMissingKey() {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "hello");
        assertNull(TRACE_ID.get(context));
    }

    @Test
    public void defaultIsSuppliedForMissingKey() {
        ThreadContext context = ThreadContext.emptyContext();
        assertEquals("hello", context.getOrElseGet(REQUEST_ID, () -> "hello"));
    }

    @Test
    public void keyValuesAreAdditive() {
        ThreadContext empty = ThreadContext.emptyContext();
        ThreadContext oneKey = empty.with(REQUEST_ID, "rid");
        ThreadContext twoKey = oneKey.with(TRACE_ID, "tid");

        assertNull(empty.get(REQUEST_ID));
        assertNull(empty.get(TRACE_ID));
        assertEquals("rid", oneKey.get(REQUEST_ID));
        assertNull(oneKey.get(TRACE_ID));
        assertEquals("rid", twoKey.get(REQUEST_ID));
        assertEquals("tid", twoKey.get(TRACE_ID));
    }

    @Test
    public void keyValuesAreOverride() {
        ThreadContext oneKey = ThreadContext.emptyContext().with(REQUEST_ID, "rid1");
        ThreadContext overide = oneKey.with(REQUEST_ID, "rid2");

        assertEquals("rid1", oneKey.get(REQUEST_ID));
        assertEquals("rid2", overide.get(REQUEST_ID));
    }

    @Test
    public void multipleValuesCanBeAdded() {
        Double h = Double.valueOf(3.14159);

        Map<ThreadContext.Key, Object> values = new HashMap<>();
        values.put(REQUEST_ID, "rid");
        values.put(TRACE_ID, "tid");
        values.put(HEIGHT, h);
        ThreadContext context = ThreadContext.emptyContext().with(values);

        assertEquals("rid", context.get(REQUEST_ID));
        assertEquals("tid", context.get(TRACE_ID));
        assertEquals(h, context.get(HEIGHT));
    }

    @Test
    public void contextIsPassedToRunnable() {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        context.wrap(() -> assertSame(context, ThreadContext.current())).run();
        assertSame(ThreadContext.emptyContext(), ThreadContext.current());
    }

    @Test
    public void contextIsPassedToCallable() throws Exception {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        String result = context.wrap((Callable<String>)() -> {
            assertSame(context, ThreadContext.current());
            return "value";
        }).call();
        assertSame(ThreadContext.emptyContext(), ThreadContext.current());
        assertEquals("value", result);
    }

    @Test
    public void contextIsPassedToSupplier() throws Exception {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        String result = context.wrap((Supplier<String>)() -> {
            assertSame(context, ThreadContext.current());
            return "value";
        }).get();
        assertSame(ThreadContext.emptyContext(), ThreadContext.current());
        assertEquals("value", result);
    }

    @Test
    public void contextIsDetachedOnClose() {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        try (ThreadContext.CloseableContext ignored = context.open()) {
            assertSame(context, ThreadContext.current());
        }
        assertSame(ThreadContext.emptyContext(), ThreadContext.current());
    }

    @Test
    public void contextIsRestoredOnClose() {
        ThreadContext outer = ThreadContext.emptyContext().with(TRACE_ID, "tid");
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        outer.wrap(() -> {
            assertSame(outer, ThreadContext.current());
            try (ThreadContext.CloseableContext ignored = context.open()) {
                assertSame(context, ThreadContext.current());
            }
            assertSame(outer, ThreadContext.current());
        }).run();
    }

    @Test
    public void keyAccessesCurrentContext() {
        ThreadContext context = ThreadContext.emptyContext().with(REQUEST_ID, "rid");
        assertNull(REQUEST_ID.current());
        context.wrap(() -> assertEquals("rid", REQUEST_ID.current())).run();
    }

    @Test
    public void wraperContextIsUsedEvenIfOuterContextExists() {
        ThreadContext outer = ThreadContext.emptyContext().with(REQUEST_ID, "rid1");
        ThreadContext inner = ThreadContext.emptyContext().with(REQUEST_ID, "rid2");
        Runnable wrapped = inner.wrap(() -> assertSame(inner, ThreadContext.current()));
        try (ThreadContext.CloseableContext ignored = outer.open()) {
            wrapped.run();
        }
    }
}
