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

import com.google.auto.service.AutoService;

/**
 * For testing purposes, this factory retrieves a pre-generated captured state
 * from a thread local, which can be set externally, and returns it.
 * This allows us to verify that the captured state actually gets
 * across the thread boundary and is executed in the expected order.
 *
 * A real implementation of a thread-local-based captor
 * would likely retrieve a value from a thread local, then put that value
 * into a newly generated captured state.
 *
 */
@AutoService(StateCaptor.class)
public class ThreadLocalStateCaptor
        implements StateCaptor<CapturedState> {

    public static ThreadLocal<CapturedState> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public CapturedState get() {
        return THREAD_LOCAL.get();
    }

    @Override
    public String toString() {
        return "TestThreadStateCapture";
    }
}
