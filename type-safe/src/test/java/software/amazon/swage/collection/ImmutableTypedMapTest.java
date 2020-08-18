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

import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImmutableTypedMapTest {

    @Test
    public void get_typesafe() throws Exception {
        final TypedMap.Key<String> ka = TypedMap.key("Key1", String.class);
        final TypedMap.Key<Integer> kb = TypedMap.key("Key2", Integer.class);

        final String va = "foobar";
        final Integer vb = Integer.valueOf(1337);

        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .add(kb, vb)
                .build();

        assertFalse("TypedMap instance is unexpectedly empty", data.isEmpty());
        assertEquals("TypedMap instance has wrong size", 2, data.size());
        assertTrue("TypedMap instance missing key", data.containsKey(ka));
        assertTrue("TypedMap instance missing key", data.containsKey(kb));
        assertTrue("TypedMap instance missing value", data.containsValue(va));
        assertTrue("TypedMap instance missing value", data.containsValue(vb));
        assertEquals("TypedMap instance has wrong value for key", va, data.get(ka));
        assertEquals("TypedMap instance has wrong value for key", vb, data.get(kb));
    }

    @Test
    public void no_entry() throws Exception {
        final TypedMap.Key<String> ka = TypedMap.key("Key1", String.class);
        final TypedMap.Key<Integer> kb = TypedMap.key("Key2", Integer.class);

        final String va = "foobar";
        TypedMap data = ImmutableTypedMap.Builder
                .with(ka, va)
                .build();

        assertNull("Get on missing key failed to return null", data.get(kb));
        assertEquals("getOptional on missing key returned a value", Optional.empty(), data.getOptional(kb));

        assertFalse("containsKey is true for unused key", data.containsKey(kb));
        assertFalse("containsValue is true for non-existing value", data.containsValue("nope"));

        // check the rest of the map is there
        assertFalse("TypedMap instance is unexpectedly empty", data.isEmpty());
        assertEquals("TypedMap instance has wrong size", 1, data.size());
        assertTrue("TypedMap instance missing key", data.containsKey(ka));
        assertTrue("TypedMap instance missing value", data.containsValue(va));
        assertEquals("TypedMap instance has wrong value for key", va, data.get(ka));
        assertEquals("TypedMap instance has wrong value for key", va, data.getOptional(ka).get());
    }

    @Test
    public void empty_constant() throws Exception {
        final TypedMap.Key<Instant> key = TypedMap.key("Foo", Instant.class);

        TypedMap empty = TypedMap.EMPTY;

        assertTrue("Empty map not empty", empty.isEmpty());
        assertEquals("Empty map has non-zero size", 0, empty.size());
        assertNull("Empty map has a value", empty.get(key));
        assertFalse("Empty map contains a key", empty.containsKey(key));
        assertFalse("Empty map contains a value", empty.containsValue("something"));
        assertTrue("Empty map has non-empty key set", empty.keySet().isEmpty());

        Iterator<TypedMap.Entry> it = empty.iterator();
        assertFalse(it.hasNext());
        try {
            it.next();
        } catch (NoSuchElementException e) {
            //expected
            return;
        }
        fail("Expected NoSuchElementException not thrown iterating on empty map");
    }

    @Test
    public void keyset() throws Exception {
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
        assertEquals("Set of keys has wrong size", 4, s.size());
        assertTrue("Set of keys is missing a key", s.contains(ka));
        assertTrue("Set of keys is missing a key", s.contains(kb));
        assertTrue("Set of keys is missing a key", s.contains(kc));
        assertTrue("Set of keys is missing a key", s.contains(kd));
    }

    @Test
    public void typedKeyset() throws Exception {
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
        assertEquals("Typed set of keys has wrong size", 2, s.size());
        assertFalse("Typed set of keys has unexpected member", s.contains(ka));
        assertTrue("Typed set of keys is missing a key", s.contains(kb));
        assertTrue("Typed set of keys is missing a key", s.contains(kc));
        assertFalse("Typed set of keys has unexpected member", s.contains(kd));
    }

    @Test
    public void iterator() throws Exception {
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
        assertEquals("Iterator iterated incorrect increments", 3, i);
        assertTrue("Iterator missed an entry", seen_a);
        assertTrue("Iterator missed an entry", seen_b);
        assertTrue("Iterator missed an entry", seen_c);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void iterator_remove() throws Exception {
        final TypedMap.Key<Long> ka = TypedMap.key("1", Long.class);
        final Long va = Long.valueOf(999999999);

        TypedMap data = ImmutableTypedMap.Builder.with(ka, va).build();

        Iterator<TypedMap.Entry> it = data.iterator();
        it.remove();
    }

    @Test
    public void forEach() throws Exception {
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
        assertTrue("forEach missed an entry", seen[0]);
        assertTrue("forEach missed an entry", seen[1]);
        assertTrue("forEach missed an entry", seen[2]);
        assertFalse("forEach hit an unexpected entry", seen[3]);
    }

    @Test
    public void typedIterator() throws Exception {
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
        assertEquals("Iterator iterated incorrect increments", 2, i);
        assertTrue("Typed iterator missed an entry", seen_a);
        assertFalse("Typed iterator returned an entry of wrong type", seen_b);
        assertTrue("Typed iterator missed an entry", seen_c);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void typedIterator_remove() throws Exception {
        final TypedMap.Key<String> ka = TypedMap.key("y", String.class);
        final String va = "yes";

        TypedMap data = ImmutableTypedMap.Builder.with(ka, va).build();

        Iterator<TypedMap.Entry<String>> it = data.typedIterator(String.class);
        it.remove();
    }


    @Test
    public void forEachTyped() throws Exception {
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

        assertTrue("forEachTyped missed an entry", seen[0]);
        assertFalse("forEachTyped hit an unexpected entry", seen[1]);
        assertTrue("forEachTyped missed an entry", seen[2]);
        assertFalse("forEachTyped hit an unexpected entry", seen[3]);
        assertFalse("forEachTyped hit an unexpected entry", seen[4]);
    }

    @Test
    public void testMultipleBuildsDontMutateInstances() {
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
