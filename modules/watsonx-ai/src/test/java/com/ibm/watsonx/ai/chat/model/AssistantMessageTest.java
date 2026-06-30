/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

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
}
