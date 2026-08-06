/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.textprocessing.GroundingHints.FieldData;

public class GroundingHintsTest {

    @Test
    void should_be_equal_when_fields_match() {
        var a = GroundingHints.builder().add("f1", FieldData.of(List.of(0.0, 0.0, 1.0, 1.0), 1)).build();
        var b = GroundingHints.builder().add("f1", FieldData.of(List.of(0.0, 0.0, 1.0, 1.0), 1)).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_be_equal_to_itself() {
        var g = GroundingHints.builder().add("f1", FieldData.of(List.of(0.1, 0.2, 0.3, 0.4), 1)).build();
        assertEquals(g, g);
    }

    @Test
    void should_not_be_equal_to_null() {
        assertNotEquals(null, GroundingHints.builder().build());
    }

    @Test
    void should_not_be_equal_to_different_type() {
        assertNotEquals("string", GroundingHints.builder().build());
    }

    @Test
    void should_not_be_equal_when_fields_differ() {
        var a = GroundingHints.builder().add("f1", FieldData.of(List.of(0.0, 0.0, 1.0, 1.0), 1)).build();
        var b = GroundingHints.builder().add("f2", FieldData.of(List.of(0.0, 0.0, 1.0, 1.0), 1)).build();
        assertNotEquals(a, b);
    }

    @Test
    void should_produce_non_null_toString() {
        var g = GroundingHints.builder().add("f1", FieldData.of(List.of(0.1, 0.2, 0.3, 0.4), 1)).build();
        assertTrue(g.toString().contains("GroundingHints"));
    }

    @Test
    void should_expose_convenience_accessors() {
        var g = GroundingHints.builder().add("f1", FieldData.of(List.of(0.1, 0.2, 0.9, 0.8), 2)).build();
        assertTrue(g.hasField("f1"));
        assertFalse(g.hasField("missing"));
        assertNotNull(g.field("f1"));
        assertNull(g.field("missing"));
        assertNotNull(g.bbox("f1"));
        assertNull(g.bbox("missing"));
        assertEquals(2, g.pageNumber("f1"));
        assertNull(g.pageNumber("missing"));
        assertTrue(g.fieldNames().contains("f1"));
    }

    @Test
    void should_handle_null_fieldMap_in_convenience_accessors() {
        var g = GroundingHints.builder().fields(null).build();
        assertFalse(g.hasField("x"));
        assertNull(g.field("x"));
        assertNull(g.bbox("x"));
        assertNull(g.pageNumber("x"));
        assertEquals(0, g.fieldNames().size());
    }
}
