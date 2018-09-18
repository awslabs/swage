package software.amazon.swage.metrics;

import java.time.Instant;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.record.MetricRecorder;

/**
 * A MetricContext implementation that acts as a sink for recorded events.
 */
public class NullContext implements MetricContext {

    private static final NullContext EMPTY = new NullContext(TypedMap.empty());

    /**
     * The default NullContext with an empty set of attributes.
     *
     * @return the empty NullContext
     */
    public static NullContext empty() {
        return EMPTY;
    }

    private final TypedMap attributes;
    private final MetricContext parent;

    /**
     * @param attributes the attributes of the measurement context
     */
    public NullContext(TypedMap attributes) {
        this(attributes, null);
    }

    NullContext(TypedMap attributes, MetricContext parent) {
        this.attributes = attributes;
        this.parent = parent;
    }

    @Override
    public TypedMap attributes() {
        return attributes;
    }

    @Override
    public void record(Metric label, Number value, Unit unit, Instant time) {
    }

    @Override
    public void count(Metric label, long delta) {
    }

    @Override
    public void close() {
    }

    @Override
    public MetricContext newChild(TypedMap attributes) {
        return new NullContext(attributes, this);
    }

    @Override
    public MetricContext parent() {
        return parent;
    }
}
