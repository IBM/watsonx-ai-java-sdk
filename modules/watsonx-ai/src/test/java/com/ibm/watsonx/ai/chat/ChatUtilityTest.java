/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters;

public class ChatUtilityTest {

    private static final String MODEL_ID = "meta-llama/llama-3-3-70b-instruct";

    @Test
    void should_throw_when_gateway_parameters_passed() {
        var request = ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .parameters(ModelGatewayParameters.builder().modelId(MODEL_ID).build())
            .build();

        var ex = assertThrows(IllegalArgumentException.class,
            () -> ChatUtility.buildTextChatRequest(request, null));

        assertTrue(ex.getMessage().contains("ChatService expects ChatParameters"),
            () -> "message should state the expected type but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("ModelGatewayParameters"),
            () -> "message should name the offending type but was: " + ex.getMessage());
    }

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
