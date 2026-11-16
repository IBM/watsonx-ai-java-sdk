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

    @Test
    void should_replace_the_default_scope_when_request_selects_another_scope() {
        var request = ChatRequest.builder().messages(UserMessage.text("Hi"))
            .parameters(ChatParameters.builder().spaceId("request-space").build()).build();
        var defaults = ChatParameters.builder().projectId("default-project").build();

        var result = ChatUtility.buildTextChatRequest(request, defaults);

        assertNull(result.projectId());
        assertEquals("request-space", result.spaceId());

        request = request.toBuilder().parameters(ChatParameters.builder().projectId("request-project").build()).build();
        defaults = ChatParameters.builder().spaceId("default-space").build();
        result = ChatUtility.buildTextChatRequest(request, defaults);

        assertEquals("request-project", result.projectId());
        assertNull(result.spaceId());
    }

    @Test
    void should_keep_default_scope_when_request_only_changes_generation_parameters() {
        var request = ChatRequest.builder().messages(UserMessage.text("Hi"))
            .parameters(ChatParameters.builder().temperature(0.5).build()).build();

        var result = ChatUtility.buildTextChatRequest(request, ChatParameters.builder().projectId("default-project").build());

        assertEquals("default-project", result.projectId());
        assertNull(result.spaceId());
    }
}
