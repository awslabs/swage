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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An immutable implementation of TypedMap backed by a dumb list
 */
public class ListTypedMap implements TypedMap {

    private final List<TypedMap.Entry> data;

    private ListTypedMap(List<TypedMap.Entry> entries) {
        this.data = Collections.unmodifiableList(entries);
    }

    /**
     * Builds an immutable instance of the map.
     *
     * This builder is basic, different domains will have more specific benchmark
     * and provide more targeted builders.
     */
    public static class Builder {
        private final ArrayList<TypedMap.Entry> data = new ArrayList<>();

        protected Builder() {
        }

        public static Builder from(TypedMap data) {
            Builder b = new Builder();
            data.iterator().forEachRemaining((e) -> b.data.add(e));
            return b;
        }

        public static <T> Builder with(Key<T> key, T value) {
            Builder b = new Builder();
            b.data.add(new Entry(key, value));
            return b;
        }

        public final <T> Builder add(Key<T> key, T value) {
            this.data.add(new TypedMap.Entry(key, value));
            return this;
        }

        public ListTypedMap build() {
            return new ListTypedMap(data);
        }
    }


    @Override
    public final <T> T get(Key<T> key) {
        for (TypedMap.Entry e : this.data) {
            if (e.getKey().equals(key)) {
                return (T) e.getValue();
            }
        }
        return null;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Set<Key> keySet() {
        Set<Key> s = new HashSet<>(this.data.size());
        for (Entry<?> e : this.data) {
            s.add(e.getKey());
        }
        return Collections.unmodifiableSet(s);
    }

    @Override
    public <T> Set<Key<T>> typedKeySet(final Class<T> clazz) {
        Set<Key<T>> keys = new HashSet<>();
        for (Entry<?> e : this.data) {
            if (e.getKey().valueType.equals(clazz)) {
                keys.add((Key<T>) e.getKey());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public boolean containsKey(final Key key) {
        for (Entry<?> e : this.data) {
            if (e.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(final Object value) {
        for (Entry<?> e : this.data) {
            if (e.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i != data.size();
            }

            @Override
            public Entry<?> next() {
                if (i >= data.size()) {
                    throw new NoSuchElementException();
                }
                return data.get(i++);
            }
        };
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

}
