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

/**
 * {@code CapturedState}s represent the data that will be copied
 * from the main thread to an execution thread. After the CapturedState
 * has been copied into another thread, it is given the opportunity to
 * execute code before and after the task, typically to manipulate
 * thread state, especially thread locals.
 */
public interface CapturedState {
    /**
     * In the context of a wrapped executing thread,
     * this is executed before the inner Runnable or Callable.
     *
     * This method should execute quickly and not throw exceptions.
     * If this method throws an exception, {@link #afterThreadExecution()}
     * will be executed immediately, the execution of the wrapped thread
     * will be skipped, and the exception will then be the execution result.
     */
    public void beforeThreadExecution();

    /**
     * In the context of a wrapped executing thread,
     * this is executed after the inner Runnable or Callable.
     *
     * This method should execute quickly and not throw exceptions.
     * If this method throws an exception, any remaining CapturedStates
     * will have their <code>afterThreadExecution()</code> methods skipped
     * and the exception will be the execution result.
     *
     * If an <code>afterThreadExecution()</code> throws an exception after a
     * <code>beforeThreadExecution()</code> threw an exception, the behavior
     * is undefined.
     */
    public void afterThreadExecution();
}
