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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class ContextAwareExecutorTest {
    private static final ThreadContext.Key<String> KEY = ThreadContext.key(String.class);
    private static final Executor executor = new ContextAwareExecutor(Executors.newSingleThreadExecutor());

    private final AtomicReference<ThreadContext> contextCapture = new AtomicReference<>();
    private final CountDownLatch latch = new CountDownLatch(1);

    // A task that captures the ThreadContext present when it is being executed.
    private final Runnable captureTask = () -> {
        contextCapture.set(ThreadContext.current());
        latch.countDown();
    };

    @Test
    void currentContextIsPropagatedToTask() throws Exception {
        ThreadContext context = ThreadContext.emptyContext().with(KEY, "value");
        try (ThreadContext.CloseableContext ignored = context.open()) {
            executor.execute(captureTask);
        }
        latch.await();
        assertSame(context, contextCapture.get());
    }

    @Test
    void withNoContextDefaultIsUsed() throws Exception {
        executor.execute(captureTask);
        latch.await();
        assertSame(ThreadContext.emptyContext(), contextCapture.get());
    }
}
