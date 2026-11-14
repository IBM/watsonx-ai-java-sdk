/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.ibm.watsonx.ai.core.ImmutableCollections.copyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImmutableCollectionsTest {

    @Test
    void should_copy_a_concrete_collection_without_preserving_its_implementation_type() {
        var source = new ArrayList<>(List.of("original"));

        var snapshot = ImmutableCollections.copy(source);

        source.clear();
        var values = assertInstanceOf(List.class, snapshot);
        assertEquals(List.of("original"), values);
        assertThrows(UnsupportedOperationException.class, values::clear);
    }

    @Test
    void should_copy_nested_concrete_collection_types_through_supported_interfaces() {
        var values = new ArrayList<>(List.of("original"));
        var source = new LinkedHashMap<String, ArrayList<String>>();
        source.put("values", values);

        var snapshot = ImmutableCollections.copy(source);

        source.clear();
        values.clear();
        var copiedMap = assertInstanceOf(Map.class, snapshot);
        var copiedValues = assertInstanceOf(List.class, copiedMap.get("values"));
        assertEquals(Map.of("values", List.of("original")), copiedMap);
        assertThrows(UnsupportedOperationException.class, copiedMap::clear);
        assertThrows(UnsupportedOperationException.class, copiedValues::clear);
    }

    @Test
    void should_preserve_nested_collection_interface_types_in_typed_helpers() {
        var values = new ArrayList<>(List.of("original"));
        Map<String, List<String>> source = Map.of("values", values);

        var snapshot = copyMap(source);

        values.clear();
        List<String> copiedValues = snapshot.get("values");
        assertEquals(List.of("original"), copiedValues);
        assertThrows(UnsupportedOperationException.class, copiedValues::clear);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_snapshot_nested_collections_and_preserve_order_and_nulls() {
        var cells = new ArrayList<>(Arrays.asList("original", null));
        var roles = new LinkedHashSet<>(Arrays.asList("second", "first", null));
        var nested = new LinkedHashMap<String, Object>();
        nested.put("cells", cells);
        nested.put("roles", roles);
        var source = new LinkedHashMap<String, Object>();
        source.put("second", nested);
        source.put("first", null);

        var snapshot = copyMap(source);
        cells.set(0, "changed");
        roles.clear();
        nested.clear();
        source.clear();

        assertEquals(List.of("second", "first"), new ArrayList<>(snapshot.keySet()));
        assertNull(snapshot.get("first"));
        var copiedNested = (Map<String, Object>) snapshot.get("second");
        var copiedCells = (List<Object>) copiedNested.get("cells");
        var copiedRoles = (Set<Object>) copiedNested.get("roles");
        assertEquals(Arrays.asList("original", null), copiedCells);
        assertEquals(Arrays.asList("second", "first", null), new ArrayList<>(copiedRoles));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> copiedNested.clear());
        assertThrows(UnsupportedOperationException.class, () -> copiedCells.add("new"));
        assertThrows(UnsupportedOperationException.class, () -> copiedRoles.clear());
    }

    @Test
    void should_preserve_null_collections_and_non_collection_values() {
        assertNull(ImmutableCollections.copyMap(null));
        assertNull(ImmutableCollections.copyList(null));
        assertNull(ImmutableCollections.copySet(null));
        assertNull(ImmutableCollections.copy(null));
        var value = new Object();
        assertSame(value, ImmutableCollections.copy(value));
    }
}
