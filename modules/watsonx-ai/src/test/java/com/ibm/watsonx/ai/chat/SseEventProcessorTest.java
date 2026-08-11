/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.CompleteToolCallEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialThinkingEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialToolCallEvent;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;

public class SseEventProcessorTest {

    @Test
    void should_propagate_moderations_and_detections_from_streaming_chunks() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var firstChunk = "data: "
            + """
                {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Sure, your phone numbers are 3572865321."}}],\
                "created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.380Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":29,"end":39},"entity":"PhoneNumber","word":"3572865321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"3572865321","start":29,"end":39}]}]}}""";

        var stopChunk = "data: " + """
            {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant"}}],\
            "created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.381Z"}""";

        var usageChunk = "data: " + """
            {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[],"created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.381Z",\
            "usage":{"completion_tokens":19,"prompt_tokens":49,"total_tokens":68}}""";

        processor.processChunk(firstChunk);
        processor.processChunk(stopChunk);
        processor.processChunk(usageChunk);

        var response = processor.buildResponse();

        assertNotNull(response.moderations());
        assertEquals(1, response.moderations().get("pii").size());
        var mod = response.moderations().get("pii").get(0);
        assertEquals(0.8f, mod.score());
        assertEquals("PhoneNumber", mod.entity());
        assertEquals(29, mod.position().start());
        assertEquals(39, mod.position().end());

        assertNotNull(response.detections());
        assertEquals(1, response.detections().get("output").size());
        var det = response.detections().get("output").get(0);
        assertEquals(0, det.choiceIndex());
        assertEquals("en_syntax_rbr_pii", det.results().get(0).detectorId());
        assertEquals("PhoneNumber", det.results().get(0).detection());

        assertNotNull(response.usage());
        assertEquals(68, response.usage().totalTokens());
        assertEquals("stop", response.choices().get(0).finishReason());
    }

    @Test
    void should_aggregate_moderations_and_detections_across_multiple_chunks() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        // Chunk 1: empty moderations, empty output detections (the shape mistral emits before any match).
        var chunk1 = "data: " + """
            {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Here are the phone numbers you provided:"}}],\
            "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.000Z",\
            "moderations":{},"detections":{"output":[{"choice_index":0,"results":[]}]}}""";

        // Chunk 2: first PII match.
        var chunk2 = "data: "
            + """
                {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"\\n1. 357-286-5321"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.001Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":7,"end":19},"entity":"PhoneNumber","word":"357-286-5321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"357-286-5321","start":7,"end":19}]}]}}""";

        // Chunk 3: second PII match, finish_reason=stop.
        var chunk3 = "data: "
            + """
                {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant","content":"\\n2. 213-234-8765"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.002Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":6,"end":18},"entity":"PhoneNumber","word":"213-234-8765"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"213-234-8765","start":6,"end":18}]}]}}""";

        processor.processChunk(chunk1);
        processor.processChunk(chunk2);
        processor.processChunk(chunk3);

        var response = processor.buildResponse();

        var piiMatches = response.moderations().get("pii");
        assertNotNull(piiMatches);
        assertEquals(2, piiMatches.size(), "moderations from all chunks should be aggregated");
        assertEquals("357-286-5321", piiMatches.get(0).word());
        assertEquals("213-234-8765", piiMatches.get(1).word());

        var outputEntries = response.detections().get("output");
        assertNotNull(outputEntries);
        assertEquals(1, outputEntries.size(), "detections grouped by choice_index should collapse to one entry");
        var results = outputEntries.get(0).results();
        assertEquals(2, results.size(), "detection results from all chunks should be aggregated for the same choice_index");
        assertTrue(results.stream().anyMatch(r -> "357-286-5321".equals(r.text())));
        assertTrue(results.stream().anyMatch(r -> "213-234-8765".equals(r.text())));
    }

    @Test
    void should_expose_per_chunk_moderations_on_partial_response_event() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        // Chunk 1: content without any PII match, no moderations flagged.
        var chunk1 = "data: " + """
            {"id":"chatcmpl-3","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Here are the phone numbers you provided:"}}],\
            "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.000Z",\
            "moderations":{},"detections":{"output":[{"choice_index":0,"results":[]}]}}""";

        // Chunk 2: content contains a PII match, moderations populated.
        var chunk2 = "data: "
            + """
                {"id":"chatcmpl-3","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"\\n1. 357-286-5321"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.001Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":7,"end":19},"entity":"PhoneNumber","word":"357-286-5321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"357-286-5321","start":7,"end":19}]}]}}""";

        var events1 = processor.processChunk(chunk1);
        var events2 = processor.processChunk(chunk2);

        var partial1 = events1.events().stream()
            .filter(PartialResponseEvent.class::isInstance)
            .map(PartialResponseEvent.class::cast)
            .findFirst()
            .orElseThrow();

        // Chunk 1 has no PII match: moderations is empty, detections.output[0].results is empty.
        assertNotNull(partial1.chunk().moderations());
        assertTrue(partial1.chunk().moderations().isEmpty());
        assertNotNull(partial1.chunk().detections());
        assertTrue(partial1.chunk().detections().get("output").get(0).results().isEmpty());

        var partial2 = events2.events().stream()
            .filter(PartialResponseEvent.class::isInstance)
            .map(PartialResponseEvent.class::cast)
            .findFirst()
            .orElseThrow();

        // Chunk 2 carries the PII match: a handler can react (block, tag, log) at this precise moment.
        var chunkPii = partial2.chunk().moderations().get("pii");
        assertNotNull(chunkPii);
        assertEquals(1, chunkPii.size());
        assertEquals("PhoneNumber", chunkPii.get(0).entity());
        assertEquals("357-286-5321", chunkPii.get(0).word());
        assertEquals(7, chunkPii.get(0).position().start());
        assertEquals(19, chunkPii.get(0).position().end());
    }

    @Test
    void should_keep_content_events_when_reasoning_content_is_empty_on_same_chunk() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var chunk = "data: " + """
            {"id":"chatcmpl-4","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b","model":"openai/gpt-oss-120b",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Hello","reasoning_content":""}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var result = processor.processChunk(chunk);

        var responseEvents = result.events().stream()
            .filter(PartialResponseEvent.class::isInstance)
            .map(PartialResponseEvent.class::cast)
            .toList();

        assertEquals(1, responseEvents.size(), "the content event must survive an empty reasoning_content on the same chunk");
        assertEquals("Hello", responseEvents.get(0).content());

        // The empty reasoning token must not emit a thinking event.
        assertFalse(result.events().stream().anyMatch(PartialThinkingEvent.class::isInstance));
    }

    @Test
    void should_complete_every_tool_call_per_choice_when_n_is_greater_than_one() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        // n=2: choices are streamed interleaved, one per chunk. choices[0] requests two tools, choices[1] a single one.

        // choices[0], tool 0: name and first argument fragment.
        var chunk1 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"sum","arguments":"{\\"a\\":1"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        // choices[1], tool 0: a complete tool call in a single delta.
        var chunk2 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":1,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_c","type":"function","function":{"name":"now","arguments":"{\\"tz\\":\\"UTC\\"}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        // choices[0], tool 0: remaining argument fragment.
        var chunk3 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"function":{"arguments":",\\"b\\":2}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z"}""";

        // choices[0], tool 1: starting a new tool completes the previous one of the same choice.
        var chunk4 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":1,"id":"call_b","type":"function","function":{"name":"diff","arguments":"{\\"a\\":3,\\"b\\":4}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.003Z"}""";

        // Terminal chunk of choices[0]: completes its last tool.
        var chunk5 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.004Z"}""";

        // Terminal chunk of choices[1]: completes its own tool, independently of choices[0].
        var chunk6 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":1,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.005Z"}""";

        var completed = Stream.of(chunk1, chunk2, chunk3, chunk4, chunk5, chunk6)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast)
            .map(CompleteToolCallEvent::completeToolCall)
            .toList();

        assertEquals(3, completed.size(), "every tool of every choice must be completed exactly once");

        // choices[0], tool 0: completed when tool 1 of the same choice starts, with both argument fragments joined.
        assertEquals(0, completed.get(0).index());
        assertEquals(0, completed.get(0).toolCall().index());
        assertEquals("call_a", completed.get(0).toolCall().id());
        assertEquals("sum", completed.get(0).toolCall().function().name());
        assertEquals("{\"a\":1,\"b\":2}", completed.get(0).toolCall().function().arguments());

        // choices[0], tool 1: completed by the terminal chunk of choices[0].
        assertEquals(0, completed.get(1).index());
        assertEquals(1, completed.get(1).toolCall().index());
        assertEquals("call_b", completed.get(1).toolCall().id());
        assertEquals("diff", completed.get(1).toolCall().function().name());

        // choices[1], tool 0: completed by its own terminal chunk, not swallowed by the one of choices[0].
        assertEquals(1, completed.get(2).index());
        assertEquals(0, completed.get(2).toolCall().index());
        assertEquals("call_c", completed.get(2).toolCall().id());
        assertEquals("now", completed.get(2).toolCall().function().name());
        assertEquals("{\"tz\":\"UTC\"}", completed.get(2).toolCall().function().arguments());

        var response = processor.buildResponse();
        assertEquals(2, response.choices().size());

        for (var choice : response.choices()) {
            assertEquals("tool_calls", choice.finishReason(), "each choice must keep its own finish reason");
            var expectedTools = choice.index() == 0 ? 2 : 1;
            assertEquals(expectedTools, choice.message().toolCalls().size());
        }
    }

    @Test
    void should_throw_when_the_stream_carries_no_usable_output() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var roleChunk = "data: " + """
            {"id":"chatcmpl-6","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var stopChunk = "data: " + """
            {"id":"chatcmpl-6","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        processor.processChunk(roleChunk);
        processor.processChunk(stopChunk);

        var response = processor.buildResponse();

        // The choice exists, only its content is empty.
        assertEquals(1, response.choices().size());
        assertNull(response.choices().get(0).message().content());

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals("The model generated no content, tool calls or refusal (finish reason: STOP)", ex.getMessage());
        assertEquals(FinishReason.STOP, ex.finishReason());
        assertEquals(0, ex.index());
    }

    @Test
    void should_throw_when_the_stream_is_truncated_while_the_model_is_thinking() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var thinkingChunk = "data: " + """
            {"id":"chatcmpl-7","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b","model":"openai/gpt-oss-120b",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","reasoning_content":"The user is asking for"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var lengthChunk = "data: " + """
            {"id":"chatcmpl-7","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b","model":"openai/gpt-oss-120b",\
            "choices":[{"index":0,"finish_reason":"length","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        processor.processChunk(thinkingChunk);
        processor.processChunk(lengthChunk);

        var response = processor.buildResponse();
        assertEquals("The user is asking for", response.choices().get(0).message().reasoningContent());

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals(FinishReason.LENGTH, ex.finishReason());
        assertEquals(0, ex.index());
    }

    @Test
    void should_emit_a_single_partial_tool_call_for_a_parameterless_tool() {

        var processor = new SseEventProcessor(List.of(Tool.of("get_current_time")), null, TextChatResponse::builder);

        // The Streaming API sends empty arguments for a tool without parameters, and the number of deltas depends on the model.
        var firstDelta = "data: " + """
            {"id":"chatcmpl-8","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"get_current_time","arguments":""}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var secondDelta = "data: " + """
            {"id":"chatcmpl-8","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"function":{"name":"","arguments":""}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        var terminalChunk = "data: " + """
            {"id":"chatcmpl-8","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z"}""";

        var events = Stream.of(firstDelta, secondDelta, terminalChunk)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .toList();

        var partials = events.stream()
            .filter(PartialToolCallEvent.class::isInstance)
            .map(PartialToolCallEvent.class::cast)
            .map(PartialToolCallEvent::toolCall)
            .toList();

        assertEquals(1, partials.size(), "the synthetic empty arguments must be emitted once per tool call");
        assertEquals("{}", partials.get(0).arguments());
        assertEquals("get_current_time", partials.get(0).name());
        assertEquals("call_a", partials.get(0).id());
        assertEquals(0, partials.get(0).toolIndex());

        var completed = events.stream()
            .filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast)
            .map(CompleteToolCallEvent::completeToolCall)
            .toList();

        assertEquals(1, completed.size());
        assertEquals("{}", completed.get(0).toolCall().function().arguments());

        assertEquals(2, events.size(), "the synthetic fragment and the completion are emitted by the terminal chunk");
        assertInstanceOf(PartialToolCallEvent.class, events.get(0), "the partial tool call must precede the completion");
        assertInstanceOf(CompleteToolCallEvent.class, events.get(1));
    }

    @Test
    void should_emit_a_single_partial_tool_call_when_a_parameterless_tool_streams_the_empty_arguments() {

        var processor = new SseEventProcessor(List.of(Tool.of("get_current_time")), null, TextChatResponse::builder);

        // OpenAI-compatible endpoints (Model Gateway) do return an empty object for a tool without parameters.
        var nameDelta = "data: " + """
            {"id":"chatcmpl-13","object":"chat.completion.chunk","model":"gpt-4o",\
            "choices":[{"index":0,"finish_reason":"","delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"get_current_time","arguments":""}}]}}],\
            "created":1}""";

        var argumentsDelta = "data: " + """
            {"id":"chatcmpl-13","object":"chat.completion.chunk","model":"gpt-4o",\
            "choices":[{"index":0,"finish_reason":"","delta":{"tool_calls":[\
            {"index":0,"function":{"arguments":"{}"}}]}}],\
            "created":1}""";

        var terminalChunk = "data: " + """
            {"id":"chatcmpl-13","object":"chat.completion.chunk","model":"gpt-4o",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}],\
            "created":1}""";

        var usageChunk = "data: " + """
            {"id":"chatcmpl-13","object":"chat.completion.chunk","model":"gpt-4o","choices":[],\
            "created":1,"usage":{"completion_tokens":12,"prompt_tokens":45,"total_tokens":57}}""";

        var events = Stream.of(nameDelta, argumentsDelta, terminalChunk, usageChunk)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .toList();

        var partials = events.stream()
            .filter(PartialToolCallEvent.class::isInstance)
            .map(PartialToolCallEvent.class::cast)
            .map(PartialToolCallEvent::toolCall)
            .toList();

        assertEquals(1, partials.size(), "the streamed fragment must not be followed by a synthetic one");
        assertEquals("{}", partials.get(0).arguments());
        assertEquals("get_current_time", partials.get(0).name());
        assertEquals("call_a", partials.get(0).id());

        var completed = events.stream()
            .filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast)
            .map(CompleteToolCallEvent::completeToolCall)
            .toList();

        assertEquals(1, completed.size());
        assertEquals("{}", completed.get(0).toolCall().function().arguments(), "the streamed fragments must add up to the final arguments");
    }

    @Test
    void should_emit_a_single_partial_tool_call_for_each_parameterless_tool() {

        var tools = List.of(Tool.of("get_current_time"), Tool.of("get_current_date"));
        var processor = new SseEventProcessor(tools, null, TextChatResponse::builder);

        var firstToolDelta = "data: " + """
            {"id":"chatcmpl-14","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"get_current_time","arguments":""}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var secondToolDelta = "data: " + """
            {"id":"chatcmpl-14","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[\
            {"index":1,"id":"call_b","type":"function","function":{"name":"get_current_date","arguments":""}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        var terminalChunk = "data: " + """
            {"id":"chatcmpl-14","object":"chat.completion.chunk","model_id":"google/gemma","model":"google/gemma",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z"}""";

        var events = Stream.of(firstToolDelta, secondToolDelta, terminalChunk)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .toList();

        var partials = events.stream()
            .filter(PartialToolCallEvent.class::isInstance)
            .map(PartialToolCallEvent.class::cast)
            .map(PartialToolCallEvent::toolCall)
            .toList();

        assertEquals(2, partials.size(), "each tool call must get its own synthetic empty arguments");

        assertEquals(0, partials.get(0).toolIndex());
        assertEquals("get_current_time", partials.get(0).name());
        assertEquals("{}", partials.get(0).arguments());

        assertEquals(1, partials.get(1).toolIndex());
        assertEquals("get_current_date", partials.get(1).name());
        assertEquals("{}", partials.get(1).arguments());

        // The tool call of index 0 is completed when the one of index 1 starts, its partial must come first.
        assertEquals(4, events.size());
        assertInstanceOf(PartialToolCallEvent.class, events.get(0));
        assertInstanceOf(CompleteToolCallEvent.class, events.get(1));
        assertInstanceOf(PartialToolCallEvent.class, events.get(2));
        assertInstanceOf(CompleteToolCallEvent.class, events.get(3));
    }

    @Test
    void should_emit_the_synthetic_partial_tool_call_with_the_generated_id() {

        var processor = new SseEventProcessor(List.of(Tool.of("get_current_time")), null, TextChatResponse::builder);

        // Watsonx doesn't return "id" nor FinishReason.TOOL_CALLS if the option tool-choice is set to REQUIRED.
        var toolCallDelta = "data: " + """
            {"id":"chatcmpl-15","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"type":"function","function":{"name":"get_current_time","arguments":""}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var usageChunk = "data: " + """
            {"id":"chatcmpl-15","object":"chat.completion.chunk","model_id":"mistral","model":"mistral","choices":[],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z",\
            "usage":{"completion_tokens":8,"prompt_tokens":243,"total_tokens":251}}""";

        var events = Stream.of(toolCallDelta, usageChunk)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .toList();

        assertEquals(2, events.size());

        var partial = assertInstanceOf(PartialToolCallEvent.class, events.get(0)).toolCall();
        var completed = assertInstanceOf(CompleteToolCallEvent.class, events.get(1)).completeToolCall();

        assertNotNull(partial.id());
        assertEquals(completed.toolCall().id(), partial.id(), "the partial must carry the same id as the completed tool call");
        assertEquals("{}", partial.arguments());
        assertEquals("{}", completed.toolCall().function().arguments());
        assertEquals("tool_calls", processor.buildResponse().choices().get(0).finishReason());
    }

    @Test
    void should_not_fail_when_a_tool_call_delta_has_no_arguments() {

        var tool = Tool.of("get_current_time", JsonSchema.object().property("country", JsonSchema.string()));
        var processor = new SseEventProcessor(List.of(tool), null, TextChatResponse::builder);

        var delta = "data: " + """
            {"id":"chatcmpl-9","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"get_current_time"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var events = processor.processChunk(delta).events();

        assertFalse(events.stream().anyMatch(PartialToolCallEvent.class::isInstance), "a delta without arguments carries no fragment to emit");
    }

    @Test
    void should_not_fail_when_the_finish_reason_is_tool_calls_without_any_tool_call() {

        var tool = Tool.of("getWeather", JsonSchema.object().property("city", JsonSchema.string()));
        var processor = new SseEventProcessor(List.of(tool), null, TextChatResponse::builder);

        var roleChunk = "data: " + """
            {"id":"chatcmpl-10","object":"chat.completion.chunk","model_id":"meta-llama/llama-3-3-70b-instruct",\
            "model":"meta-llama/llama-3-3-70b-instruct",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var toolCallsChunk = "data: " + """
            {"id":"chatcmpl-10","object":"chat.completion.chunk","model_id":"meta-llama/llama-3-3-70b-instruct",\
            "model":"meta-llama/llama-3-3-70b-instruct",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        var usageChunk = "data: " + """
            {"id":"chatcmpl-10","object":"chat.completion.chunk","model_id":"meta-llama/llama-3-3-70b-instruct",\
            "model":"meta-llama/llama-3-3-70b-instruct","choices":[],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z",\
            "usage":{"completion_tokens":8,"prompt_tokens":243,"total_tokens":251}}""";

        assertTrue(processor.processChunk(roleChunk).events().isEmpty());
        assertTrue(processor.processChunk(toolCallsChunk).events().isEmpty(), "there is no tool call to complete");
        assertTrue(processor.processChunk(usageChunk).events().isEmpty());

        var response = processor.buildResponse();

        assertEquals(1, response.choices().size());
        assertNull(response.choices().get(0).message().content());
        assertNull(response.choices().get(0).message().toolCalls());
        assertEquals("tool_calls", response.choices().get(0).finishReason());
        assertEquals(251, response.usage().totalTokens());

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals("The model generated no content, tool calls or refusal (finish reason: TOOL_CALLS)", ex.getMessage());
        assertEquals(FinishReason.TOOL_CALLS, ex.finishReason());
        assertEquals(0, ex.index());
    }

    @Test
    void should_complete_the_tool_call_of_the_choice_that_has_one() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        // n=2: choices[0] streams a tool call, choices[1] declares "tool_calls" without emitting any.
        var toolCallChunk = "data: " + """
            {"id":"chatcmpl-11","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"sum","arguments":"{\\"a\\":1,\\"b\\":2}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var emptyToolCallsChunk = "data: " + """
            {"id":"chatcmpl-11","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":1,"finish_reason":"tool_calls","delta":{}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        var terminalChunk = "data: " + """
            {"id":"chatcmpl-11","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z"}""";

        var completed = Stream.of(toolCallChunk, emptyToolCallsChunk, terminalChunk)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast)
            .map(CompleteToolCallEvent::completeToolCall)
            .toList();

        assertEquals(1, completed.size(), "the guard must not suppress the legitimate completion of choices[0]");
        assertEquals(0, completed.get(0).index());
        assertEquals("call_a", completed.get(0).toolCall().id());
        assertEquals("{\"a\":1,\"b\":2}", completed.get(0).toolCall().function().arguments());
    }

    @Test
    void should_accumulate_tool_call_deltas_that_arrive_out_of_order() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        // The tool call of index 1 is streamed first, the one of index 0 afterwards.
        var chunk1 = "data: " + """
            {"id":"chatcmpl-12","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":1,"id":"call_b","type":"function","function":{"name":"diff","arguments":"{\\"a\\":3"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var chunk2 = "data: " + """
            {"id":"chatcmpl-12","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[\
            {"index":1,"function":{"arguments":",\\"b\\":4}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        var chunk3 = "data: " + """
            {"id":"chatcmpl-12","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[\
            {"index":0,"id":"call_a","type":"function","function":{"name":"sum","arguments":"{\\"a\\":1,\\"b\\":2}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.002Z"}""";

        var chunk4 = "data: " + """
            {"id":"chatcmpl-12","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":"tool_calls","delta":{"role":"assistant"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.003Z"}""";

        var completed = Stream.of(chunk1, chunk2, chunk3, chunk4)
            .map(processor::processChunk)
            .flatMap(result -> result.events().stream())
            .filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast)
            .map(CompleteToolCallEvent::completeToolCall)
            .toList();

        assertEquals(2, completed.size(), "each tool call must be completed exactly once");

        // The tool call of index 1 is completed when the one of index 0 starts, not by itself.
        assertEquals(1, completed.get(0).toolCall().index());
        assertEquals("call_b", completed.get(0).toolCall().id());
        assertEquals("{\"a\":3,\"b\":4}", completed.get(0).toolCall().function().arguments());

        assertEquals(0, completed.get(1).toolCall().index());
        assertEquals("call_a", completed.get(1).toolCall().id());
        assertEquals("{\"a\":1,\"b\":2}", completed.get(1).toolCall().function().arguments());

        var toolCalls = processor.buildResponse().toAssistantMessage().toolCalls();
        assertEquals(2, toolCalls.size(), "the deltas of the two indexes must not be merged into a single tool call");
        assertEquals("diff", toolCalls.get(0).function().name());
        assertEquals("sum", toolCalls.get(1).function().name());
    }
}
