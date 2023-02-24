/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.swage.collection;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmutableTypedMapTest {

    @Test
    void get_typesafe() {
        final TypedMap.Key<String> ka = TypedMap.key("Key1", String.class);
        final TypedMap.Key<Integer> kb = TypedMap.key("Key2", Integer.class);

        final String va = "foobar";
        final Integer vb = Integer.valueOf(1337);

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .build();

        assertFalse(data.isEmpty(), "TypedMap instance is unexpectedly empty");
        assertEquals(2, data.size(), "TypedMap instance has wrong size");
        assertTrue(data.containsKey(ka), "TypedMap instance missing key");
        assertTrue(data.containsKey(kb), "TypedMap instance missing key");
        assertTrue(data.containsValue(va), "TypedMap instance missing value");
        assertTrue(data.containsValue(vb), "TypedMap instance missing value");
        assertEquals(va, data.get(ka), "TypedMap instance has wrong value for key");
        assertEquals(vb, data.get(kb), "TypedMap instance has wrong value for key");
    }

    @Test
    void no_entry() {
        final TypedMap.Key<String> ka = TypedMap.key("Key1", String.class);
        final TypedMap.Key<Integer> kb = TypedMap.key("Key2", Integer.class);

        final String va = "foobar";
        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .build();

        assertNull(data.get(kb), "Get on missing key failed to return null");
        assertEquals(Optional.empty(), data.getOptional(kb), "getOptional on missing key returned a value");

        assertFalse(data.containsKey(kb), "containsKey is true for unused key");
        assertFalse(data.containsValue("nope"), "containsValue is true for non-existing value");

        // check the rest of the map is there
        assertFalse(data.isEmpty(), "TypedMap instance is unexpectedly empty");
        assertEquals(1, data.size(), "TypedMap instance has wrong size");
        assertTrue(data.containsKey(ka), "TypedMap instance missing key");
        assertTrue(data.containsValue(va), "TypedMap instance missing value");
        assertEquals(va, data.get(ka), "TypedMap instance has wrong value for key");
        assertEquals(va, data.getOptional(ka).get(), "TypedMap instance has wrong value for key");
    }

    @Test
    void empty_constant() {
        final TypedMap.Key<Instant> key = TypedMap.key("Foo", Instant.class);

        TypedMap empty = TypedMap.EMPTY;

        assertTrue(empty.isEmpty(), "Empty map not empty");
        assertEquals(0, empty.size(), "Empty map has non-zero size");
        assertNull(empty.get(key), "Empty map has a value");
        assertFalse(empty.containsKey(key), "Empty map contains a key");
        assertFalse(empty.containsValue("something"), "Empty map contains a value");
        assertTrue(empty.keySet().isEmpty(), "Empty map has non-empty key set");

        Iterator<TypedMap.Entry> it = empty.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void keyset() throws Exception {
        final TypedMap.Key<Double> ka = TypedMap.key("Alpha", Double.class);
        final TypedMap.Key<String> kb = TypedMap.key("Beta", String.class);
        final TypedMap.Key<UUID> kc = TypedMap.key("Gamma", UUID.class);
        final TypedMap.Key<Object> kd = TypedMap.key("Delta", Object.class);

        final Double va = Double.valueOf(867.5309);
        final String vb = "Here is a string, with...\nmultiple lines and stuff.";
        final UUID vc = UUID.randomUUID();
        final Object vd = new Object();

        TypedMap data = ImmutableTypedMap.Builder
                .with(kc, vc)
                .add(ka, va)
                .add(kb, vb)
                .add(kd, vd)
                .build();

        Set<TypedMap.Key> s = data.keySet();
        assertEquals(4, s.size(), "Set of keys has wrong size");
        assertTrue(s.contains(ka), "Set of keys is missing a key");
        assertTrue(s.contains(kb), "Set of keys is missing a key");
        assertTrue(s.contains(kc), "Set of keys is missing a key");
        assertTrue(s.contains(kd), "Set of keys is missing a key");
    }

    @Test
    void typedKeyset() throws Exception {
        final TypedMap.Key<Double> ka = TypedMap.key("A", Double.class);
        final TypedMap.Key<String> kb = TypedMap.key("B", String.class);
        final TypedMap.Key<String> kc = TypedMap.key("C", String.class);
        final TypedMap.Key<Object> kd = TypedMap.key("D", Object.class);

        final Double va = Double.valueOf(3.14159);
        final String vb = "Mmm tasty pi";
        final String vc = "but no love for tau";
        final Object vd = "or a type safe string";

        TypedMap data = ImmutableTypedMap.Builder
                .with(kc, vc)
                .add(ka, va)
                .add(kb, vb)
                .add(kd, vd)
                .build();

        Set<TypedMap.Key<String>> s = data.typedKeySet(String.class);
        assertEquals(2, s.size(), "Typed set of keys has wrong size");
        assertFalse(s.contains(ka), "Typed set of keys has unexpected member");
        assertTrue(s.contains(kb), "Typed set of keys is missing a key");
        assertTrue(s.contains(kc), "Typed set of keys is missing a key");
        assertFalse(s.contains(kd), "Typed set of keys has unexpected member");
    }

    @Test
    void iterator() throws Exception {
        final TypedMap.Key<Long> ka = TypedMap.key("1", Long.class);
        final TypedMap.Key<Short> kb = TypedMap.key("2", Short.class);
        final TypedMap.Key<BigInteger> kc = TypedMap.key("5", BigInteger.class);

        final Long va = Long.valueOf(999999999);
        final Short vb = Short.valueOf((short)1);
        final BigInteger vc = BigInteger.valueOf(Long.MAX_VALUE+Short.MAX_VALUE);

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .add(kc, vc)
                .build();

        assertEquals(3, data.size());

        int i = 0;
        boolean seen_a = false;
        boolean seen_b = false;
        boolean seen_c = false;
        for (TypedMap.Entry e : data) {
            i++;
            if (e.getKey().equals(ka) && e.getValue().equals(va)) {
                seen_a = true;
            }
            if (e.getKey().equals(kb) && e.getValue().equals(vb)) {
                seen_b = true;
            }
            if (e.getKey().equals(kc) && e.getValue().equals(vc)) {
                seen_c = true;
            }
        }
        assertEquals(3, i, "Iterator iterated incorrect increments");
        assertTrue(seen_a, "Iterator missed an entry");
        assertTrue(seen_b, "Iterator missed an entry");
        assertTrue(seen_c, "Iterator missed an entry");
    }

    @Test
    void iterator_remove() throws Exception {
        final TypedMap.Key<Long> ka = TypedMap.key("1", Long.class);
        final Long va = Long.valueOf(999999999);

        TypedMap data = ImmutableTypedMap.Builder.with(ka, va).build();

        Iterator<TypedMap.Entry> it = data.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void forEach() {
        final TypedMap.Key<Long> ka = TypedMap.key("1", Long.class);
        final TypedMap.Key<Short> kb = TypedMap.key("2", Short.class);
        final TypedMap.Key<BigInteger> kc = TypedMap.key("5", BigInteger.class);

        final Long va = Long.valueOf(999999999);
        final Short vb = Short.valueOf((short)1);
        final BigInteger vc = BigInteger.valueOf(Long.MAX_VALUE+Short.MAX_VALUE);

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .add(kc, vc)
                .build();

        assertEquals(3, data.size());

        final boolean[] seen = {false,false,false,false};
        data.forEach((e) -> {
            if (e.getKey().equals(ka) && e.getValue().equals(va)) {
                seen[0] = true;
            }
            else if (e.getKey().equals(kb) && e.getValue().equals(vb)) {
                seen[1] = true;
            }
            else if (e.getKey().equals(kc) && e.getValue().equals(vc)) {
                seen[2] = true;
            }
            else {
                seen[3] = true;
            }
        });
        assertTrue(seen[0], "forEach missed an entry");
        assertTrue(seen[1], "forEach missed an entry");
        assertTrue(seen[2], "forEach missed an entry");
        assertFalse(seen[3], "forEach hit an unexpected entry");
    }

    @Test
    void typedIterator() {
        final TypedMap.Key<String> ka = TypedMap.key("y", String.class);
        final TypedMap.Key<Object> kb = TypedMap.key("n", Object.class);
        final TypedMap.Key<String> kc = TypedMap.key("m", String.class);

        final String va = "yes";
        final String vb = "no";
        final String vc = "maybe";

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .add(kc, vc)
                .build();

        assertEquals(3, data.size());

        int i = 0;
        boolean seen_a = false;
        boolean seen_b = false;
        boolean seen_c = false;

        Iterator<TypedMap.Entry<String>> it = data.typedIterator(String.class);
        while (it.hasNext()) {
            i++;
            final TypedMap.Entry<String> e = it.next();
            TypedMap.Key<String> k = e.getKey();
            String v = e.getValue();
            if (k.equals(ka) && v.equals(va)) {
                seen_a = true;
            }
            if (k.equals(kb) && v.equals(vb)) {
                seen_b = true;
            }
            if (k.equals(kc) && v.equals(vc)) {
                seen_c = true;
            }
        }
        assertEquals(2, i, "Iterator iterated incorrect increments");
        assertTrue(seen_a, "Typed iterator missed an entry");
        assertFalse(seen_b, "Typed iterator returned an entry of wrong type");
        assertTrue(seen_c, "Typed iterator missed an entry");
    }

    @Test
    void typedIterator_remove() {
        final TypedMap.Key<String> ka = TypedMap.key("y", String.class);
        final String va = "yes";

        TypedMap data = ImmutableTypedMap.Builder.with(ka, va).build();

        Iterator<TypedMap.Entry<String>> it = data.typedIterator(String.class);
        assertThrows(UnsupportedOperationException.class, it::remove);
    }


    @Test
    void forEachTyped() {
        final TypedMap.Key<String> ka = TypedMap.key("y", String.class);
        final TypedMap.Key<Object> kb = TypedMap.key("n", Object.class);
        final TypedMap.Key<String> kc = TypedMap.key("m", String.class);
        final TypedMap.Key<Integer> kd = TypedMap.key("s", Integer.class);

        final String va = "yes";
        final String vb = "no";
        final String vc = "maybe";
        final Integer vd = 50;

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .add(kc, vc)
                .add(kd, vd)
                .build();

        assertEquals(4, data.size());

        final boolean[] seen = {false,false,false,false,false};
        data.forEachTyped(String.class, (e) -> {
            TypedMap.Key<String> k = e.getKey();
            String v = e.getValue();
            if (k.equals(ka) && v.equals(va)) {
                seen[0] = true;
            }
            else if (k.equals(kb) && v.equals(vb)) {
                seen[1] = true;
            }
            else if (k.equals(kc) && v.equals(vc)) {
                seen[2] = true;
            }
            else if (k.equals(kd) && v.equals(vd)) {
                seen[3] = true;
            }
            else {
                seen[4] = true;
            }
        });

        assertTrue(seen[0], "forEachTyped missed an entry");
        assertFalse(seen[1], "forEachTyped hit an unexpected entry");
        assertTrue(seen[2], "forEachTyped missed an entry");
        assertFalse(seen[3], "forEachTyped hit an unexpected entry");
        assertFalse(seen[4], "forEachTyped hit an unexpected entry");
    }

    @Test
    void testMultipleBuildsDontMutateInstances() {
        final TypedMap.Key<String> strKey = TypedMap.key("TestId1", String.class);
        final String foo = "FOO";
        final String bar = "BAR";

        ImmutableTypedMap.Builder builder = ImmutableTypedMap.Builder.with(strKey, foo);
        TypedMap firstInstance = builder.build();
        String originalValue = firstInstance.get(strKey);

        assertEquals(foo, originalValue);

        // Override existing key using the same builder
        TypedMap secondInstance = builder.add(strKey, bar).build();

        // first instance should not be mutated
        assertEquals(originalValue, firstInstance.get(strKey));
        assertEquals(bar, secondInstance.get(strKey));
    }
}
