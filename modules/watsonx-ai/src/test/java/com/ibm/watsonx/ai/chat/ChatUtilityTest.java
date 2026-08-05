/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ChatUtilityTest {

    private static final String MODEL_ID = "meta-llama/llama-3-3-70b-instruct";

    @Test
    void should_build_when_chat_parameters_supplied() {
        var request = ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .parameters(ChatParameters.builder().modelId(MODEL_ID).build())
            .build();

        var result = assertDoesNotThrow(() -> ChatUtility.buildTextChatRequest(request, null));

        assertNotNull(result);
        assertEquals(MODEL_ID, result.modelId());
    }

    @Test
    void should_build_when_no_parameters_supplied() {
        var request = ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .build();

        var result = assertDoesNotThrow(() -> ChatUtility.buildTextChatRequest(request, null));

        assertNotNull(result);
        assertNull(result.modelId());
    }
}
