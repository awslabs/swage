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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A typesafe heterogeneous map where multiple values can be stored for
 * the same type, with keys using reference-equality for comparison.
 *
 * <p>
 * This is similar to a {@link java.util.Map}, but allowing different entries in the
 * map to have different types.  The entries are strongly typed, with the key
 * specifying the type of the value stored with it.
 * <p>
 * This is similar to a {@link java.util.EnumMap}, but with the
 * definition of keys decoupled from an Enum definition.  This allows usage of
 * keys following the extensible enum pattern, or any other scheme, as desired.
 * Keys may additionally be named with a String identifier to assist in de/serializing.
 * <p>
 * This is similar to a {@link java.util.IdentityHashMap}, in that keys are compared
 * based only on their identity, not their values.  Two different key objects
 * with the exact same value semantics could store different values in this map.
 * <p>
 * Uses of the map are expected to define the keys they use up-front for their
 * particular domain, using data only for those well defined keys.  Keys are
 * specific to the usage for a TypedMap, arbitrarily creating new keys is
 * discouraged.
 * <p>
 * This does not use the {@link java.util.Map} interface as the semantics are subtly
 * different.  Entries in this map have their value types constrained by the
 * key, not the map itself.
 *
 * <p>
 * Implementations should be immutable and thread safe.  No mutating operations
 * are provided on this interface.
 *
 * <p>
 * This is useful in 'bag of crap' scenarios, where a collection of data needs
 * to be used but each individual chunk of data may be of a different type.
 *
 * TODO: builder defined here?
 * TODO: explore auto-generating builders based on keys, perhaps with Immutables lib
 */
public interface TypedMap extends Iterable<TypedMap.Entry> {

    /**
     * Creates an instance of a Key for use when storing data.
     * Users of a TypedMap are expected to have some well-defined, constrained
     * set of keys under which to store data, similar to an enum.
     * Each key will be compared with object identity. The name of a key is
     * additional metadata to help in interpreting data, it is distinct from
     * identity of the key itself.
     *
     * TODO: namespace keys
     * TODO: a variant taking a TypeLiteral or equivalent
     *
     * @param name Name for the data stored under this key, appropriate for identifying when serialized
     * @param valueType Class type of the data to be stored under this key
     * @param <T> Class type of the data to be stored under this key
     * @return Key to use for storing/retrieving data in the TypedMap
     */
    public static <T> Key<T> key(String name, Class<T> valueType) {
        return new Key<>(name, valueType);
    }

    /**
     * A key for identifying values stored in the TypedMap.
     * Keys are explicitly compared by identity only - two different key
     * instances are considered different regardless of their values.
     * The members of a key object are additional metadata to help interpret
     * the data stored under the key, distinct from identity of the key itself.
     *
     * @param <T> The type of value for this key
     */
    public static final class Key<T> {
        public final String name;
        public final Class<T> valueType;

        private Key(final String key, final Class<T> valueType) {
            this.name = key;
            this.valueType = valueType;
        }

        /**
         * @return The string name of the key
         */
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Key<?> key = (Key<?>) o;
            return Objects.equals(name, key.name) &&
                    Objects.equals(valueType, key.valueType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, valueType);
        }
    }

    /**
     * An entry in the container, including key and value.
     * Similar to {@link java.util.Map.Entry}, but with a strongly-typed value
     * that has the type specified in the key.
     *
     * @param <T> Type of the value data.
     */
    public static final class Entry<T> {
        private final Key<T> key;
        private final T value;

        public Entry(final Key<T> key, final T value) {
            this.key = key;
            this.value = value;
        }

        public Key<T> getKey() {
            return key;
        }
        public T getValue() {
            return value;
        }
    }

    /**
     * Get the piece of data stored for the given key
     * @param key Key to retrieve data for
     * @param <T> Type of the data
     * @return Data stored under the given key, or null if nothing exists for the key
     */
    public <T> T get(Key<T> key);

    /**
     * Returns an Optional containing the value stored for the given key, or
     * empty if the key does not have a value mapped.
     * @param key Key to retrieve data for
     * @param <T> Type of the data
     * @return Optional containing value stored under the given key, or empty if nothing exists for the key
     */
    public default <T> Optional<T> getOptional(Key<T> key) {
        T value = get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Returns true if this object contains a mapping for the specified key.
     * @param key Key whose presence is being tested
     * @return true if a mapping exists for the key, false otherwise
     */
    public boolean containsKey(Key<?> key);

    /**
     * Returns true if this object maps one or more keys to the specified value.
     * @param value Value whose presence is being tested
     * @return true if there is a mapping to the value, false otherwise
     */
    public boolean containsValue(Object value);

    /**
     * Returns the number of key-value pairs stored.
     * @return the number of key-value pairs stored
     */
    public int size();

    /**
     * Returns true if there is nothing stored in this object.
     * @return true if there is nothing here, false otherwise
     */
    public boolean isEmpty();

    /**
     * Returns a Set view of the keys that have mappings.
     * The returned set will be immutable.
     * @return a Set view of the keys used in this object
     */
    public Set<Key> keySet();

    /**
     * Returns a Set view of the keys with the given type that have mappings.
     * This is a subset of the keys returned with {@link #keySet()}
     * The returned set will be immutable.
     * @param clazz  Type of entries for the keys
     * @param <T>    Type of entries for the keys
     * @return a Set view of the keys with the given type used in this object
     */
    public <T> Set<Key<T>> typedKeySet(Class<T> clazz);

    /**
     * Returns an iterator over all the entries in this object.
     * There is no guarantee of order.
     * The returned iterator will throw {@link UnsupportedOperationException} on {@link Iterator#remove()}
     * @return an Iterator over all the entries in this object
     */
    public Iterator<Entry> iterator();


    /**
     * Returns an iterator over entries with values of the specified class.
     * The entries iterated over are a subset of the entries given by {@link #iterator()}.
     * There is no guarantee of order.
     * The returned iterator will throw {@link UnsupportedOperationException} on {@link Iterator#remove()}
     * @param clazz  Type of values to iterate over
     * @param <T>    Type of values to iterate over
     * @return an Iterator over entries with values of the specified class
     */
    public <T> Iterator<Entry<T>> typedIterator(Class<T> clazz);

    /**
     * Returns a Spliterator over entries in this object.
     * A default implementation is provided which is equivalent to the
     * default implementation on the Collection interface.
     * @return a Spliterator over entries in this object
     */
    public default Spliterator<Entry> spliterator() {
        return Spliterators.spliterator(this.iterator(), this.size(), 0);
    }

    /**
     * Returns a sequential Stream with this object as the source.
     * A default implementation is provided which is equivalent to the
     * default implementation on the Collection interface.
     * @return a sequential Stream with this object as the source
     */
    public default Stream<Entry> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    //TODO: parallelStream?

    /**
     * Performs the given action for each entry in this object, until each
     * entry has been processed or the action throws an exception.
     * There is no guarantee of order for the actions performed.
     * A default implementation is provided which is equivalent to the
     * default implementation on the Map interface.
     * @param action The action to be performed for each entry
     */
    public default void forEach(Consumer<? super Entry> action) {
        for (Iterator<Entry> it = this.iterator(); it.hasNext(); ) {
            final Entry e = it.next();
            action.accept(e);
        }
    }

    /**
     * Performs the given action for each entry which has a value of type T,
     * until all such entries have been processed or the action throws an
     * exception.
     * There is no guarantee of order for the actions performed.
     * A default implementation is provided which uses typedIterator.
     * @param clazz  Type of values to iterate over
     * @param <T>    Type of values to iterate over
     * @param action The action to be performed for each entry.
     */
    public default <T> void forEachTyped(Class<T> clazz, Consumer<? super Entry<T>> action) {
        this.typedIterator(clazz).forEachRemaining(action);
    }

    /**
     * Returns an empty TypedMap.  Use when you need a data container but don't
     * have any data.  Always returns the same empty instance.
     * @return An empty TypedMap
     */
    public static TypedMap empty() {
        return EMPTY;
    }

    /**
     * The empty TypedMap instance.
     */
    static final TypedMap EMPTY = new TypedMap() {
        @Override
        public <T> T get(final Key<T> key) {
            return null;
        }
        @Override
        public <T> Optional<T> getOptional(final Key<T> key) {
            return Optional.empty();
        }
        @Override
        public boolean containsKey(final Key<?> key) {
            return false;
        }
        @Override
        public boolean containsValue(final Object value) {
            return false;
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
        @Override
        public Set<Key> keySet() {
            return Collections.emptySet();
        }
        @Override
        public <T> Set<Key<T>> typedKeySet(final Class<T> clazz) {
            return Collections.emptySet();
        }

        @Override
        public Iterator<Entry> iterator() {
            return new Iterator<Entry>() {
                @Override
                public boolean hasNext() { return false; }
                @Override
                public Entry next() { throw new NoSuchElementException(); }
            };
        }

        @Override
        public <T> Iterator<Entry<T>> typedIterator(final Class<T> clazz) {
            return new Iterator<Entry<T>>() {
                @Override
                public boolean hasNext() { return false; }
                @Override
                public Entry next() { throw new NoSuchElementException(); }
            };
        }
    };

}
