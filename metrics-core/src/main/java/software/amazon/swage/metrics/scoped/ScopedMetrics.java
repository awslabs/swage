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
package software.amazon.swage.metrics.scoped;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.record.MetricRecorder;
import software.amazon.swage.metrics.Unit;
import software.amazon.swage.threadcontext.ThreadContext;

import java.time.Instant;

/**
 * A global/singleton that provides automatic handling of metric context.
 * Leverages the ThreadContext library to carry the metric objects around as
 * needed.  This will only work properly if all using code adheres to the
 * ThreadContext wrappers - all scope changes (new threads, etc) must be done
 * via ThreadContext.
 *
 * <p>
 * To use, the ScopedMetrics must first be primed with the MetricRecorder that
 * will be used.  All subsequent usage will use this recorder.  The recorder is
 * a global singleton, calling init will affect all other uses of this class.
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
// Initialize
MetricRecorder<ExtendedContext> reporter = ...;
ScopedMetrics.init(metricRecorder);

// Sometime later, start a task/request/operation
try (ClosableContext ig = ScopedMetrics.open(contextData)) {
    ...
    // Record metric using MetricRecorder and ContextData for current task
    ScopedMetrics.record(StandardMetric.TIME, sometime);
    ...
}
 * }
 * </pre>
 *
 * TODO: move fewer-parameter versions to MetricRecorder?
 *
 * TODO: throw this away, make frameworks handle it?
 */
public class ScopedMetrics {
    // Purely static bag o methods
    private ScopedMetrics() {}

    private static final Object lock = new Object();
    private static MetricRecorder recorder;

    private static final ThreadContext.Key<MetricContext> CTX_KEY =
            ThreadContext.key(MetricContext.class);


    /**
     * Initialize the ScopedRecorder system with the global MetricRecorder.
     * @param metricRecorder MetricRecorder to use for recording metrics
     */
    public static void init(MetricRecorder metricRecorder) {
        if (metricRecorder == null) {
            throw new IllegalArgumentException("Unable to init with null MetricRecorder");
        }
        synchronized (lock) {
            recorder = metricRecorder;
        }
    }

    /**
     * Return an AutoClosable that associates the recorder and context with the
     * Thread until close() is called. This can be used with the try-with-resources
     * construct to ensure the current context is restored after the block completes.
     * For example:
     * <pre>
     *     // some other context is associated with the thread
     *     try (CloseableContext ignored = ScopedRecorder.open(context)) {
     *         // all ScopeReporter calls use the above context
     *     }
     *     // the previous context is restored
     * </pre>
     *
     * @param contextData ContextData to use in the new scope
     * @return the AutoCloseable
     */
    public static AutoCloseable open(final TypedMap contextData)
    {
        if (recorder == null) {
            throw new IllegalStateException("Unititialized, unable to open metrics scope");
        }
        return ThreadContext.current()
                            .with(CTX_KEY, recorder.context(contextData))
                            .open();
    }

    /**
     * Record the value of a specific metric, as sampled at a specific time.
     * Context is retrieved from the current ThreadContext.
     * Assumes the value was sampled at the same time as this call, lowers to
     *   sample(name, value, Instant.now())
     *
     * @param label the metric to capture
     * @param value the current value
     * @param unit  the unit of the value
     */
    public static final void record(final Metric label, final Number value, final Unit unit) {
        ThreadContext.current().get(CTX_KEY).record(label, value, unit, Instant.now());
    }

    /**
     * Record the value of a specific metric, as sampled at a specific time.
     * Context is retrieved from the current ThreadContext.
     *
     * @param label the metric to capture
     * @param value the current value
     * @param unit  the unit of the value
     * @param timestamp time the metric event occurred
     */
    public static final void record(final Metric label, final Number value, final Unit unit, final Instant timestamp) {
        ThreadContext.current().get(CTX_KEY).record(label, value, unit, timestamp);
    }

    /**
     * Record occurrence of a specific metric event.
     * Context is retrieved from the current ThreadContext.
     *
     * <p>This is equivalent to calling {@link #count(Metric, long) count(name, 1)}.
     * @param label the metric to capture
     */
    public static final void count(final Metric label) {
        ThreadContext.current().get(CTX_KEY).count(label, 1);
    }

    /**
     * Record the increase or decrease in a metric.
     * Context is retrieved from the current ThreadContext.
     *
     * @param label the metric to capture
     * @param delta the increase (or decrease) in the value
     */
    public static final void count(final Metric label, final long delta) {
        ThreadContext.current().get(CTX_KEY).count(label, delta);
    }

}
