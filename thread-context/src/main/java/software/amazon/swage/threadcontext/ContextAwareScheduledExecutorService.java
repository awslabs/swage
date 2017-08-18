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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A ScheduledExecutorService that propagates current ThreadContext to tasks being run.
 */
public class ContextAwareScheduledExecutorService extends ContextAwareExecutorService implements ScheduledExecutorService {
    private final ScheduledExecutorService delegate;

    /**
     * Constructor providing the ScheduledExecutorService to use to execute the tasks.
     *
     * @param delegate a ScheduledExecutorService to delegate to
     */
    public ContextAwareScheduledExecutorService(ScheduledExecutorService delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(ThreadContext.current().wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate.schedule(ThreadContext.current().wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(ThreadContext.current().wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(ThreadContext.current().wrap(command), initialDelay, delay, unit);
    }
}
