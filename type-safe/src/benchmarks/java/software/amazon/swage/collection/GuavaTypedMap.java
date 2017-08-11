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
package com.amazon.collection;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of TypedMap backed by a Guava ImmutableMap
 */
public class GuavaTypedMap implements TypedMap {

    private final ImmutableMap<Key, Entry> dataMap;

    private GuavaTypedMap(ImmutableMap<Key, Entry> dataMap) {
        this.dataMap = dataMap;
    }

    @Override
    public <T> T get(final Key<T> key) {
        return (T) this.dataMap.get(key);
    }


    @Override
    public int size() {
        return dataMap.size();
    }

    @Override
    public boolean isEmpty() {
        return dataMap.isEmpty();
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
        return keys;
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
        return entries.iterator();
    }

    @Override
    public Iterator<Entry> iterator() {
        return this.dataMap.values().iterator();
    }

    /**
     * Builds an immutable instance of the map.
     *
     * This builder is basic, different domains will have more specific benchmark
     * and provide more targeted builders.
     */
    public static class Builder {
        private final ImmutableMap.Builder<Key, Entry> builder = ImmutableMap.builder();

        private Builder() {
        }

        public static Builder from(TypedMap data) {
            Builder b = new Builder();
            data.iterator().forEachRemaining((e) -> b.builder.put(e.getKey(), e));
            return b;
        }

        public static <T> Builder with(Key<T> key, T value) {
            Builder b = new Builder();
            b.builder.put(key, new Entry(key, value));
            return b;
        }

        public final <T> Builder add(Key<T> key, T value) {
            this.builder.put(key, new Entry(key, value));
            return this;
        }

        public GuavaTypedMap build() {
            return new GuavaTypedMap(builder.build());
        }
    }

}
