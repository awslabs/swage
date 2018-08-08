package software.amazon.swage.metrics;

import java.time.Instant;

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;

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

    /**
     * @param attributes the attributes of the measurement context
     */
    public NullContext(TypedMap attributes) {
        this.attributes = attributes;
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
    public MetricContext newChildContext(TypedMap attributes) {
        ImmutableTypedMap.Builder childAttributes = ImmutableTypedMap.Builder.from(this.attributes);
        attributes.stream().forEach(entry -> childAttributes.add(entry.getKey(), entry.getValue()));
        return new NullContext(attributes);
    }

    @Override
    public void close() {
    }
}
