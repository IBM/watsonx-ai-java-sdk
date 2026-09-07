/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.ToolMessage;

public class ToolExecutorTest {

    @Test
    void should_process_tool_calls_and_return_tool_messages() {
        var assistantMessage = new AssistantMessage(null, null, null, null, List.of(ToolCall.of("id", "name", "{ \"code\": \"code\"}")));
        var toolMessages = assistantMessage.processTools((toolName, toolArgs) -> {
            JSONAssert.assertEquals("{ \"code\": \"code\"}", toolArgs.toString(), true);
            assertEquals("name", toolName);
            assertEquals("code", toolArgs.get("code"));
            return "result";
        });
        assertEquals(1, toolMessages.size());
        var toolMessage = assertInstanceOf(ToolMessage.class, toolMessages.get(0));
        assertEquals("id", toolMessage.toolCallId());
        assertEquals("result", toolMessage.content());
    }

    @Test
    void should_return_an_empty_list_of_tool_messages() {
        var assistantMessage = new AssistantMessage(null, null, null, null, List.of());

        ToolExecutor toolExecutor = (toolName, toolArgs) -> "";
        var toolMessages = assistantMessage.processTools(toolExecutor);
        assertEquals(0, toolMessages.size());

        assistantMessage = new AssistantMessage("Hello", null, null, null, null);
        toolMessages = assistantMessage.processTools(toolExecutor);
        assertEquals(0, toolMessages.size());
    }

    @Test
    void should_process_a_tool_call_without_parameters() {
        var assistantMessage = new AssistantMessage(null, null, null, null, List.of(ToolCall.of("id", "name", null)));
        var toolMessages = assistantMessage.processTools((toolName, toolArgs) -> {
            assertEquals("name", toolName);
            assertNull(toolArgs);
            return "result";
        });
        assertEquals(1, toolMessages.size());
        var toolMessage = assertInstanceOf(ToolMessage.class, toolMessages.get(0));
        assertEquals("id", toolMessage.toolCallId());
        assertEquals("result", toolMessage.content());
    }

    @Test
    void should_unwrap_double_quoted_json_arguments() {
        ToolCall toolCall = ToolCall.of("id", "name", "\"{ \\\"name\\\": \\\"value\\\" }\"");
        var toolMessage = toolCall.processTool((toolName, toolArgs) -> {
            assertEquals("name", toolName);
            assertEquals("value", toolArgs.get("name"));
            return toolArgs.get("name");
        });
        assertEquals("value", toolMessage.content());
    }
}
