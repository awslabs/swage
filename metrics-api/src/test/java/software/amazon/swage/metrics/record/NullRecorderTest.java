package software.amazon.swage.metrics.record;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.MetricContext;

public class NullRecorderTest {

    @Test
    public void contextReturnsAttributes() {
        TypedMap attributes = TypedMap.empty();
        NullRecorder recorder = new NullRecorder();
        MetricContext context = recorder.context(attributes);
        assertSame(attributes, context.attributes());
    }
}
