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
package software.amazon.swage.metrics;

import java.io.Closeable;
import java.time.Instant;

import software.amazon.swage.collection.TypedMap;

/**
 * MetricRecorder is used to record the events that occur as an application is
 * executing.
 *
 * <p>We support two types of measure:
 * <ul>
 *     <li><b>Gauges</b> which record the actual value at a point in time.</li>
 *     <li><b>Counters</b> which record the change in a value.</li>
 * </ul>
 *
 * <p>The metrics captured are associated with the currently executing unit of
 * work (aka task) such as a service request, as captured by a context data object.
 * Recording a metric is done by sending a MetricRecorder instance the current
 * context data and the specific event to record.
 *
 * <p>The MetricRecorder provides a Context abstraction wrapping the context data
 * and a reference to the MetricRecorder for convenient usage - metric-recording
 * tasks need only reference a Context object, with the context data and
 * MetricRecorder being handled by the task defining layer.

 * <p>A recorder instance may also have 'global' context associated with it, such
 * as application name or running host.
 *
 * <p>This object is thread safe.  It can be shared across the lifetime of an
 * application for multiple different tasks/requests.
 *
 * <p>The methods here will not throw Exceptions related to their work.
 * Any problems recording metrics events will not impact the application itself,
 * and so will be ignored/swallowed. Implementations are free to log or take
 * other appropriate action.
 *
 * <p>In terms of the previous CoralMetrics API, the MetricRecorder carries the
 * functionality of both a MetricsFactory and a Reporter.  Where the old Reporter
 * would be a single-use object per metrics instance (eg per request) this
 * recorder can and should be shared - it has the same lifecycle as the old
 * MetricsFactory.  It also takes over the job of managing creation of new
 * metric contexts, as was previously done with newMetric() on MetricsFactory.
 * The Context object replaces the previous Metric object for carrying metric context.
 * Having a thread-safe MetricRecorder avoids the excessive objects and
 * unpleasant synchronization hoops of the old Reporters.  Using metrics in a
 * multi-threaded environment becomes much less error prone.
 *
 * Usage
 * <pre>
 * {@code
// During app initialization create or inject the appropriate recorder implementation
MetricRecorder recorder = new RecorderImpl();

// Sometime later, per task/request/operation, create an object that captures
// the context and will be provided to the task code
TypedMap contextData = DomainSpecificBuilder
                            .withId(taskId)
                            .somethingElse(somethingElse)
                            .build();
SomeContext metricCtx = recorder.context(contextData);

// Inside task/request/operation, record some metrics
metricCtx.record(StandardMetric.Time, sometime, Unit.MILLISECOND);
 * }
 * </pre>
 *
 * MetricRecorder implementations should provide record() and count() methods.
 * They must ensure that the thread-safety and non-exceptiony promises are kept.
 *
 * <p>There is an implicit assumption that task code will only ever emit to one
 * MetricRecorder instance.  If multiple recording destinations are required, a
 * composite MetricRecorder implementation may be used, fanning out metric events
 * as appropriate.
 *
 * TODO: JSR-305 @ThreadSafe
 * TODO: add explicit shutdown/lifecycle management?
 */
public abstract class MetricRecorder {

    /**
     * Context under which a metric is reported.
     *
     * A Context is a convenience wrapper around the context data object, where
     * the data object represents the actual unique identifier for a given task.
     * In general, applications recording metrics will be interacting with an
     * instance of this Context (or some functionality on top of this).
     *
     * Each context carries a reference to the recorder appropriate for recording
     * metrics.  This removes the need for users to carry around both a recorder
     * and the current context data, as well as giving a strong link between what
     * a recorder requires in a context and the context that provides it.
     * Convenience functions are provided to call methods on the recorder with
     * this context.
     *
     * In the previous world of CoralMetrics, this context was carried implicitly
     * with the Metrics object.  One query log entry would be one context.
     *
     * TODO: provide a childContext() mechanism?
     */
    public final class Context implements Closeable {
        private final TypedMap data;

        private Context(final TypedMap data) {
            this.data = data;
        }

        /**
         * Record the value of a specific metric, as gauged at a specific time
         * in the given context.
         * This is the workhorse method of metrics - if you have something to
         * record, chances are you want this.  Examples include request time,
         * cache hits (or misses), currently used memory, failure, bytes sent, etc.
         *
         * Uses the MetricRecorder implicit in this context.
         *
         * This method will fail silently, as application code is not expected
         * to depend on success or failure.
         *
         * @param label The metric being recorded.
         * @param value Gauged value, with units.
         * @param unit  Type of the value, e.g. seconds, percent, etc.
         * @param time  When the value was sampled.
         */
        public final void record(Metric label, Number value, Unit unit, Instant time) {
            MetricRecorder.this.record(label, value, unit, time, data);
        }

        /**
         * Convenience method to record a metric at the current time.
         * Equivalent to calling record(metric, value, unit, Instant.now(), data)
         * @param label The metric being recorded.
         * @param value Gauged value, with units.
         * @param unit  Type of the value, e.g. seconds, percent, etc.
         */
        public final void record(Metric label, Number value, Unit unit) {
            MetricRecorder.this.record(label, value, unit, Instant.now(), data);
        }

        /**
         * Count the increase or decrease of a metric in the given context.
         *
         * These are explicitly aggregated values, where only the total number of
         * occurrences in a context matter.
         * Examples include counting number of times a method is called, or keeping
         * track of the number of errors encountered, or the amount of data sent
         * while in the given context.
         *
         * If you need to distinguish between occurrences of an event, or care that
         * an event did not occur (for ratios of success for example) then you want
         * to record those individually and not use a count.
         * Changes to the count are not timestamped as only the total value of all
         * counts for a metric have any meaning - if the individual change needs to
         * be tracked, you probably want to record the change as a gauged event.
         *
         * Uses the MetricRecorder implicit in this context.
         *
         * This method will fail silently, as application code is not expected
         * to depend on success or failure.
         *
         * @param label The metric being recorded.
         * @param delta Change in the value to record.
         */
        public final void count(Metric label, long delta) {
            MetricRecorder.this.count(label, delta, data);
        }

        /**
         * Indicate that the context scope is finished, and will emit no more
         * metric events.
         *
         * Depending on the MetricRecorder implementation, this may or may not
         * be required to ensure metric events are emitted properly.
         * Best practice is to call close() once a task is completed.
         *
         * Once close() is called, any subsequent calls on this context will
         * result in undefined behavior.  MetricRecorder implementations may
         * throw an IllegalStateException in such a case, but have no obligation
         * to do so.
         */
        public final void close() {
            MetricRecorder.this.close(data);
        }
    }

    /**
     * Construct a new Context object for the given data.
     *
     * The returned Context is a light wrapper around the provided data object.
     * Context objects created with the same data object will be functionally
     * equivalent, and MetricRecorder implementations are free to return the
     * same object if called multiple times with the same data object.
     * The logical context scope is defined by object identity on the context
     * data object.  Two context data objects with the same values will be
     * treated as different contexts.
     *
     * @param data An object to be used for identifying the Context.
     * @return A new Context object wrapping the provided data object.
     */
    public Context context(TypedMap data) {
        return new Context(data);
    }


    /**
     * Record the value of a specific metric, as gauged at a specific time
     * in the given context.
     *
     * This method will fail silently, as application code is not expected
     * to depend on success or failure.
     *
     * @param label The metric being recorded.
     * @param value Gauged value, with units.
     * @param unit  Type of the value, e.g. seconds, percent, etc.
     * @param time  When the value was sampled.
     * @param context Identifying data about context the value belongs in.
     */
    protected abstract void record(
            Metric label,
            Number value,
            Unit unit,
            Instant time,
            TypedMap context);

    /**
     * Count the increase or decrease of a metric in the given context.
     *
     * These are explicitly aggregated values, where only the total number of
     * occurrences in a context matter.
     * Examples include counting number of times a method is called, or keeping
     * track of the number of errors encountered, or the amount of data sent
     * while in the given context.
     *
     * If you need to distinguish between occurrences of an event, or care that
     * an event did not occur (for ratios of success for example) then you want
     * to record those individually and not use a count.
     * Changes to the count are not timestamped as only the total value of all
     * counts for a metric have any meaning - if the individual change needs to
     * be tracked, you probably want to record the change as a gauged event.
     *
     * This method will fail silently, as application code is not expected
     * to depend on success or failure.
     *
     * @param label The metric to capture
     * @param delta The increase (or decrease) in the value
     * @param context Identifying data about context the value belongs in.
     */
    protected abstract void count(Metric label, long delta, TypedMap context);


    /**
     * Indicate that the task represented by the given context is finished, and
     * will emit no more metric events.
     *
     * Once close() is called for a given context, the MetricRecorder may clean
     * up, aggregate, and/or flush any data associated with that context.
     *
     * After close() is called for a given context, any subsequent calls to this
     * recorder referencing that context will result in undefined behavior.
     * MetricRecorder implementations may throw an IllegalStateException in such
     * a case, but have no obligation to do so.
     *
     * The logical context scope is defined by object identity on the context
     * data object.  Two context data objects with the same values will be
     * treated as different contexts.
     *
     * The default implementation of this method is a no-op.  MetricRecorder
     * implementations should implement this method as appropriate, but may
     * ignore if no buffering or aggregation is present.
     *
     * @param context Identifying data for the context being closed.
     */
    protected void close(TypedMap context) {
        return;
    }

}
