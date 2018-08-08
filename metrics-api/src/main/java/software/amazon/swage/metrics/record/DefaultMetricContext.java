package software.amazon.swage.metrics.record;

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

import java.time.Instant;

final class DefaultMetricContext implements MetricContext {
    private final MetricRecorder.RecorderContext context;
    private final MetricRecorder recorder;

    public DefaultMetricContext(MetricRecorder recorder, TypedMap attributes) {
        this.recorder = recorder;
        this.context = recorder.newRecorderContext(attributes);
    }

    @Override
    public TypedMap attributes() {
        return context.attributes();
    }

    @Override
    public void record(Metric label, Number value, Unit unit, Instant time) {
        recorder.record(label, value, unit, time, context);
    }

    @Override
    public void count(Metric label, long delta) {
        recorder.count(label, delta, context);
    }

    @Override
    public void close() {
        recorder.close(context);
    }

    @Override
    public MetricContext newChildContext(TypedMap attributes) {
        ImmutableTypedMap.Builder childAttributes = ImmutableTypedMap.Builder.from(context.attributes());
        attributes.stream().forEach(entry -> childAttributes.add(entry.getKey(), entry.getValue()));
        return new DefaultMetricContext(recorder, childAttributes.build());
    }
}
