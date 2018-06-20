import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.ThreadLocalMetrics;
import software.amazon.swage.metrics.Unit;
import software.amazon.swage.metrics.record.MetricRecorder;
import software.amazon.swage.metrics.record.NullRecorder;
import software.amazon.swage.threadcontext.ThreadContext;

/**
 * Sample code that illustrates the different actors in the system.
 */
public class Sample {
    private static final Metric NANO_TIME = Metric.define("Time");
    private static final Metric CALL = Metric.define("Call");

    public static void main(String[] args) {
        // Role 1: Set up the metric recording system.
        MetricRecorder recorder = new NullRecorder();

        // Role 2: Set up the context in which measurements will be taken.
        TypedMap metadata = ContextData.withId("id").build();
        try (MetricContext context = recorder.context(metadata)) {

            // Call a task that is aware of the MetricContext
            contextAware(context);

            // Options for calling code that is not aware of the metrics framework
            // First we add the MetricContext to the ThreadContext that will be used when
            // application code is called. Typically any other Thread-associated keys would
            // be added at the same time.
            ThreadContext threadContext = ThreadContext.emptyContext()
                    .with(ThreadLocalMetrics.METRIC_CONTEXT, context);

            // Associating the MetricContext aroud a block of code.
            try (ThreadContext.CloseableContext ig = threadContext.open()) {
                applicationCode();
            }

            // Or just using a wrapper if we're simply invoking a method.
            threadContext.wrap(Sample::applicationCode).run();
        }
    }

    // Role 3: A task that emits metrics but is passed the MetricContext to use.
    private static void contextAware(MetricContext context) {
        context.record(NANO_TIME, System.nanoTime(), Unit.NONE);
        context.count(CALL);

    }
    // Role 3: A task that emits metrics, getting the context from the Thread.
    // The application code is not aware of metrics being collected.
    private static void applicationCode() {
        libraryCode();
    }

    // But the library it uses emits metrics.
    private static void libraryCode() {
        MetricContext context = ThreadLocalMetrics.current();
        context.record(NANO_TIME, System.nanoTime(), Unit.NONE);
        context.count(CALL);
    }
}
