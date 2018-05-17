package software.amazon.swage.metrics;

import software.amazon.swage.collection.ImmutableTypedMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder for data carried with the context under which a metric is reported.
 *
 * <p>
 * This builder allows for regular state-based equality semantics.
 *
 * <p>
 * Built instances are immutable.  Metric recording will use
 * the data across threads as needed, and relies on the data being
 * unchanged for a particular Context.
 *
 * <p>
 */
public class DynamicContextData extends ImmutableTypedMap.Builder {
    public DynamicContextData() {
        super(new ConcurrentHashMap<>());
    }
}
