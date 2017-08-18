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

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A holder for contextual information that can be retrieved based on the currently executing Thread.
 *
 * This is intended for framework developers to provide application code with access to contextual
 * information without needing to pass framework objects through all APIs; this is often needed
 * when calling third party libraries that may be used with multiple frameworks.
 *
 * This is similar to a ThreadLocal but has been integrated with the concurrent interfaces (such as
 * Executor) to allow this contextual information to be propagated across Threads used by parallel
 * frameworks.
 *
 * A ThreadContext is immutable and is safe for reuse across worker threads. Frameworks should
 * ensure the values passed are also safe for concurrent access (for example, by being immutable).
 */
public final class ThreadContext {

    private static final ThreadContext ROOT = new ThreadContext(TypedMap.empty());

    private static final ThreadLocal<ThreadContext> CONTEXT = ThreadLocal.withInitial(() -> ROOT);

    /**
     * Returns the ThreadContext associated with the currently executing thread.
     * If no context has been set then the {@link #emptyContext() empty context} will be returned.
     * @return the current ThreadContext; will not be null
     */
    public static ThreadContext current() {
        return CONTEXT.get();
    }

    /**
     * Returns a ThreadContext with no key values set.
     * This can be used to create new contexts.
     * @return an empty ThreadContext
     */
    public static ThreadContext emptyContext() {
        return ROOT;
    }

    private final TypedMap values;

    private ThreadContext(TypedMap values) {
        this.values = values;
    }

    /**
     * Returns a new ThreadContext containing all key/values from this context
     * and with the new key/value pair supplied.
     * @param key the key to add
     * @param value the value for that key
     * @param <V> the type of value
     * @return a new ThreadContext with the additional key
     */
    public <V> ThreadContext with(Key<V> key, V value) {
        TypedMap map = ImmutableTypedMap.Builder
                                        .from(this.values)
                                        .add(key.key, value)
                                        .build();
        return new ThreadContext(map);
    }

    /**
     * Returns a new ThreadContext containing all key/values from this context
     * combined with the new key/value pairs supplied.
     * @param newValues the key/value pairs to add
     * @return a new ThreadContext with the additional key/values
     */
    public ThreadContext with(Map<Key, Object> newValues) {
        ImmutableTypedMap.Builder b = ImmutableTypedMap.Builder.from(this.values);
        newValues.forEach((k, v) -> {
            b.add(k.key, v);
        });
        return new ThreadContext(b.build());
    }

    /**
     * Returns the value for a key in this context, or null if no value is set.
     * @param key the key to look up
     * @param <V> the type of value
     * @return the key's value
     */
    public <V> V get(Key<V> key) {
        return values.get(key.key);
    }

    /**
     * Wraps a Runnable task to ensure this context will be associated with the Thread
     * when the task is executed.
     * @param runnable the task to wrap
     * @return the wrapped task
     */
    public Runnable wrap(Runnable runnable) {
        return () -> {
            try (CloseableContext ignored = open()) {
                runnable.run();
            }
        };
    }

    /**
     * Wraps a Callable task to ensure this context will be associated with the Thread
     * when the task is executed.
     * @param callable the task to wrap
     * @param <T> the callable result type
     * @return the wrapped task
     */
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            try (CloseableContext ignored = open()) {
                return callable.call();
            }
        };
    }

    /**
     * Wraps a Supplier to ensure this context will be associated with the Thread
     * when it is executed.
     * @param supplier the supplier to wrap
     * @param <T> the typeof object the supplier supplies
     * @return the wrapped supplier
     */
    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> {
            try (CloseableContext ignored = open()) {
                return supplier.get();
            }
        };
    }

    /**
     * Return an AutoClosable that associates this context with the Thread until close() is
     * called. This can be used with the try-with-resources construct to ensure the current
     * context is restored after the block completes. For example:
     * <pre>
     *     // some other context is associated with the thread
     *     try (CloseableContext ignored = context.open()) {
     *         // this context is now associated with the thread
     *     }
     *     // the previous context is restored
     * </pre>
     *
     * @return the AutoCloseable
     */
    public CloseableContext open() {
        ThreadContext prior = CONTEXT.get();
        CONTEXT.set(this);
        return prior == ROOT ? CONTEXT::remove : () -> CONTEXT.set(prior);
    }

    /**
     * An AutoCloseable wrapper that restores the context captured during open().
     */
    public interface CloseableContext extends AutoCloseable {
        // Override to indicate no Exception will be thrown.
        @Override
        void close();
    }

    /**
     * A type-safe Key that identifies a Thread-local contextual value.
     *
     * @param type the type of value for this key
     * @param <T> the type of value for this key
     * @return the key
     */
    public static <T> Key<T> key(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return new Key(TypedMap.key(null, type));
    }

    /**
     * A Key for identifying Thread-local contextual values.
     * @param <T> the type of value for this key.
     */
    public static final class Key<T> {
        private final TypedMap.Key<T> key;

        private Key(TypedMap.Key<T> key) {
            this.key = key;
        }

        /**
         * Returns the value of this key in the current context.
         * @return the value of this key in the current context
         */
        public T current() {
            return get(ThreadContext.current());
        }

        /**
         * Returns the value of this key in the given context.
         * @param context the context used to look up the key
         * @return the value of this key in the given context
         */
        public T get(ThreadContext context) {
            return context.get(this);
        }
    }
}
