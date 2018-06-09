import java.time.Instant;

import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

/**
 * A dummy recorder that simply swallows all measurements.
 */
public class NullRecorder extends MetricRecorder {
    @Override
    protected void record(Metric label, Number value, Unit unit, Instant time, MetricContext context) {
    }

    @Override
    protected void count(Metric label, long delta, MetricContext context) {
    }
}
