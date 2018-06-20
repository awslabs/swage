package software.amazon.swage.metrics;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;

public class NullContextTest {

    @Test
    public void defaultContextHasNoAttributes() {
        assertTrue(NullContext.empty().attributes().isEmpty());
    }

    @Test
    public void nullContextCanHaveAttributes() {
        TypedMap.Key<String> ID = TypedMap.key("ID", String.class);
        TypedMap attributes = ImmutableTypedMap.Builder.with(ID, "id").build();
        NullContext context = new NullContext(attributes);
        assertSame(attributes, context.attributes());
    }
}
