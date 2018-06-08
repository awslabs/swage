package software.amazon.swage.metrics.record;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

/**
 *
 */
public class MemoryRecorder extends MetricRecorder {
    // Helper to represent a metric event, for ease comparing input and output
    public static class Event {
        public final Metric metric;
        public final Number value;
        public final Unit unit;
        public final Instant timestamp;
        public final Context context;

        private Event(Metric metric, Number value, Unit unit, Instant timestamp, Context context) {
            this.metric = metric;
            this.value = value;
            this.unit = unit;
            this.timestamp = timestamp;
            this.context = context;
        }

        private Event(Metric name, long delta, Context context) {
            this.metric = name;
            this.value = Long.valueOf(delta);
            this.unit = Unit.NONE;
            this.timestamp = null;
            this.context = context;
        }
    }

    private final List<Event> output = new ArrayList<>();

    @Override
    protected void record(Metric label, Number value, Unit unit, Instant time, Context context) {
        output.add(new Event(label, value, unit, time, context));
    }

    @Override
    protected void count(Metric metric, long delta, Context context) {
        output.add(new Event(metric, delta, context));
    }

    public List<Event> output() {
        return output;
    }
}
