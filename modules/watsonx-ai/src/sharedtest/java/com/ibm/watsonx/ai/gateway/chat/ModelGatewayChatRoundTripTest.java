/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionEntry;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult.Position;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Think;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Cache;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Prediction;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Router;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.StreamOptions;

@DisabledInNativeImage
public class ModelGatewayChatRoundTripTest {

    @Test
    void should_serialize_and_deserialize_the_gateway_chat_response() {

        var EXPECTED = """
            {
                "id": "chat-1",
                "object": "chat.completion",
                "model_id": "gpt-4o",
                "model": "gpt-4o",
                "choices": [
                    { "index": 0, "message": { "role": "assistant", "content": "Hello there!" }, "finish_reason": "stop" }
                ],
                "created": 1735689600,
                "model_version": "1.0.0",
                "created_at": "2026-08-04T10:00:00.000Z",
                "usage": { "completion_tokens": 10, "prompt_tokens": 20, "total_tokens": 30 },
                "extraction_tags": {
                    "think": { "opening": "<think>", "closing": "</think>" },
                    "response": { "opening": "<response>", "closing": "</response>" }
                },
                "moderations": {
                    "output": [ { "score": 0.9, "input": false, "position": { "start": 0, "end": 4 }, "entity": "EmailAddress", "word": "test" } ]
                },
                "detections": {
                    "output": [
                        { "choice_index": 0, "results": [
                            { "detector_id": "en_syntax_rbr_pii", "detection_type": "pii", "detection": "PhoneNumber", "score": 0.8, "text": "123", "start": 0, "end": 3 }
                        ] }
                    ]
                },
                "service_tier": "default",
                "system_fingerprint": "fp_abc123",
                "cached": false
            }""";

        var response = ModelGatewayChatResponse.builder()
            .id("chat-1")
            .object("chat.completion")
            .model("gpt-4o")
            .modelId("gpt-4o")
            .modelVersion("1.0.0")
            .createdAt("2026-08-04T10:00:00.000Z")
            .choices(List.of(new ResultChoice(0, new ResultMessage("assistant", "Hello there!", null, null, null), "stop")))
            .created(1735689600L)
            .usage(new ChatUsage(10, 20, 30))
            .extractionTags(ExtractionTags.of(new Think("<think>", "</think>"), new ExtractionTags.Response("<response>", "</response>")))
            .moderations(Map.of("output", List.of(new ModerationResult(0.9f, false, new Position(0, 4), "EmailAddress", "test"))))
            .detections(Map.of("output", List.of(new DetectionEntry(0, List.of(
                new DetectionResult("en_syntax_rbr_pii", "pii", "PhoneNumber", 0.8, "123", 0, 3))))))
            .serviceTier("default")
            .systemFingerprint("fp_abc123")
            .cached(false)
            .build();

        var json = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertFalse(json.contains("\"systemFingerprint\""));

        var roundTripped = Json.fromJson(json, ModelGatewayChatResponse.class);
        assertEquals(response, roundTripped);
    }

    @Test
    void should_serialize_the_text_chat_request_with_every_gateway_property() {

        var EXPECTED = """
            {
                "model": "gpt-4o",
                "messages": [ { "role": "user", "content": [ { "type": "text", "text": "Hello there" } ] } ],
                "tools": [ { "type": "function", "function": { "name": "get_weather", "description": "Gets the current weather for a city" } } ],
                "tool_choice": "auto",
                "frequency_penalty": 0.1,
                "logit_bias": { "50256": -100 },
                "logprobs": true,
                "top_logprobs": 3,
                "max_completion_tokens": 256,
                "n": 1,
                "presence_penalty": 0.2,
                "seed": 42,
                "stop": ["STOP"],
                "temperature": 0.7,
                "top_p": 0.9,
                "response_format": { "type": "json_object" },
                "audio": { "voice": "alloy" },
                "metadata": { "env": "test" },
                "modalities": ["text"],
                "parallel_tool_calls": true,
                "prediction": { "type": "content", "content": "The quick brown fox" },
                "reasoning_effort": "medium",
                "service_tier": "auto",
                "store": false,
                "stream_options": { "include_usage": true },
                "router": { "cache": { "enabled": true, "threshold": 0.8 } },
                "user": "user-123",
                "stream": true
            }""";

        var request = ModelGatewayTextChatRequest.builder()
            .model("gpt-4o")
            .messages(List.<ChatMessage>of(UserMessage.text("Hello there")))
            .tools(List.of(Tool.of("get_weather", "Gets the current weather for a city")))
            .toolChoice("auto")
            .frequencyPenalty(0.1)
            .logitBias(Map.of("50256", -100))
            .logprobs(true)
            .topLogprobs(3)
            .maxCompletionTokens(256)
            .n(1)
            .presencePenalty(0.2)
            .seed(42)
            .stop(List.of("STOP"))
            .temperature(0.7)
            .topP(0.9)
            .timeLimit(5000L)
            .responseFormat(Map.of("type", "json_object"))
            .audio(Map.of("voice", "alloy"))
            .metadata(Map.of("env", "test"))
            .modalities(List.of("text"))
            .parallelToolCalls(true)
            .prediction(new Prediction("content", "The quick brown fox"))
            .reasoningEffort("medium")
            .serviceTier("auto")
            .store(false)
            .streamOptions(new StreamOptions(true))
            .router(new Router(new Cache(true, null, 0.8)))
            .user("user-123")
            .stream(true)
            .build();

        var json = Json.toJson(request);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertFalse(json.contains("time_limit"));
        assertFalse(json.contains("timeLimit"));
    }

    @Test
    void should_round_trip_the_text_chat_request_through_its_builder() {

        var request = ModelGatewayTextChatRequest.builder()
            .model("gpt-4o")
            .temperature(0.5)
            .maxCompletionTokens(128)
            .prediction(new Prediction("content", "known text"))
            .streamOptions(new StreamOptions(false))
            .router(new Router(new Cache(false, null, null)))
            .reasoningEffort("low")
            .serviceTier("flex")
            .store(true)
            .user("user-456")
            .stream(false)
            .build();

        var roundTripped = Json.fromJson(Json.toJson(request), ModelGatewayTextChatRequest.class);
        assertEquals(request, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_prediction_configuration() {

        var EXPECTED = """
            { "type": "content", "content": "The quick brown fox jumps over the lazy dog" }""";

        var prediction = new Prediction("content", "The quick brown fox jumps over the lazy dog");

        var json = Json.toJson(prediction);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(prediction, Json.fromJson(json, Prediction.class));
    }

    @Test
    void should_serialize_and_deserialize_the_stream_options() {

        var EXPECTED = """
            { "include_usage": true }""";

        var streamOptions = new StreamOptions(true);

        var json = Json.toJson(streamOptions);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(streamOptions, Json.fromJson(json, StreamOptions.class));
    }

    @Test
    void should_serialize_and_deserialize_the_cache_configuration() {

        var EXPECTED = """
            { "enabled": true, "filter": "recent", "threshold": 0.85 }""";

        var cache = new Cache(true, "recent", 0.85);

        var json = Json.toJson(cache);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(cache, Json.fromJson(json, Cache.class));
    }

    @Test
    void should_serialize_and_deserialize_the_router_configuration() {

        var EXPECTED = """
            { "cache": { "enabled": true, "threshold": 0.8 } }""";

        var router = new Router(new Cache(true, null, 0.8));

        var json = Json.toJson(router);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(router, Json.fromJson(json, Router.class));
    }
}
