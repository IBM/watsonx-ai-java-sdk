/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;

public class KvpFieldsTest {

    @Test
    void should_be_equal_when_fields_match() {
        var a = KvpFields.builder().add("name", KvpField.of("Full name", "John")).build();
        var b = KvpFields.builder().add("name", KvpField.of("Full name", "John")).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_be_equal_to_itself() {
        var k = KvpFields.builder().add("name", KvpField.of("Full name", "John")).build();
        assertEquals(k, k);
    }

    @Test
    void should_not_be_equal_to_null() {
        assertNotEquals(null, KvpFields.builder().build());
    }

    @Test
    void should_not_be_equal_to_different_type() {
        assertNotEquals("string", KvpFields.builder().build());
    }

    @Test
    void should_not_be_equal_when_fields_differ() {
        var a = KvpFields.builder().add("name", KvpField.of("Full name", "John")).build();
        var b = KvpFields.builder().add("age", KvpField.of("Age", "30")).build();
        assertNotEquals(a, b);
    }

    @Test
    void should_have_null_fields_when_built_empty() {
        var a = KvpFields.builder().build();
        var b = KvpFields.builder().build();
        assertNull(a.fields());
        assertEquals(a, b);
    }

    @Test
    void should_produce_non_null_toString() {
        var k = KvpFields.builder().add("name", KvpField.of("Full name", "John")).build();
        assertTrue(k.toString().contains("KvpFields"));
    }
}
