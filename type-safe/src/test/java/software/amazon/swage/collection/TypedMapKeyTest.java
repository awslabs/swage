package software.amazon.swage.collection;

import org.junit.Test;

import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TypedMapKeyTest {

    @Test
    public void correctlyImplementsHashCode() {
        final String keyName = UUID.randomUUID().toString();
        final TypedMap.Key<String> key = TypedMap.key(keyName, String.class);

        // Test 1: Hashcode is deterministic and reflexive
        assertEquals(key.hashCode(), key.hashCode());

        // Test 2: Hashcode is determined by name and class
        assertEquals(Objects.hash(key.name, key.valueType), key.hashCode());

        // Test 3: two objects with the same values have same hash
        final TypedMap.Key<String> same = TypedMap.key(keyName, String.class);
        assertEquals(key.hashCode(), same.hashCode());
        assertEquals(same.hashCode(), key.hashCode());

        // Test 4: two objects with different values do not have same hash
        final String another = UUID.randomUUID().toString();
        final TypedMap.Key<String> different = TypedMap.key(another, String.class);
        final TypedMap.Key<Object> differentValue = TypedMap.key(keyName, Object.class);
        assertNotEquals(different.hashCode(), key.hashCode());
        assertNotEquals(differentValue.hashCode(), key.hashCode());
    }

    @Test
    public void correctlyImplementsEquals() {
        final String keyName = UUID.randomUUID().toString();
        final TypedMap.Key<String> key = TypedMap.key(keyName, String.class);

        // Test 1: equals is deterministic and reflexive
        assertEquals(key, key);

        // Test2: is not equal to null or random Object
        assertNotEquals(null, key);
        assertNotEquals(new Object(), key);

        // Test 3: two objects with the same values are equals
        final TypedMap.Key<String> same = TypedMap.key(keyName, String.class);
        assertEquals(key, same);
        assertEquals(same, key);

        // Test 4: two objects with different values are not equal
        final String another = UUID.randomUUID().toString();
        final TypedMap.Key<String> different = TypedMap.key(another, String.class);
        final TypedMap.Key<Object> differentValue = TypedMap.key(keyName, Object.class);
        assertNotEquals(different, key);
        assertNotEquals(differentValue, key);
    }
}
