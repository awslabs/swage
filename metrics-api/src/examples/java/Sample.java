import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

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
            task(context);
        }
    }

    // Role 3: A task that emits metrics.
    private static void task(MetricContext context) {
        context.record(NANO_TIME, System.nanoTime(), Unit.NONE);
        context.count(CALL);
    }
}
