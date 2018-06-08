package software.amazon.swage.metrics;

import java.io.Closeable;
import java.time.Instant;

import software.amazon.swage.collection.TypedMap;

/**
 * The context in which a measurement is taken. A {MetricContext} contains the parameters of
 * the environment in which a measurement is taken.
 * In the physical world, these might define the effective time and place;
 * in a computer system, they might define the host, request, function, task or
 * other application-specific parameters.
 * <p/>
 * A {MetricContext} is the primary interface used to record measurements.
 */
// TODO: provide a childContext() mechanism?
public final class MetricContext implements Closeable {
    private final MetricRecorder recorder;
    private final TypedMap dimensions;

    /**
     * Create a new context for taking measurements.
     *
     * @param recorder   where measurments will be recorded
     * @param dimensions the properties of this context
     */
    public MetricContext(MetricRecorder recorder, TypedMap dimensions) {
        this.recorder = recorder;
        this.dimensions = dimensions;
    }

    /**
     * Returns dimensions to associate with
     *
     * @return the Context dimensions
     */
    public TypedMap dimensions() {
        return dimensions;
    }

    /**
     * Record the value of a specific metric in this context, as gauged at a specific time.
     *
     * @param label the metric being recorded
     * @param value gauged value, with units.
     * @param unit  type of the value, e.g. seconds, percent, etc.
     * @param time  when the value was sampled
     */
    public void record(Metric label, Number value, Unit unit, Instant time) {
        recorder.record(label, value, unit, time, this);
    }

    /**
     * Record a measurement at the current time.
     * <p/>
     * Equivalent to calling record(metric, value, unit, Instant.now())
     *
     * @param label the metric being recorded
     * @param value gauged value, with units.
     * @param unit  type of the value, e.g. seconds, percent, etc.
     */
    public void record(Metric label, Number value, Unit unit) {
        record(label, value, unit, Instant.now());
    }

    /**
     * Count the increase or decrease of a metric in the given context.
     * <p/>
     * Records a change in value of a metric in the scope of this context. This
     * is often used to record occurrences of an event, such as the number of times
     * a method is called, an error occurred, or the amount of data sent.
     * <p/>
     * Changes to the count are not timestamped as only the total value of all
     * counts for a metric have any meaning. If the individual change needs to
     * be tracked, it should be recorded as a gauged event.
     *
     * @param label the metric being recorded
     * @param delta the change in the value
     */
    public void count(Metric label, long delta) {
        recorder.count(label, delta, this);
    }

    /**
     * Count a single occurrence of a metric.
     * <p/>
     * Equivalent to calling count(label, 1).
     *
     * @param label the metric that occurred
     */
    public void count(Metric label) {
        count(label, 1L);
    }

    /**
     * Close this context, indicating that no more measurements will be taken.
     */
    public void close() {
        recorder.close(this);
    }
}
