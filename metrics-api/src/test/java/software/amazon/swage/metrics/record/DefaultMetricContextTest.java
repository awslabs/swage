package software.amazon.swage.metrics.record;

import org.junit.Test;
import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.MetricContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class DefaultMetricContextTest {
    @Test
    public void childContextReturnsCombinedAttributes() {
        TypedMap.Key<String> grandParentKey = TypedMap.key("grandParent", String.class);
        TypedMap.Key<String> parentKey = TypedMap.key("parent", String.class);
        TypedMap.Key<String> childKey = TypedMap.key("child", String.class);
        String grandParentValue = "grandParent";
        String parentValue = "parent";
        String childValue = "child";

        TypedMap attributes = ImmutableTypedMap.Builder
                .with(grandParentKey, grandParentValue)
                .add(ContextData.ID, "1")
                .build();
        TypedMap parentAttributes = ImmutableTypedMap.Builder
                .with(parentKey, parentValue)
                .add(ContextData.ID, "2")
                .build();
        TypedMap childAttributes = ImmutableTypedMap.Builder
                .with(childKey, childValue)
                .add(ContextData.ID, "3")
                .build();

        MetricRecorder recorder = new NullRecorder();
        MetricContext grandParentContext = new DefaultMetricContext(recorder, attributes);
        MetricContext parentContext = grandParentContext.newChildContext(parentAttributes);
        MetricContext childContext = parentContext.newChildContext(childAttributes);

        TypedMap actualChildAttributes = childContext.attributes();
        assertEquals(4, actualChildAttributes.size());
        assertEquals(grandParentValue, actualChildAttributes.get(grandParentKey));
        assertEquals(parentValue, actualChildAttributes.get(parentKey));
        assertEquals(childValue, actualChildAttributes.get(childKey));
        assertEquals("3", actualChildAttributes.get(ContextData.ID));
    }
}
