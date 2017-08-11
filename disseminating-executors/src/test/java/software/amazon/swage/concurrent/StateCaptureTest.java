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

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.Test;
import org.mockito.InOrder;

/**
 * Verifies that captures work as expected
 */
public class StateCaptureTest {

    @Test
    public void testExecutorCaptures() throws InterruptedException {
        // Setup
        ExecutorService e = Executors.newCachedThreadPool();
        Executor f = StateCapture.capturingDecorator(e);

        CapturedState mockCapturedState = mock(CapturedState.class);
        Runnable mockRunnable = mock(Runnable.class);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);
        f.execute(mockRunnable);
        e.shutdown();
        e.awaitTermination(10, TimeUnit.HOURS);

        verifyStandardCaptures(mockCapturedState, mockRunnable);
    }

    @Test
    public void testScheduledExecutorServiceCaptures() throws InterruptedException {
        // Setup
        ScheduledExecutorService e = Executors.newScheduledThreadPool(10);
        ScheduledExecutorService f = StateCapture.capturingDecorator(e);

        CapturedState mockCapturedState = mock(CapturedState.class);
        Runnable mockRunnable = mock(Runnable.class);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);
        f.execute(mockRunnable);
        e.shutdown();
        e.awaitTermination(10, TimeUnit.HOURS);

        verifyStandardCaptures(mockCapturedState, mockRunnable);
    }

    @Test
    public void testCompletionServiceRunnableCaptures() throws InterruptedException, Exception {
        // Setup
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletionService<Object> delegate = new ExecutorCompletionService<>(executor);
        CompletionService<Object> cs = StateCapture.capturingDecorator(delegate);

        CapturedState mockCapturedState = mock(CapturedState.class);
        Runnable mockRunnable = mock(Runnable.class);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);
        Object result = new Object();
        Future<Object> futureResult = cs.submit(mockRunnable, result);
        assertThat("Expected the delegate response to return",
            result, sameInstance(futureResult.get()));
        executor.shutdown();

        verifyStandardCaptures(mockCapturedState, mockRunnable);
    }

    @Test
    public void testCompletionServiceCallableCaptures() throws InterruptedException, Exception {
        // Setup
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletionService<Object> delegate = new ExecutorCompletionService<>(executor);
        CompletionService<Object> cs = StateCapture.capturingDecorator(delegate);

        CapturedState mockCapturedState = mock(CapturedState.class);
        Object expectedResult = new Object();
        @SuppressWarnings("unchecked")
        Callable<Object> mockCallable = mock(Callable.class);
        when(mockCallable.call()).thenReturn(expectedResult);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);
        Future<Object> futureResult = cs.submit(mockCallable);
        executor.shutdown();

        verifyStandardCaptures(mockCapturedState, mockCallable, expectedResult, futureResult.get());
    }

    @Test
    public void testExecutorServiceInvokeAllCaptures() throws Exception {
        testExecutorServiceCallables((f, mockCallable) -> {
            List<Future<Object>> results;
            try {
                results = f.invokeAll(Collections.singleton(mockCallable));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return results.get(0);
        });
    }

    @Test
    public void testExecutorServiceInvokeAllTimeoutCaptures() throws Exception {
        testExecutorServiceCallables((f, mockCallable) -> {
            final List<Future<Object>> results;
            try {
                results = f.invokeAll(Collections.singleton(mockCallable), 1, TimeUnit.HOURS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return results.get(0);
        });
    }

    @Test
    public void testExecutorServiceInvokeAnyCaptures() throws Exception {
        testExecutorServiceCallables((f, mockCallable) -> {
            try {
                return CompletableFuture.completedFuture(
                    f.invokeAny(Collections.singleton(mockCallable)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testExecutorServiceInvokeAnyTimeoutCaptures() throws Exception {
        testExecutorServiceCallables((f, mockCallable) -> {
            try {
                return CompletableFuture.completedFuture(
                    f.invokeAny(Collections.singleton(mockCallable), 1, TimeUnit.HOURS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testExecutorServiceSubmitCallableCaptures() throws Exception {
        testExecutorServiceCallables((f, mockCallable) -> {
            try {
                return f.submit(mockCallable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecutorServiceSubmitRunnableCaptures() throws Exception {
        testExecutorServiceRunnables((f, mockCallable) -> {
            try {
                return (Future<Object>)f.submit(mockCallable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testExecutorServiceSubmitRunnableResultCaptures() throws Exception {
        testExecutorServiceRunnables((f, mockCallable) -> {
            try {
                Object result = new Object();
                return f.submit(mockCallable, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void testExecutorServiceRunnables(
        BiFunction<ExecutorService, Runnable, Future<Object>> testMethod) throws Exception {
        // Setup
        ExecutorService e = Executors.newCachedThreadPool();
        ExecutorService f = StateCapture.capturingDecorator(e);

        CapturedState mockCapturedState = mock(CapturedState.class);
        Runnable mockRunnable = mock(Runnable.class);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);

        // Execution
        testMethod.apply(f, mockRunnable);

        f.shutdown();
        f.awaitTermination(10, TimeUnit.HOURS);

        verifyStandardCaptures(mockCapturedState, mockRunnable);
    }

    private void testExecutorServiceCallables(
        BiFunction<ExecutorService, Callable<Object>, Future<Object>> testMethod) throws Exception {
        // Setup
        ExecutorService e = Executors.newCachedThreadPool();
        ExecutorService f = StateCapture.capturingDecorator(e);

        Object object = new Object();
        CapturedState mockCapturedState = mock(CapturedState.class);
        @SuppressWarnings("unchecked")
        Callable<Object> mockCallable = mock(Callable.class);
        when(mockCallable.call()).thenReturn(object);
        ThreadLocalStateCaptor.THREAD_LOCAL.set(mockCapturedState);

        // Execution
        Future<Object> result = testMethod.apply(f, mockCallable);

        f.shutdown();
        f.awaitTermination(10, TimeUnit.HOURS);

        verifyStandardCaptures(mockCapturedState, mockCallable, object, result.get());
    }

    private void verifyStandardCaptures(CapturedState mockCapturedState, Runnable mockRunnable) {
        InOrder inOrder = inOrder(mockCapturedState, mockRunnable);
        inOrder.verify(mockCapturedState).beforeThreadExecution();
        inOrder.verify(mockRunnable).run();
        inOrder.verify(mockCapturedState).afterThreadExecution();
        verifyNoMoreInteractions(mockCapturedState, mockRunnable);
    }

    private <T> void verifyStandardCaptures(CapturedState mockCapturedState,
            Callable<T> mockCallable, T expectedCallResult, T callResult) throws Exception {

        assertThat("Should get same object from wrapped callable that was returned by callable",
            expectedCallResult, sameInstance(callResult));

        InOrder inOrder = inOrder(mockCapturedState, mockCallable);
        inOrder.verify(mockCapturedState).beforeThreadExecution();
        inOrder.verify(mockCallable).call();
        inOrder.verify(mockCapturedState).afterThreadExecution();
        verifyNoMoreInteractions(mockCapturedState, mockCallable);
    }
}
