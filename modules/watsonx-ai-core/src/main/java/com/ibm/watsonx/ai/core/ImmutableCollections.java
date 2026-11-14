/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates immutable snapshots of collection trees while preserving iteration order and null values. Values other than maps, lists and sets are
 * retained as supplied. Inputs must be acyclic collection trees. Self-references and other collection cycles are not supported.
 * <p>
 * The typed helpers preserve declared element types, so collection values at every nesting level must be declared as {@link Map}, {@link List},
 * {@link Set} or {@link Object}. Concrete collection classes and more specific interfaces are not preserved. Use {@link #copy(Object)} when the
 * source uses such types, and access the resulting collections through the supported interfaces.
 */
public final class ImmutableCollections {

    private ImmutableCollections() {}

    /**
     * Recursively copies map values into a read-only map in the source's iteration order. Keys are retained as supplied.
     *
     * @param source the acyclic map to copy, or {@code null}
     * @param <K> the key type
     * @param <V> the value type, using {@code Map}, {@code List}, {@code Set} or {@code Object} for nested collections
     * @return a read-only snapshot, or {@code null} when the source is null
     */
    public static <K, V> Map<K, V> copyMap(Map<K, V> source) {
        if (source == null)
            return null;
        var copy = new LinkedHashMap<K, V>();
        source.forEach((key, value) -> {
            @SuppressWarnings("unchecked")
            V copiedValue = (V) copy(value);
            copy.put(key, copiedValue);
        });
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Recursively copies list elements into a read-only list, preserving order and null elements.
     *
     * @param source the acyclic list to copy, or {@code null}
     * @param <T> the element type, using {@code Map}, {@code List}, {@code Set} or {@code Object} for nested collections
     * @return a read-only snapshot, or {@code null} when the source is null
     */
    public static <T> List<T> copyList(List<T> source) {
        if (source == null)
            return null;
        var copy = new ArrayList<T>(source.size());
        source.forEach(value -> {
            @SuppressWarnings("unchecked")
            T copiedValue = (T) copy(value);
            copy.add(copiedValue);
        });
        return Collections.unmodifiableList(copy);
    }

    /**
     * Recursively copies set elements into a read-only set, preserving iteration order and null elements.
     *
     * @param source the acyclic set to copy, or {@code null}
     * @param <T> the element type, using {@code Map}, {@code List}, {@code Set} or {@code Object} for nested collections
     * @return a read-only snapshot, or {@code null} when the source is null
     */
    public static <T> Set<T> copySet(Set<T> source) {
        if (source == null)
            return null;
        var copy = new LinkedHashSet<T>();
        source.forEach(value -> {
            @SuppressWarnings("unchecked")
            T copiedValue = (T) copy(value);
            copy.add(copiedValue);
        });
        return Collections.unmodifiableSet(copy);
    }

    /**
     * Recursively snapshots maps, lists and sets. Other values, including null and mutable non-collection objects, are returned unchanged. Collection
     * results implement {@link Map}, {@link List} or {@link Set} and do not retain concrete source implementation types. The return type is
     * {@code Object} because a snapshot need not implement the source's concrete class or more specific collection interfaces.
     *
     * @param value the value or acyclic collection tree to copy
     * @return the read-only collection snapshot or the original non-collection value
     */
    public static Object copy(Object value) {
        if (value instanceof Map<?, ?> map)
            return copyMap(map);
        if (value instanceof List<?> list)
            return copyList(list);
        if (value instanceof Set<?> set)
            return copySet(set);
        return value;
    }
}
