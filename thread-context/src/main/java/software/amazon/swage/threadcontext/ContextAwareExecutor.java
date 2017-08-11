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

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * An {@link Executor} that ensures the current ThreadContext is propagated to tasks when they run.
 */
public class ContextAwareExecutor implements Executor {
    private final Executor executor;

    /**
     * Constructor specifying Executor to delegate to.
     * @param executor the Executor to delegate to
     */
    public ContextAwareExecutor(Executor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        this.executor = executor;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(ThreadContext.current().wrap(command));
    }
}
