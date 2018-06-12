package software.amazon.swage.metrics;

import software.amazon.swage.threadcontext.ThreadContext;

/**
 * Support for associating a MetricContext with the current Thread.
 */
public final class ThreadLocalMetrics {
    /**
     * ThreadContext key for storing current MetricContext.
     */
    public static final ThreadContext.Key<MetricContext> METRIC_CONTEXT = ThreadContext.key(MetricContext.class);

    /**
     * @return the MetricContext associated with the current Thread
     */
    public static MetricContext current() {
        return ThreadContext.current().getOrDefault(METRIC_CONTEXT, NullContext::empty);
    }

    // Suppress construction.
    private ThreadLocalMetrics() {
    }

}
