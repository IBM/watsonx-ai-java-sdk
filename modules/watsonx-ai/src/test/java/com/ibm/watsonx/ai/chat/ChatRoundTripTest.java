/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.core.Json;

public class ChatRoundTripTest {

    @Test
    void should_serialize_text_chat_request_with_nested_messages_and_hide_assistant_thinking() {
        var systemMessage = SystemMessage.of("You are a helpful assistant.", "system-1");
        var assistantMessage = new AssistantMessage("Sure thing.", "internal chain-of-thought reasoning", "assistant-1", null, null);
        var tool = Tool.of("get_weather");

        var request = TextChatRequest.builder()
            .modelId("ibm/granite-3-8b-instruct")
            .spaceId("space-123")
            .projectId("project-456")
            .messages(List.of(systemMessage, assistantMessage))
            .tools(List.of(tool))
            .toolChoiceOption("auto")
            .frequencyPenalty(0.5)
            .logitBias(Map.of("50256", -100))
            .logprobs(true)
            .topLogprobs(3)
            .maxCompletionTokens(256)
            .n(1)
            .presencePenalty(0.2)
            .seed(42)
            .stop(List.of("\n\n"))
            .temperature(0.7)
            .topP(0.9)
            .timeLimit(30000L)
            .chatTemplateKwargs(Map.of("enable_thinking", true))
            .includeReasoning(true)
            .reasoningEffort("medium")
            .guidedChoice(Set.of("yes"))
            .context("some context")
            .build();

        var EXPECTED = """
            {
              "model_id": "ibm/granite-3-8b-instruct",
              "model": "ibm/granite-3-8b-instruct",
              "space_id": "space-123",
              "project_id": "project-456",
              "messages": [
                {"role": "system", "content": "You are a helpful assistant.", "name": "system-1"},
                {"role": "assistant", "content": "Sure thing.", "name": "assistant-1"}
              ],
              "tools": [
                {"type": "function", "function": {"name": "get_weather"}}
              ],
              "tool_choice_option": "auto",
              "frequency_penalty": 0.5,
              "logit_bias": {"50256": -100},
              "logprobs": true,
              "top_logprobs": 3,
              "max_completion_tokens": 256,
              "n": 1,
              "presence_penalty": 0.2,
              "seed": 42,
              "stop": ["\\n\\n"],
              "temperature": 0.7,
              "top_p": 0.9,
              "time_limit": 30000,
              "chat_template_kwargs": {"enable_thinking": true},
              "include_reasoning": true,
              "reasoning_effort": "medium",
              "guided_choice": ["yes"],
              "context": "some context"
            }
            """;

        var actual = Json.toJson(request);
        JSONAssert.assertEquals(EXPECTED, actual, true);
        assertFalse(actual.contains("\"thinking\""));
    }

    @Test
    void should_round_trip_text_chat_request_through_builder_mixin() {
        var tool = Tool.of("get_weather");

        var request = TextChatRequest.builder()
            .modelId("ibm/granite-3-8b-instruct")
            .spaceId("space-123")
            .projectId("project-456")
            .tools(List.of(tool))
            .toolChoiceOption("auto")
            .toolChoice(Map.of("type", "function", "name", "get_weather"))
            .frequencyPenalty(0.5)
            .logitBias(Map.of("50256", -100))
            .logprobs(true)
            .topLogprobs(3)
            .maxCompletionTokens(256)
            .n(1)
            .presencePenalty(0.2)
            .seed(42)
            .stop(List.of("\n\n"))
            .temperature(0.7)
            .topP(0.9)
            .timeLimit(30000L)
            .chatTemplateKwargs(Map.of("enable_thinking", true))
            .includeReasoning(true)
            .reasoningEffort("medium")
            .guidedChoice(Set.of("yes"))
            .context("some context")
            .build();

        var json = Json.toJson(request);
        var roundTripped = Json.fromJson(json, TextChatRequest.class);

        assertEquals(request, roundTripped);
    }

    @Test
    void should_round_trip_chat_response_through_builder_mixin() {
        var usage = new ChatUsage(42, 10, 52);
        var extractionTags = ExtractionTags.of(
            new ExtractionTags.Think("<think>", "</think>"),
            new ExtractionTags.Response("<response>", "</response>"));
        var resultMessage = new ResultMessage("assistant", "Hello there", null, null, null);
        var choice = new ChatResponse.ResultChoice(0, resultMessage, "stop");

        var response = ChatResponse.builder()
            .id("chatcmpl-123")
            .object("chat.completion")
            .model("ibm/granite-3-8b-instruct")
            .choices(List.of(choice))
            .created(1700000000L)
            .usage(usage)
            .extractionTags(extractionTags)
            .build();

        var EXPECTED = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "model": "ibm/granite-3-8b-instruct",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "Hello there"}, "finish_reason": "stop"}
              ],
              "created": 1700000000,
              "usage": {"completion_tokens": 42, "prompt_tokens": 10, "total_tokens": 52},
              "extraction_tags": {
                "think": {"opening": "<think>", "closing": "</think>"},
                "response": {"opening": "<response>", "closing": "</response>"}
              }
            }
            """;

        var actual = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, actual, true);

        var roundTripped = Json.fromJson(actual, ChatResponse.class);
        assertEquals(response, roundTripped);
    }

    @Test
    void should_hide_derived_blocked_by_moderation_and_round_trip_text_chat_response() {
        var usage = new ChatUsage(20, 5, 25);
        var resultMessage = new ResultMessage("assistant", "The result is 42.", null, null, null);
        var choice = new ChatResponse.ResultChoice(0, resultMessage, "stop");

        var response = TextChatResponse.builder()
            .id("chatcmpl-456")
            .object("chat.completion")
            .modelId("ibm/granite-3-8b-instruct")
            .model("ibm/granite-3-8b-instruct")
            .choices(List.of(choice))
            .created(1700000001L)
            .modelVersion("1.0.0")
            .createdAt("2026-01-01T00:00:00Z")
            .usage(usage)
            .build();

        var EXPECTED = """
            {
              "id": "chatcmpl-456",
              "object": "chat.completion",
              "model_id": "ibm/granite-3-8b-instruct",
              "model": "ibm/granite-3-8b-instruct",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "The result is 42."}, "finish_reason": "stop"}
              ],
              "created": 1700000001,
              "model_version": "1.0.0",
              "created_at": "2026-01-01T00:00:00Z",
              "usage": {"completion_tokens": 20, "prompt_tokens": 5, "total_tokens": 25}
            }
            """;

        var actual = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, actual, true);
        assertFalse(actual.contains("blocked"));

        var roundTripped = Json.fromJson(actual, TextChatResponse.class);
        assertEquals(response, roundTripped);
        assertFalse(roundTripped.isBlockedByModeration());
    }

    @Test
    void should_deserialize_extraction_tags_via_json_creator() {
        var JSON = """
            {"think": {"opening": "<think>", "closing": "</think>"}, "response": {"opening": "<response>", "closing": "</response>"}}
            """;

        var tags = Json.fromJson(JSON, ExtractionTags.class);

        assertEquals(new ExtractionTags.Think("<think>", "</think>"), tags.think());
        assertEquals(new ExtractionTags.Response("<response>", "</response>"), tags.response());
        JSONAssert.assertEquals(JSON, Json.toJson(tags), true);
    }

    @Test
    void should_flatten_tool_arguments_raw_map_via_any_getter() {
        AtomicReference<ToolArguments> captured = new AtomicReference<>();
        ToolExecutor executor = (name, args) -> {
            captured.set(args);
            return "ok";
        };

        ToolCall.of("call-1", "get_weather", "{\"city\":\"Rome\",\"days\":3}").processTool(executor);

        var toolArguments = captured.get();
        assertNotNull(toolArguments);
        assertEquals("Rome", toolArguments.get("city"));
        JSONAssert.assertEquals("{\"city\": \"Rome\", \"days\": 3}", Json.toJson(toolArguments), true);
    }

    @Test
    void should_ignore_thinking_on_assistant_message_serialization_and_deserialization() {
        var original = new AssistantMessage("Sure thing.", "internal chain-of-thought reasoning", "assistant-1", null, null);
        assertNotNull(original.thinking());

        var json = Json.toJson(original);
        assertFalse(json.contains("\"thinking\""));
        assertFalse(json.contains("chain-of-thought"));

        var roundTripped = Json.fromJson(json, AssistantMessage.class);
        assertEquals(original.content(), roundTripped.content());
        assertEquals(original.name(), roundTripped.name());
        assertNull(roundTripped.thinking());
    }
}
