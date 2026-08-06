/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ImproveSchemaExceptionTest {

    @Test
    void should_store_code_and_message() {
        var ex = new ImproveSchemaException("E001", "improve failed");
        assertEquals("E001", ex.code());
        assertEquals("improve failed", ex.getMessage());
    }

    @Test
    void should_store_code_message_and_cause() {
        var cause = new RuntimeException("root");
        var ex = new ImproveSchemaException("E002", "improve failed with cause", cause);
        assertEquals("E002", ex.code());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void should_include_code_and_message_in_toString() {
        var ex = new ImproveSchemaException("E003", "some message");
        assertTrue(ex.toString().contains("E003"));
        assertTrue(ex.toString().contains("some message"));
    }
}
