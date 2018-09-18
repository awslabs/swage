package software.amazon.swage.metrics;

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
public interface MetricContext extends AutoCloseable {
    /**
     * Returns the attributes and their values that identity the context in which measurements
     * are being taken.
     *
     * @return the Context attributes
     */
    TypedMap attributes();

    /**
     * Record the value of a specific metric in this context, as gauged at a specific time.
     *
     * @param label the metric being recorded
     * @param value gauged value, with units.
     * @param unit  type of the value, e.g. seconds, percent, etc.
     * @param time  when the value was sampled
     */
    void record(Metric label, Number value, Unit unit, Instant time);

    /**
     * Record a measurement at the current time.
     * <p/>
     * Equivalent to calling record(metric, value, unit, Instant.now())
     *
     * @param label the metric being recorded
     * @param value gauged value, with units.
     * @param unit  type of the value, e.g. seconds, percent, etc.
     */
    default void record(Metric label, Number value, Unit unit) {
        record(label, value, unit, Instant.now());
    }

    /**
     * Count the increase or decrease of a metric in the given context.
     * <p/>
     * Records a change in value of a metric in the scope of this context. This
     * is often used to record occurrences of an event, such as the number of times
     * a method is called, an error occurred, or the amount of data sent.
     * <p/>
     * Changes to the count are timestamped with the time of context closure,
     * since only the total value of all counts for a metric at the end of a context
     * have any meaning. If the individual change needs to be tracked, it should be
     * recorded as a gauged event.
     *
     * @param label the metric being recorded
     * @param delta the change in the value
     */
    void count(Metric label, long delta);

    /**
     * Count a single occurrence of a metric.
     * <p/>
     * Equivalent to calling count(label, 1).
     *
     * @param label the metric that occurred
     */
    default void count(Metric label) {
        count(label, 1L);
    }

    /**
     * Close this context, indicating that no more measurements will be taken.
     */
    @Override
    void close();
}

