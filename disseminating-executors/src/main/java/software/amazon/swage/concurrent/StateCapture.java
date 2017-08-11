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
package software.amazon.swage.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * A class that allows plugins to provide strategies to copy data and state
 * from one thread to another.
 */
public final class StateCapture {

    private static final Logger log = Logger.getLogger(StateCapture.class.toString());
    private static final List<StateCaptor<?>> stateCaptors;

    static {
        ClassLoader classLoader;
        /* This system property is available to accommodate customers who need StateCaptors to be discovered and
         * loaded from the context class loader. This is a legacy requirement, and this system property should not be
         * enabled unless absolutely necessary. It will eventually be removed.
         */
        if (System.getProperty("StateCapture.useContextClassLoader") == null) {
            classLoader = StateCapture.class.getClassLoader();
        } else {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        @SuppressWarnings("rawtypes")
        ServiceLoader<StateCaptor> serviceLoader = ServiceLoader.load(StateCaptor.class, classLoader);

        final List<StateCaptor<?>> registeredStateCaptors = new ArrayList<StateCaptor<?>>();
        for (StateCaptor<?> stateCaptor : serviceLoader) {
            registeredStateCaptors.add(stateCaptor);
            log.fine("Registering state captor: " + stateCaptor.getClass().getCanonicalName());
        }

        if (registeredStateCaptors.isEmpty()) {
            log.fine("Found no state captors to register.");
            stateCaptors = Collections.emptyList();
        } else {
            stateCaptors = Collections.unmodifiableList(registeredStateCaptors);
        }
    }

    private StateCapture() {}

    /**
     * Creates an Executor with a <code>submit</code> method that calls each registered
     * {@code StateCaptor} in the context of the calling thread,
     * wraps the provided runnable with one containing the returned captured states in
     * its closure, then submits the new runnable to the delegate executor.
     *
     * If the passed in executor is already decorated, or there are no registered
     * state captors, the delegate executor is returned unmodified.
     *
     * @param executor executor to delegate execution to
     * @return an executor that wraps Runnables
     */
    public static Executor capturingDecorator(final Executor executor) {
        if (stateCaptors.isEmpty() || executor instanceof CapturingExecutor) {
            return executor;
        } else {
            return new CapturingExecutor(executor);
        }
    }

    /**
     * Creates an ExecutorService with <code>submit, invokeAll</code> and <code>invokeAny> methods
     * that calls each registered {@code StateCaptor} in the context of the calling thread,
     * wraps the provided runnable or callable with one containing the returned captured state in
     * its closure, then submits the wrapped runnable or callable to the delegate executor service.
     *
     * In the case of <code>invokeAll</code> and <code>invokeAny</code>, the factories are called once
     * per Callable.
     *
     * If the passed in executor service is already decorated, or there are no registered
     * state captors, the delegate executor service is returned unmodified.
     *
     * @param executor executor service to delegate execution to
     * @return an executor service that wraps Runnables / Callables
     */
    public static ExecutorService capturingDecorator(final ExecutorService executorService) {
        if (stateCaptors.isEmpty() || executorService instanceof CapturingExecutorService) {
            return executorService;
        } else {
            return new CapturingExecutorService(executorService);
        }
    }

    /**
     * Creates an ScheduledExecutorService with <code>submit, invokeAll</code> and <code>invokeAny> methods
     * that calls each registered {@code StateCaptor} in the context of the calling thread,
     * wraps the provided runnable or callable with one containing the returned captured state in
     * its closure, then submits the wrapped runnable or callable to the delegate executor service.
     *
     * In the case of <code>invokeAll</code> and <code>invokeAny</code>, the factories are called once
     * per Callable.
     *
     * If the passed in executor service is already decorated, or there are no registered
     * state captors, the delegate executor service is returned unmodified.
     *
     * @param executorService a scheduled executor service to delegate execution to
     * @return a scheduled executor service that wraps Runnables
     */
    public static ScheduledExecutorService capturingDecorator(final ScheduledExecutorService executorService) {
        if (stateCaptors.isEmpty() || executorService instanceof CapturingExecutorService) {
            return executorService;
        } else {
            return new CapturingScheduledExecutorService(executorService);
        }
    }

    /**
     * Creates an CompletionService with a <code>submit</code> method that calls each
     * registered {@code StateCaptor} in the context of the calling thread,
     * wraps the provided runnable or callable with one containing the returned captured states in
     * its closure, then submits the wrapped runnable or callable to the delegate completion service.
     *
     * If the passed in completion service is already decorated, or there are no registered
     * state captors, the delegate executor service is returned unmodified.
     *
     * @param completionService completion service to delegate execution to
     * @return an completion service that calls registered state captors and propagates
     *  the captured state.
     */
    public static <T> CompletionService<T> capturingDecorator(final CompletionService<T> completionService) {
        if (stateCaptors.isEmpty() || completionService instanceof CapturingCompletionService) {
            return completionService;
        } else {
            return new CapturingCompletionService<T>(completionService);
        }
    }

    /**
     * Creates a runnable that allows capturing state. All captured states related
     * to this runnable are created at the time of creation, not at the time of
     * execution of the returned runnable.
     *
     * If the passed in runnable is already decorated, or there are no registered
     * state captors, the delegate runnable is returned unmodified.
     *
     * @param delegate runnable that ultimately runs
     * @return a runnable that calls registered state captors and propagates
     *  the captured state
     */
    public static Runnable capturingDecorator(Runnable delegate) {
        if (stateCaptors.isEmpty() || delegate instanceof CapturingRunnable) {
            return delegate;
        }

        final List<CapturedState> thingsToPropagate = new ArrayList<CapturedState>(stateCaptors.size());
        for (final StateCaptor<?> factory : stateCaptors) {
            thingsToPropagate.add(factory.get());
        }

        return new CapturingRunnable(delegate, thingsToPropagate);
    }

    /**
     * Creates a callable that allows capturing state. All captured states related
     * to this callable are created at the time of creation, not at the time of
     * execution of the returned runnable.
     *
     * If the passed in callable is already decorated, or there are no registered
     * state captors, the delegate callable is returned unmodified.
     *
     * @param delegate callable that ultimately runs
     * @return a callable that calls registered state captors and propagates
     *  the captured state
     */
    public static <V> Callable<V> capturingDecorator(Callable<V> delegate) {
        if (stateCaptors.isEmpty() || delegate instanceof CapturingRunnable) {
            return delegate;
        }

        final List<CapturedState> thingsToPropagate = new ArrayList<CapturedState>(stateCaptors.size());
        for (final StateCaptor<?> factory : stateCaptors) {
            thingsToPropagate.add(factory.get());
        }
        return new CapturingCallable<V>(delegate, thingsToPropagate);
    }

    private static <T> Collection<? extends Callable<T>> ofCallables(Collection<? extends Callable<T>> tasks) {
        if (stateCaptors.isEmpty() || tasks.isEmpty()) {
            return tasks;
        }

        List<Callable<T>> wrappedCallables = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> callable : tasks) {
            wrappedCallables.add(capturingDecorator(callable));
        }

        return wrappedCallables;
    }

    private static class CapturingRunnable implements Runnable {

        private final Runnable innerRunnable;
        /**
         * "If a thread only reads references to an object that were written after
         * the last freeze of its final fields, that thread is always guaranteed to
         * see the frozen value of the object's final fields. Such references are
         * called correctly published, because they are published after the object
         * is initialized. There may be objects that are reachable by following a
         * chain of references from such a final field. Reads of those objects will
         * be up to date as of the the freeze of the final field."
         * - Final Field Semantics, Jeremy Manson & William Pugh, April 7, 2003
         */
        private final List<CapturedState> capturedStates;

        CapturingRunnable(Runnable innerRunnable, List<CapturedState> capturedStates) {
            this.innerRunnable = innerRunnable;
            this.capturedStates = capturedStates;
        }

        @Override
        public void run() {
            for (CapturedState capturedState : capturedStates) {
                capturedState.beforeThreadExecution();
            }
            try {
                innerRunnable.run();
            } finally {
                for (CapturedState capturedState : capturedStates) {
                    capturedState.afterThreadExecution();
                }
            }
        }
    }

    private static class CapturingCallable<V> implements Callable<V> {

        private final Callable<V> innerCallable;
        /**
         * "If a thread only reads references to an object that were written after
         * the last freeze of its final fields, that thread is always guaranteed to
         * see the frozen value of the object's final fields. Such references are
         * called correctly published, because they are published after the object
         * is initialized. There may be objects that are reachable by following a
         * chain of references from such a final field. Reads of those objects will
         * be up to date as of the the freeze of the final field."
         * - Final Field Semantics, Jeremy Manson & William Pugh, April 7, 2003
         */
        private final List<CapturedState> capturedStates;

        CapturingCallable(Callable<V> innerCallable, List<CapturedState> capturedStates) {
            this.innerCallable = innerCallable;
            this.capturedStates = capturedStates;
        }

        @Override
        public V call() throws Exception {
            for (CapturedState capturedState : capturedStates) {
                capturedState.beforeThreadExecution();
            }
            try {
                return innerCallable.call();
            } finally {
                for (CapturedState capturedState : capturedStates) {
                    capturedState.afterThreadExecution();
                }
            }
        }
    }

    private static class CapturingExecutor implements Executor {

        private final Executor delegate;

        private CapturingExecutor(final Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(capturingDecorator(command));
        }
    }

    private static class CapturingExecutorService extends CapturingExecutor implements ExecutorService {

        private final ExecutorService delegate;

        private CapturingExecutorService(final ExecutorService delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(capturingDecorator(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(capturingDecorator(task), result);

        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(capturingDecorator(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(ofCallables(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
            return delegate.invokeAll(ofCallables(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(ofCallables(tasks));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(ofCallables(tasks), timeout, unit);
        }
    }

    private static class CapturingScheduledExecutorService extends CapturingExecutorService implements ScheduledExecutorService {

        private ScheduledExecutorService delegate;

        public CapturingScheduledExecutorService(ScheduledExecutorService delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return delegate.schedule(capturingDecorator(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return delegate.schedule(capturingDecorator(callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return delegate.scheduleAtFixedRate(capturingDecorator(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return delegate.scheduleWithFixedDelay(capturingDecorator(command), initialDelay, delay, unit);
        }
    }

    private static class CapturingCompletionService<T> implements CompletionService<T> {

        private final CompletionService<T> delegate;

        public CapturingCompletionService(CompletionService<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Future<T> submit(Callable<T> task) {
            return delegate.submit(capturingDecorator(task));
        }

        @Override
        public Future<T> submit(Runnable task, T result) {
            return delegate.submit(capturingDecorator(task), result);
        }

        @Override
        public Future<T> take() throws InterruptedException {
            return delegate.take();
        }

        @Override
        public Future<T> poll() {
            return delegate.poll();
        }

        @Override
        public Future<T> poll(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.poll(timeout, unit);
        }
    }
}
