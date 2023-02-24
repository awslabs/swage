package software.amazon.swage.metrics;

import org.junit.jupiter.api.Test;
import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullContextTest {

    @Test
    void defaultContextHasNoAttributes() {
        assertTrue(NullContext.empty().attributes().isEmpty());
    }

    @Test
    void nullContextCanHaveAttributes() {
        TypedMap.Key<String> ID = TypedMap.key("ID", String.class);
        TypedMap attributes = ImmutableTypedMap.Builder.with(ID, "id").build();
        NullContext context = new NullContext(attributes);
        assertSame(attributes, context.attributes());
    }
}
