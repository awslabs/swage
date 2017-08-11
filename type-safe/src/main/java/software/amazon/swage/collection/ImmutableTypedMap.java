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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable implementation of a TypedMap.
 *
 * This is a naive implementation backed by a standard HashMap.
 */
public final class ImmutableTypedMap implements TypedMap {

    // Holder for the data
    private final Map<Key, Entry> dataMap;

    /**
     * This should be called by builders, and naught else.
     * @param dataMap Typesafe heterogeneous map of context data
     */
    private ImmutableTypedMap(Map<Key<?>, Entry<?>> dataMap) {
        this.dataMap = Collections.unmodifiableMap(dataMap);
    }


    /**
     * Get the piece of data
     * @param key Key to retrieve data for
     * @param <T> Type of the data
     * @return Data stored under the given key, or null if no mapping exists for the key
     */
    @Override
    public final <T> T get(Key<T> key) {
        Entry e = this.dataMap.get(key);
        if (e != null) {
            // value type is determined by key, this is a guaranteed safe cast
            return (T) e.getValue();
        }
        return null;
    }

    @Override
    public int size() {
        return this.dataMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.dataMap.isEmpty();
    }

    @Override
    public Iterator<Entry> iterator() {
        return this.dataMap.values().iterator();
    }

    @Override
    public <T> Iterator<Entry<T>> typedIterator(Class<T> clazz) {
        //TODO: something less brain-dead
        List<Entry<T>> entries = new ArrayList();
        for (Iterator<Entry> it = this.iterator(); it.hasNext(); ) {
            final Entry e = it.next();
            if (e.getKey().valueType.equals(clazz)) {
                entries.add(e);
            }
        }
        return Collections.unmodifiableCollection(entries).iterator();
    }

    @Override
    public Set<Key> keySet() {
        return this.dataMap.keySet();
    }

    @Override
    public <T> Set<Key<T>> typedKeySet(final Class<T> clazz) {
        //TODO: something less brain-dead
        Set<Key<T>> keys = new HashSet<>();
        for (Key k : this.keySet()) {
            if (k.valueType.equals(clazz)) {
                keys.add(k);
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public boolean containsKey(final Key<?> key) {
        return this.dataMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        for (Map.Entry<Key, Entry> e : this.dataMap.entrySet()) {
            if (e.getValue().getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Builds an immutable instance of the container.
     *
     * This builder is basic, different domains may have more specific data
     * and provide more targeted builders extending this one.
     */
    public static class Builder {
        private final Map<Key<?>, Entry<?>> dataMap;

        protected Builder() {
            this.dataMap = new IdentityHashMap<>();
        }

        public static Builder from(TypedMap data) {
            Builder b = new Builder();
            data.iterator().forEachRemaining((e) -> b.dataMap.put(e.getKey(), e));
            return b;
        }

        public static <T> Builder with(Key<T> key, T value) {
            Builder b = new Builder();
            b.dataMap.put(key, new Entry(key, value));
            return b;
        }

        public final <T> Builder add(Key<T> key, T value) {
            this.dataMap.put(key, new Entry(key, value));
            return this;
        }

        public ImmutableTypedMap build() {
            return new ImmutableTypedMap(dataMap);
        }
    }

}
