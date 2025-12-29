/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ThinkingEffort;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ChatRequestTest {

    @Test
    void should_not_set_thinking_when_null_is_passed() {

        ExtractionTags tags = null;

        var chatRequest = ChatRequest.builder()
            .messages(UserMessage.text("Hello"))
            .thinking(tags)
            .build();

        assertNull(chatRequest.thinking());

        ThinkingEffort thinkingEffort = null;

        chatRequest = ChatRequest.builder()
            .messages(UserMessage.text("Hello"))
            .thinking(thinkingEffort)
            .build();

        assertNull(chatRequest.thinking());
    }
}
