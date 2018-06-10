package software.amazon.swage.metrics;

import java.time.Instant;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.threadcontext.ThreadContext;

/**
 * Support for associating a MetricContext with the current Thread.
 */
public final class ThreadLocalMetrics {
    /**
     * ThreadContext key for storing current MetricContext.
     */
    public static final ThreadContext.Key<MetricContext> METRIC_CONTEXT = ThreadContext.key(MetricContext.class);

    private static final MetricContext NULL_CONTEXT = new MetricContext() {
        @Override
        public TypedMap attributes() {
            return TypedMap.empty();
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
    };

    /**
     * @return the MetricContext associated with the current Thread
     */
    public static MetricContext current() {
        MetricContext context = ThreadContext.current().get(METRIC_CONTEXT);
        if (context == null) {
            return NULL_CONTEXT;
        } else {
            return context;
        }
    }

    // Suppress construction.
    private ThreadLocalMetrics() {
    }
}
