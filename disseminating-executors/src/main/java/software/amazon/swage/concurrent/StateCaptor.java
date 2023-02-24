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
 * A {@code StateCaptor} is responsible for producing {@link CapturedState}s.
 *
 * StateCaptor's get() method is called in the context of a calling thread.
 * The returned {@link CapturedState} should capture any state or functionality
 * that should be used in the context of the called thread.
 *
 * StateCaptor implementations must have public zero-arg constructors.
 *
 * StateCaptors need to be registered with the Java {@link java.util.ServiceLoader}
 * in order to take effect at runtime. This is easily achieved using Google's
 * <a href="https://github.com/google/auto/tree/master/service">Auto Service</a>.
 * <br><br>
 * <h2>Thread safety</h2>
 * StateCaptor implementations must be thread safe.
 *
 */
public interface StateCaptor<T extends CapturedState> {

    /**
     * This method is called in the context of the calling thread, allowing a
     * {@code CapturedState} to be returned that has retained any state needed
     * for use in the context of the executing thread.
     * @return a CapturedState which will be called in the context of an
     * executing thread immediately before and after its execution.
     */
    T get();
}
