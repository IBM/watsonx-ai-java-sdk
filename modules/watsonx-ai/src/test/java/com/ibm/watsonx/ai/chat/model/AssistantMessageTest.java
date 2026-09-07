/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class AssistantMessageTest {

    @Test
    void should_allow_refusal_only_message() {
        var message = assertDoesNotThrow(
            () -> new AssistantMessage(null, null, null, "I can't help with that", null));
        assertEquals("I can't help with that", message.refusal());
    }

    @Test
    void should_reject_message_without_content_tool_calls_or_refusal() {
        var ex = assertThrows(NullPointerException.class, () -> AssistantMessage.text(null));
        assertEquals("Either content, toolCalls or refusal must be specified", ex.getMessage());
    }

    @Test
    void should_deserialize_content_to_typed_object() {
        var message = new AssistantMessage("42", null, null, null, null);
        assertEquals(42, message.toObject(new TypeToken<Integer>() {}));
    }
}
