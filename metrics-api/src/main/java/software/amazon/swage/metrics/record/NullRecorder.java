package software.amazon.swage.metrics.record;

import java.time.Instant;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.Unit;

/**
 * A dummy recorder that simply swallows all measurements.
 */
public class NullRecorder extends MetricRecorder<MetricRecorder.RecorderContext> {
    @Override
    protected RecorderContext newRecorderContext(TypedMap attributes) {
        return new RecorderContext(attributes);
    }

    @Override
    protected void record(Metric label, Number value, Unit unit, Instant time, RecorderContext context) {
    }

    @Override
    protected void count(Metric label, long delta, RecorderContext context) {
    }
}
