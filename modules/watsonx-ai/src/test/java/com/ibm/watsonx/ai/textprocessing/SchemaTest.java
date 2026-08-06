/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SchemaTest {

    @Test
    void should_be_equal_when_all_fields_match() {
        var a = Schema.builder().documentType("invoice").documentDescription("An invoice").build();
        var b = Schema.builder().documentType("invoice").documentDescription("An invoice").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_be_equal_to_itself() {
        var s = Schema.builder().documentType("invoice").build();
        assertEquals(s, s);
    }

    @Test
    void should_not_be_equal_to_null() {
        assertNotEquals(null, Schema.builder().build());
    }

    @Test
    void should_not_be_equal_to_different_type() {
        assertNotEquals("string", Schema.builder().build());
    }

    @Test
    void should_not_be_equal_when_documentType_differs() {
        assertNotEquals(Schema.builder().documentType("invoice").build(), Schema.builder().documentType("receipt").build());
        assertNotEquals(Schema.builder().build(), Schema.builder().documentType("invoice").build());
    }

    @Test
    void should_not_be_equal_when_documentDescription_differs() {
        assertNotEquals(Schema.builder().documentDescription("desc1").build(), Schema.builder().build());
    }

    @Test
    void should_not_be_equal_when_additionalPromptInstructions_differs() {
        assertNotEquals(Schema.builder().additionalPromptInstructions("extra").build(), Schema.builder().build());
    }

    @Test
    void should_include_documentType_in_toString() {
        assertTrue(Schema.builder().documentType("invoice").build().toString().contains("invoice"));
    }
}
