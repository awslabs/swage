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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A CompletionService that propagates the current ThreadContext to tasks it executes.
 *
 * @param <V> type of result produced
 */
public class ContextAwareCompletionService<V> implements CompletionService<V> {

    private final CompletionService<V> delegate;

    /**
     * Constructor providing the CompletionService to use to execute the tasks.
     *
     * @param delegate an CompletionService to delegate to
     */
    public ContextAwareCompletionService(CompletionService<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<V> submit(Callable<V> task) {
        return delegate.submit(ThreadContext.current().wrap(task));
    }

    @Override
    public Future<V> submit(Runnable task, V result) {
        return delegate.submit(ThreadContext.current().wrap(task), result);
    }

    @Override
    public Future<V> take() throws InterruptedException {
        return delegate.take();
    }

    @Override
    public Future<V> poll() {
        return delegate.poll();
    }

    @Override
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.poll(timeout, unit);
    }
}
