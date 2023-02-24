package software.amazon.swage.metrics.record;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.MetricContext;

class NullRecorderTest {

    @Test
    void contextReturnsAttributes() {
        TypedMap attributes = TypedMap.empty();
        NullRecorder recorder = new NullRecorder();
        MetricContext context = recorder.context(attributes);
        assertSame(attributes, context.attributes());
    }
}
