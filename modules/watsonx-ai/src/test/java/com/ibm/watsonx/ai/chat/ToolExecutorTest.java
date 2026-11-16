/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.JsonException;

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
        for (var whitespace : List.of("", " ", "\t", "\r", "\n", " \t\r\n")) {
            ToolCall toolCall = ToolCall.of("id", "name", whitespace + "\"{ \\\"name\\\": \\\"value\\\" }\"" + whitespace);
            var toolMessage = toolCall.processTool((toolName, toolArgs) -> {
                assertEquals("name", toolName);
                assertEquals("value", toolArgs.get("name"));
                return toolArgs.get("name");
            });
            assertEquals("value", toolMessage.content());
        }
    }

    @Test
    void should_reject_non_json_whitespace_before_double_quoted_arguments() {
        for (var whitespace : List.of("\f", "\u2003")) {
            assertRejectedArguments(whitespace + "\"{}\"");
        }
    }

    @Test
    void should_reject_invalid_object_documents_before_executing_tools() {
        for (var arguments : List.of("", "{", "{}{}", "{} trailing")) {
            assertRejectedArguments(arguments);
            assertRejectedArguments(Json.toJson(arguments));
        }
    }

    @Test
    void should_reject_malformed_or_trailing_content_around_encoded_arguments() {
        for (var arguments : List.of("\"{}", Json.toJson("{}") + "{}", Json.toJson("{}") + " trailing")) {
            assertRejectedArguments(arguments);
        }
    }

    @Test
    void should_reject_encoded_arguments_that_do_not_decode_to_an_object() {
        for (var arguments : List.of("[]", "null", "\"{}\"")) {
            assertRejectedArguments(Json.toJson(arguments));
        }
    }

    private void assertRejectedArguments(String arguments) {
        ToolExecutor executor = (name, args) -> fail("The executor must not be called for invalid arguments");
        var toolCall = ToolCall.of("id", "name", arguments);

        assertThrows(JsonException.class, () -> toolCall.processTool(executor), arguments);
    }

    @Test
    void should_expose_read_only_nested_tool_arguments() {
        var call = ToolCall.of("call-1", "search", "{\"items\":[\"original\"]}");
        call.processTool((name, arguments) -> {
            var items = arguments.<List<String>>get("items");
            assertThrows(UnsupportedOperationException.class, () -> items.add("changed"));
            return null;
        });
    }

}
