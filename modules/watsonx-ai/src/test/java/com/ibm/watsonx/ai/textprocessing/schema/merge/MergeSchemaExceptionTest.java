/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MergeSchemaExceptionTest {

    @Test
    void should_store_code_and_message() {
        var ex = new MergeSchemaException("M001", "merge failed");
        assertEquals("M001", ex.code());
        assertEquals("merge failed", ex.getMessage());
    }

    @Test
    void should_store_code_message_and_cause() {
        var cause = new RuntimeException("root");
        var ex = new MergeSchemaException("M002", "merge failed with cause", cause);
        assertEquals("M002", ex.code());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void should_include_code_and_message_in_toString() {
        var ex = new MergeSchemaException("M003", "some message");
        assertTrue(ex.toString().contains("M003"));
        assertTrue(ex.toString().contains("some message"));
    }
}
