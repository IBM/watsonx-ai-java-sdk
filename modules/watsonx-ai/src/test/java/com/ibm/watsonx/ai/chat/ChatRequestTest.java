/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ResponseFormat;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ThinkingEffort;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;

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

    @Test
    void should_set_tools_correctly() {

        var mockToolService = mock(ToolService.class);
        var googleSearchTool = new GoogleSearchTool(mockToolService);

        var chatRequest = ChatRequest.builder()
            .addMessages(UserMessage.text("Hello"))
            .tools(googleSearchTool)
            .build();

        assertEquals(googleSearchTool.name(), chatRequest.tools().get(0).function().name());
        assertEquals(googleSearchTool.schema(), chatRequest.tools().get(0));
    }

    @Test
    void should_resolve_all_response_format_values() {
        assertEquals(ResponseFormat.TEXT, ResponseFormat.from("text"));
        assertEquals(ResponseFormat.JSON, ResponseFormat.from("json_object"));
        assertEquals(ResponseFormat.JSON_SCHEMA, ResponseFormat.from("json_schema"));
        assertEquals("text", ResponseFormat.TEXT.value());
        assertEquals("json_object", ResponseFormat.JSON.value());
        assertEquals("json_schema", ResponseFormat.JSON_SCHEMA.value());
        assertThrows(IllegalArgumentException.class, () -> ResponseFormat.from("unknown"));
    }

    @Test
    void should_resolve_all_tool_choice_option_values() {
        assertEquals(ToolChoiceOption.AUTO, ToolChoiceOption.fromValue("auto"));
        assertEquals(ToolChoiceOption.REQUIRED, ToolChoiceOption.fromValue("required"));
        assertEquals(ToolChoiceOption.NONE, ToolChoiceOption.fromValue("none"));
        assertEquals("auto", ToolChoiceOption.AUTO.value());
        assertEquals("required", ToolChoiceOption.REQUIRED.value());
        assertEquals("none", ToolChoiceOption.NONE.value());
        assertThrows(IllegalArgumentException.class, () -> ToolChoiceOption.fromValue("unknown_option"));
    }
}
