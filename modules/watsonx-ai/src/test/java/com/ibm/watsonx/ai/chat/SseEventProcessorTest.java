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
import com.ibm.watsonx.ai.chat.SseEventProcessor.ProcessResult;
import com.ibm.watsonx.ai.chat.exception.EmptyChatResponseException;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Think;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

public class SseEventProcessorTest {

    @Test
    void should_fail_only_unfinished_choices_when_processing_a_stream_error() {
        for (var errorChunks : List.of(List.of("event: error", "data: generation failed"), List.of("data: {"))) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"c","choices":[
                  {"index":0,"delta":{"tool_calls":[{"index":0,"id":"a","function":{"name":"charge","arguments":"{"}}]}},
                  {"index":1,"delta":{"tool_calls":[{"index":0,"id":"b","function":{"name":"time","arguments":"{}"}}]},
                   "finish_reason":"tool_calls"}]}
                """);
            if (errorChunks.size() == 1)
                assertThrows(RuntimeException.class, () -> processor.processChunk(errorChunks.get(0)));
            else {
                processor.processChunk(errorChunks.get(0));
                assertTrue(processor.processChunk(errorChunks.get(1)).hasError());
            }

            var continued = processor.processChunk("""
                data: {"choices":[
                  {"index":0,"finish_reason":"tool_calls","delta":{}},
                  {"index":2,"delta":{"tool_calls":[{"index":0,"id":"d","function":{"name":"date","arguments":"{}"}}]},
                   "finish_reason":"stop"}]}
                """);

            var completed = continued.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall().id()).toList();
            assertEquals(List.of("d"), completed);
            assertTrue(processor.completePendingToolCalls().events().isEmpty());
            var response = processor.buildResponse();
            assertEquals(List.of("error", "tool_calls", "stop"), response.choices().stream().map(choice -> choice.finishReason()).toList());
            assertEquals("{", response.choices().get(0).message().toolCalls().get(0).function().arguments());
        }
    }

    @Test
    void should_not_complete_pending_tools_when_an_error_event_has_no_payload_before_eof() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("""
            data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"id":"a","function":{"name":"charge","arguments":"{"}}]}}]}
            """);

        processor.processChunk("event: error");

        assertTrue(processor.completePendingToolCalls().events().isEmpty());
        assertEquals(FinishReason.ERROR, processor.buildResponse().finishReason());
    }

    @Test
    void should_not_complete_aborted_tool_calls_on_terminal_chunks_or_fallback_signals() {
        for (var finishReason : List.of("length", "time_limit", "cancelled", "error", "content_filter")) {
            for (var terminalDelta : List.of("{}", "null")) {
                for (var argumentsField : List.of(",\"arguments\":\"{\"", ",\"arguments\":\"\"", "")) {
                    var scenario = "finish reason=" + finishReason + ", delta=" + terminalDelta + ", arguments=" + argumentsField;
                    var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
                    processor.processChunk("""
                        data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
                          {"index":0,"id":"a","function":{"name":"charge"%s}}]}}]}
                        """.formatted(argumentsField));

                    var terminal = processor.processChunk("""
                        data: {"choices":[{"index":0,"finish_reason":"%s","delta":%s}]}
                        """.formatted(finishReason, terminalDelta));

                    assertTrue(terminal.events().isEmpty(), scenario);
                    for (var fallback : List.of(
                        "data: {\"choices\":[],\"usage\":{\"total_tokens\":1}}",
                        "data: {\"usage\":{\"total_tokens\":1}}",
                        "data: [DONE]")) {
                        assertTrue(processor.processChunk(fallback).events().isEmpty(), scenario + ", fallback=" + fallback);
                    }
                    assertTrue(processor.completePendingToolCalls().events().isEmpty(), scenario + ", end of stream");
                    var choice = processor.buildResponse().choices().get(0);
                    assertEquals(finishReason, choice.finishReason(), scenario);
                    assertEquals(1, choice.message().toolCalls().size(), scenario);
                }
            }
        }
    }

    @Test
    void should_preserve_unfinished_choices_with_invalid_tool_arguments_on_each_fallback_signal() {
        for (var arguments : List.of("{", "{\"amount\":", "{}{}", "{} trailing", "[]", "null", "\"{", Json.toJson("{"),
            Json.toJson("{}") + Json.toJson("{}"), Json.toJson("{}") + " false", Json.toJson("{}") + "\f",
            " \t" + Json.toJson("{}") + " trailing", "\f" + Json.toJson("{}"), "\u2003" + Json.toJson("{}"))) {
            for (var fallback : List.of("", "data: {\"choices\":[],\"usage\":{\"total_tokens\":1}}",
                "data: {\"usage\":{\"total_tokens\":1}}", "data: [DONE]")) {
                var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
                var initial = processor.processChunk("""
                    data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
                      {"index":0,"id":"valid","function":{"name":"time","arguments":"{}"}},
                      {"index":1,"id":"partial","function":{"name":"charge","arguments":%s}},
                      {"index":2,"id":"empty","function":{"name":"date"}}]}},
                      {"index":1,"delta":{"tool_calls":[
                        {"index":0,"id":"other","function":{"name":"time","arguments":"{}"}}]}}]}
                    """.formatted(Json.toJson(arguments)));

                var early = initial.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                    .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall().id()).toList();
                assertEquals(List.of("valid"), early);
                var result = fallback.isEmpty() ? processor.completePendingToolCalls() : processor.processChunk(fallback);
                var completed = result.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                    .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall().id()).toList();
                var scenario = "arguments=" + arguments + ", fallback=" + fallback;
                assertEquals(List.of("other"), completed, scenario);
                assertEquals(1, result.events().size(), scenario);
                assertTrue(processor.completePendingToolCalls().events().isEmpty(), scenario);
                var response = processor.buildResponse();
                var unfinished = response.choices().get(0);
                assertEquals(FinishReason.INCOMPLETE, FinishReason.fromValue(unfinished.finishReason()), scenario);
                assertEquals(List.of("{}", arguments, "{}"),
                    unfinished.message().toolCalls().stream().map(tool -> tool.function().arguments()).toList(), scenario);
                assertEquals("tool_calls", response.choices().get(1).finishReason(), scenario);
            }
        }
    }

    @Test
    void should_complete_pending_tools_without_a_finish_reason_on_each_fallback_signal() {
        for (var arguments : List.of("", " ", "{}", " {\"amount\":1} ", Json.toJson("{\"country\":\"Italy\"}"),
            " " + Json.toJson("{}"), "\t" + Json.toJson("{}"), "\r" + Json.toJson("{}"), "\n" + Json.toJson("{}"),
            " \t\r\n" + Json.toJson("{\"path\":\"C:\\\\temp\"}") + " \t\r\n")) {
            for (var fallback : List.of("", "data: {\"choices\":[],\"usage\":{\"total_tokens\":1}}",
                "data: {\"usage\":{\"total_tokens\":1}}", "data: [DONE]")) {
                var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
                var initial = processor.processChunk("""
                    data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
                      {"index":0,"id":"a","function":{"name":"time"}},
                      {"index":1,"id":"b","function":{"name":"date","arguments":%s}}]}}]}
                    """.formatted(Json.toJson(arguments)));

                var result = fallback.isEmpty() ? processor.completePendingToolCalls() : processor.processChunk(fallback);

                var early = initial.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                    .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall()).toList();
                assertEquals(List.of("a"), early.stream().map(call -> call.id()).toList());
                assertEquals("{}", early.get(0).function().arguments());
                var completed = result.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                    .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall()).toList();
                var scenario = "arguments=" + arguments + ", fallback=" + fallback;
                assertEquals(List.of("b"), completed.stream().map(call -> call.id()).toList(), scenario);
                assertEquals(List.of(arguments.isBlank() ? "{}" : arguments),
                    completed.stream().map(call -> call.function().arguments()).toList(), scenario);
                assertEquals(FinishReason.TOOL_CALLS, processor.buildResponse().finishReason(), scenario);
                assertTrue(processor.completePendingToolCalls().events().isEmpty(), scenario);
            }
        }
    }

    @Test
    void should_complete_every_tool_call_when_the_terminal_finish_reason_is_stop() {
        for (var terminalDelta : List.of("{}", "null")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            var initial = processor.processChunk("""
                data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"a","type":"function","function":{"name":"f","arguments":"{}"}},
                  {"index":1,"id":"b","type":"function","function":{"name":"g","arguments":"{}"}}]}}]}
                """);

            var terminal = processor.processChunk("data: {\"choices\":[{\"index\":0,\"finish_reason\":\"stop\",\"delta\":"
                + terminalDelta + "}]}");

            var completed = terminal.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                .map(CompleteToolCallEvent.class::cast).toList();
            var early = initial.events().stream().filter(CompleteToolCallEvent.class::isInstance)
                .map(CompleteToolCallEvent.class::cast).toList();
            assertEquals(1, early.size());
            assertEquals("a", early.get(0).completeToolCall().toolCall().id());
            assertEquals(1, completed.size());
            assertEquals("b", completed.get(0).completeToolCall().toolCall().id());
            assertEquals("{}", completed.get(0).completeToolCall().toolCall().function().arguments());
            assertTrue(processor.processChunk("""
                data: {"choices":[],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                """).events().isEmpty());
            assertTrue(processor.processChunk("""
                data: {"choices":[{"index":0,"finish_reason":"stop","delta":{}}]}
                """).events().isEmpty());
        }
    }

    @Test
    void should_not_split_a_tool_call_on_a_blank_function_name_in_a_continuation_fragment() {
        for (var name : List.of("", " ")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"c","choices":[{"index":0,"delta":{"tool_calls":[
                  {"id":"call-a","function":{"name":"time","arguments":"{"}}]}}]}
                """);
            processor.processChunk("data: {\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"id\":\"\",\"function\":{\"name\":\""
                + name + "\",\"arguments\":\"}\"}}]}}]}");
            var terminal = processor.processChunk("""
                data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}]}
                """);

            assertEquals(1, terminal.events().stream().filter(CompleteToolCallEvent.class::isInstance).count());
            var calls = processor.buildResponse().choices().get(0).message().toolCalls();
            assertEquals(1, calls.size());
            assertEquals("time", calls.get(0).function().name());
            assertEquals("call-a", calls.get(0).id());
            assertEquals("{}", calls.get(0).function().arguments());
        }
    }

    @Test
    void should_report_an_empty_error_payload_without_poisoning_the_next_chunk() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("event: error");
        var error = processor.processChunk("data:");
        assertTrue(error.hasError());
        assertEquals("SSE error event without data", error.error().getMessage());

        var next = processor.processChunk("""
            data: {"id":"c","choices":[{"index":0,"delta":{"content":"hi"}}]}
            """);

        assertFalse(next.hasError());
        assertEquals("hi", processor.buildResponse().choices().get(0).message().content());
    }

    @Test
    void should_parse_structured_sse_error_and_preserve_original_payload_and_choices() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("""
            data: {"choices":[
              {"index":0,"delta":{"role":"assistant","content":"Partial answer"}},
              {"index":1,"delta":{"role":"assistant","content":"Complete answer"},"finish_reason":"stop"}]}
            """);
        var payload = """
            {"errors":[{"code":"downstream_request_failed",
            "message":"Downstream vllm request with Model 'openai/gpt-oss-120b failed: context deadline exceeded",
            "more_info":"https://cloud.ibm.com/apidocs/watsonx-ai#text-chat-stream"}],"trace":"request-trace","status_code":500}
            """;

        processor.processChunk("event: error");
        var result = processor.processChunk("data: " + payload);

        assertTrue(result.hasError());
        var error = assertInstanceOf(WatsonxException.class, result.error());
        assertEquals(500, error.statusCode());
        assertEquals(payload, error.getMessage());
        var details = error.details().orElseThrow();
        assertEquals(500, details.statusCode());
        assertEquals("request-trace", details.trace());
        assertEquals(1, details.errors().size());
        assertEquals(WatsonxError.Code.DOWNSTREAM_REQUEST_FAILED.value(), details.errors().get(0).code());
        assertTrue(details.errors().get(0).message().contains("context deadline exceeded"));
        assertEquals("https://cloud.ibm.com/apidocs/watsonx-ai#text-chat-stream", details.errors().get(0).moreInfo());

        var response = processor.buildResponse();
        assertEquals(List.of("error", "stop"), response.choices().stream().map(choice -> choice.finishReason()).toList());
        assertEquals("Partial answer", response.choices().get(0).message().content());
        assertEquals("Complete answer", response.choices().get(1).message().content());

        var next = processor.processChunk("""
            data: {"choices":[{"index":2,"delta":{"content":"Next answer"},"finish_reason":"stop"}]}
            """);
        assertFalse(next.hasError());
        assertEquals("Next answer", processor.buildResponse().choices().get(2).message().content());
    }

    @Test
    void should_map_nested_sse_error_to_specific_watsonx_exception() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var payload = """
            {"status_code":400,"error":{"code":"invalid_input_argument","message":"Unsupported time limit","request_id":"gateway-request"}}
            """;

        processor.processChunk("event: error");
        var result = processor.processChunk("data: " + payload);

        assertTrue(result.hasError());
        var error = assertInstanceOf(InvalidInputArgumentException.class, result.error());
        assertEquals(400, error.statusCode());
        assertEquals(payload, error.getMessage());
        var details = error.details().orElseThrow();
        assertEquals(400, details.statusCode());
        assertEquals("gateway-request", details.trace());
        assertEquals(WatsonxError.Code.INVALID_INPUT_ARGUMENT.value(), details.errors().get(0).code());
        assertEquals("Unsupported time limit", details.errors().get(0).message());
    }

    @Test
    void should_preserve_unrecognized_sse_errors_without_inventing_a_status() {
        for (var payload : List.of(
            "service error",
            " { ",
            "null",
            "[]",
            "{\"errors\":[{\"code\":\"downstream_request_failed\"}]}",
            "{\"status_code\":\"500\",\"errors\":[]}",
            "{\"status_code\":500.7,\"errors\":[]}",
            "{\"status_code\":4294967796,\"errors\":[]}",
            "{\"status_code\":200,\"errors\":[]}",
            "{\"status_code\":500,\"errors\":\"Unavailable\"}",
            "{\"status_code\":500,\"errors\":[{\"code\":7}]}",
            "{\"status_code\":500,\"error\":{\"code\":7}}")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"content":"Partial answer"}}]}
                """);
            processor.processChunk("event: error");

            var result = processor.processChunk("data: " + payload);

            assertTrue(result.hasError(), payload);
            assertEquals(RuntimeException.class, result.error().getClass(), payload);
            assertEquals(payload, result.error().getMessage(), payload);
            assertEquals(FinishReason.ERROR, processor.buildResponse().finishReason(), payload);

            var next = processor.processChunk("""
                data: {"choices":[{"index":1,"delta":{"content":"Next answer"},"finish_reason":"stop"}]}
                """);
            assertFalse(next.hasError(), payload);
            assertEquals("Next answer", processor.buildResponse().choices().get(1).message().content(), payload);
        }
    }

    @Test
    void should_parse_error_event_prefix_with_optional_space() {
        for (var prefix : List.of("event:", "event: ")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            assertFalse(processor.processChunk(prefix + "error").hasError());

            var result = processor.processChunk("data: service error");

            assertTrue(result.hasError());
            assertEquals("service error", result.error().getMessage());
        }
    }

    @Test
    void should_match_the_complete_event_name() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("event: errors_summary");
        var result = processor.processChunk("data: {\"choices\":[]}");

        assertFalse(result.hasError());
    }

    @Test
    void should_parse_data_prefix_with_optional_space_and_preserve_data_in_content() {
        for (var prefix : List.of("data:", "data: ")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk(prefix + """
                {"id":"chat-1","choices":[{"index":0,"delta":{"content":"The data: field"},"finish_reason":"stop"}]}
                """);

            assertEquals("The data: field", processor.buildResponse().toAssistantMessage().content());
            assertTrue(processor.processChunk(prefix).events().isEmpty());
            assertTrue(processor.processChunk(prefix + "[DONE]").events().isEmpty());
        }
    }

    @Test
    void should_keep_terminal_finish_reason_when_delta_is_null() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"content":"Partial answer"}}]}
            """);
        processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"length","delta":null}]}
            """);

        assertEquals(FinishReason.LENGTH, processor.buildResponse().finishReason());
    }

    @Test
    void should_process_every_choice_in_a_chunk_and_keep_refusals_separate() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var result = processor.processChunk("""
            data: {"choices":[
              {"index":17,"delta":{"role":"assistant","content":"Allowed"},"finish_reason":"stop"},
              {"index":0,"delta":{"role":"assistant","refusal":"Cannot "}}
            ]}
            """);
        processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"refusal":"answer."},"finish_reason":"stop"}]}
            """);

        var partial = assertInstanceOf(PartialResponseEvent.class, result.events().get(0));
        assertEquals(17, partial.chunk().index());
        var choices = processor.buildResponse().choices();
        assertEquals(List.of(0, 17), choices.stream().map(ChatResponse.ResultChoice::index).toList());
        assertEquals("Cannot answer.", choices.get(0).message().refusal());
        assertNull(choices.get(1).message().refusal());
        assertEquals("Allowed", choices.get(1).message().content());
    }

    @Test
    void should_keep_thinking_parser_state_separate_for_each_choice() {
        var processor = new SseEventProcessor(List.of(), ExtractionTags.of(new Think("<think>", "</think>")), TextChatResponse::builder);
        var first = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"content":"<think>"}},{"index":1,"delta":{"content":"Second answer"}}]}
            """);
        var second = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"content":"First thought"}}]}
            """);
        processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"content":"</think>"}}]}
            """);
        processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"content":"First answer"},"finish_reason":"stop"}]}
            """);

        var responseEvent = assertInstanceOf(PartialResponseEvent.class, first.events().get(0));
        assertEquals(1, responseEvent.chunk().index());
        assertEquals("Second answer", responseEvent.content());
        var thinkingEvent = assertInstanceOf(PartialThinkingEvent.class, second.events().get(0));
        assertEquals("First thought", thinkingEvent.content());
        assertEquals("First thought", processor.buildResponse().choices().get(0).message().reasoningContent());
        assertNull(processor.buildResponse().choices().get(1).message().reasoningContent());
    }

    @Test
    void should_complete_a_tool_when_terminal_delta_is_null() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("""
            data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call-1",
              "type":"function","function":{"name":"time","arguments":"{}"}}]}}]}
            """);
        var result = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":null}]}
            """);

        assertEquals(1, result.events().stream().filter(CompleteToolCallEvent.class::isInstance).count());
        assertEquals(FinishReason.TOOL_CALLS, processor.buildResponse().finishReason());
    }

    @Test
    void should_complete_the_previous_tool_at_a_greater_index_before_the_last_tool_finishes() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var first = processor.processChunk("""
            data: {"id":"chat-early","choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"id":"call-a","function":{"name":"first","arguments":"{\\"value\\":1}"}}]}}]}
            """);
        assertTrue(completedTools(first).isEmpty(), "valid JSON alone does not finish the current tool");

        var next = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":4,"id":"call-b"}]}}]}
            """);

        var early = completedTools(next);
        assertEquals(1, early.size(), "a metadata-only delta with a greater index also completes the previous tool");
        assertEquals(0, early.get(0).toolCall().index());
        assertEquals("call-a", early.get(0).toolCall().id());
        assertEquals("{\"value\":1}", early.get(0).toolCall().function().arguments());
        assertEquals(1, next.events().size(), "no argument fragment exists for the new metadata-only delta");
        assertNull(processor.buildResponse().choices().get(0).finishReason(), "an early tool completion does not finish its choice");

        var arguments = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[
              {"index":4,"function":{"name":"second","arguments":"{}"}}]}}]}
            """);
        assertTrue(completedTools(arguments).isEmpty(), "the last tool still waits for a terminal signal");
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":null}]}
            """);
        assertEquals(List.of("call-b"), completedTools(terminal).stream().map(tool -> tool.toolCall().id()).toList());
        for (var fallback : List.of("data: {\"choices\":[{\"index\":0,\"finish_reason\":\"tool_calls\",\"delta\":{}}]}",
            "data: {\"choices\":[],\"usage\":{\"total_tokens\":2}}", "data: [DONE]")) {
            assertTrue(completedTools(processor.processChunk(fallback)).isEmpty(), "tool completion must be emitted at most once");
        }
        assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
        assertEquals(early.get(0).toolCall(), processor.buildResponse().choices().get(0).message().toolCalls().get(0));
    }

    @Test
    void should_complete_parameterless_tools_at_the_next_index_with_one_synthetic_partial() {
        for (var argumentsField : List.of("", ",\"arguments\":\"\"", ",\"arguments\":\" \"")) {
            var processor = new SseEventProcessor(List.of(Tool.of("time"), Tool.of("date")), null, TextChatResponse::builder);
            var initial = processor.processChunk("""
                data: {"id":"chat-empty","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"function":{"name":"time"%s}}]}}]}
                """.formatted(argumentsField));
            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":1,"function":{"name":"date"}}]}}]}
                """);

            assertTrue(completedTools(initial).isEmpty());
            var completed = completedTools(next);
            assertEquals(1, completed.size());
            assertEquals("{}", completed.get(0).toolCall().function().arguments());
            assertEquals("time", completed.get(0).toolCall().function().name());
            if (argumentsField.endsWith("\" \"")) {
                assertEquals(1, initial.events().size(), "a streamed whitespace fragment must not be synthesized a second time");
                assertEquals(1, next.events().size());
            } else {
                assertEquals(2, next.events().size());
                var partial = assertInstanceOf(PartialToolCallEvent.class, next.events().get(0)).toolCall();
                assertInstanceOf(CompleteToolCallEvent.class, next.events().get(1));
                assertEquals("{}", partial.arguments());
                assertEquals(completed.get(0).toolCall().id(), partial.id(), "generated IDs stay stable across partial and completion");
            }
            var last = completedTools(processor.completePendingToolCalls());
            assertEquals(List.of("date"), last.stream().map(tool -> tool.toolCall().function().name()).toList());
            assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
        }
    }

    @Test
    void should_keep_a_metadata_only_tool_pending_until_its_name_and_arguments_arrive() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var metadata = processor.processChunk("""
            data: {"id":"chat-metadata","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"a"}]}}]}
            """);
        var next = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[
              {"index":1,"id":"b","function":{"name":"second","arguments":"{}"}}]}}]}
            """);

        assertTrue(metadata.events().isEmpty());
        assertTrue(completedTools(next).isEmpty(), "missing arguments do not make a tool complete before its function name is known");
        assertEquals(1, next.events().size());
        assertInstanceOf(PartialToolCallEvent.class, next.events().get(0));

        var details = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"function":{"name":"first","arguments":"{\\"value\\":1}"}}]}}]}
            """);
        assertTrue(completedTools(details).isEmpty());
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}]}
            """);

        var completed = completedTools(terminal);
        assertEquals(List.of("a", "b"), completed.stream().map(tool -> tool.toolCall().id()).toList());
        assertEquals("first", completed.get(0).toolCall().function().name());
        assertEquals("{\"value\":1}", completed.get(0).toolCall().function().arguments());
        assertEquals(completed.stream().map(CompletedToolCall::toolCall).toList(),
            processor.buildResponse().choices().get(0).message().toolCalls());
        assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
    }

    @Test
    void should_accept_complete_encoded_object_arguments_when_the_next_tool_starts() {
        for (var arguments : List.of(" {\"value\":1} ", Json.toJson("{\"value\":1}"), " \t" + Json.toJson("{}") + "\r\n")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"chat-encoded","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"call-a","function":{"name":"first","arguments":%s}}]}}]}
                """.formatted(Json.toJson(arguments)));

            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":1,"id":"call-b","function":{"name":"second","arguments":"{"}}]}}]}
                """);

            assertEquals(1, completedTools(next).size());
            assertEquals(arguments, completedTools(next).get(0).toolCall().function().arguments());
            assertInstanceOf(CompleteToolCallEvent.class, next.events().get(0), "previous completion precedes the next tool's argument fragment");
            assertInstanceOf(PartialToolCallEvent.class, next.events().get(1));
        }
    }

    @Test
    void should_not_complete_invalid_object_arguments_at_the_next_greater_index() {
        for (var arguments : List.of("{", "[]", "null", "{}{}", "{} trailing", Json.toJson("{"),
            Json.toJson("{}") + " false", Json.toJson(Json.toJson("{}")))) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"chat-invalid","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"call-a","function":{"name":"first","arguments":%s}}]}}]}
                """.formatted(Json.toJson(arguments)));

            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":2,"id":"call-b","function":{"name":"second","arguments":"{"}}]}}]}
                """);

            assertTrue(completedTools(next).isEmpty(), "invalid arguments cannot complete early: " + arguments);
            assertEquals(arguments, processor.buildResponse().choices().get(0).message().toolCalls().get(0).function().arguments());
        }
    }

    @Test
    void should_complete_tools_early_only_within_the_choice_whose_index_advances() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var initial = processor.processChunk("""
            data: {"id":"chat-choices","choices":[
              {"index":0,"delta":{"tool_calls":[{"index":0,"id":"a","function":{"name":"first","arguments":"{}"}}]}},
              {"index":1,"delta":{"tool_calls":[{"index":5,"id":"b","function":{"name":"second","arguments":"{}"}}]}}
            ]}
            """);
        assertTrue(completedTools(initial).isEmpty(), "a tool in another choice is not an index transition");

        var firstChoice = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[
              {"index":1,"id":"c","function":{"name":"third","arguments":"{}"}}]}}]}
            """);
        assertEquals(List.of("a"), completedTools(firstChoice).stream().map(tool -> tool.toolCall().id()).toList());
        assertEquals(0, completedTools(firstChoice).get(0).index());

        var secondChoice = processor.processChunk("""
            data: {"choices":[{"index":1,"delta":{"tool_calls":[
              {"index":6,"id":"d","function":{"name":"fourth","arguments":"{}"}}]}}]}
            """);
        assertEquals(List.of("b"), completedTools(secondChoice).stream().map(tool -> tool.toolCall().id()).toList());
        assertEquals(1, completedTools(secondChoice).get(0).index());
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":1,"finish_reason":"stop","delta":{}},{"index":0,"finish_reason":"tool_calls","delta":{}}]}
            """);
        assertEquals(List.of("d", "c"), completedTools(terminal).stream().map(tool -> tool.toolCall().id()).toList());
        assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
    }

    @Test
    void should_not_complete_tools_early_after_an_abnormal_finish_reason() {
        for (var finishReason : List.of("length", "time_limit", "cancelled", "error", "content_filter")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"chat-aborted","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"a","function":{"name":"first","arguments":"{}"}}]}}]}
                """);
            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"finish_reason":"%s","delta":{"tool_calls":[
                  {"index":1,"id":"b","function":{"name":"second","arguments":"{}"}}]}}]}
                """.formatted(finishReason));
            assertTrue(completedTools(next).isEmpty(), finishReason);
            var continuation = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":2,"id":"c","function":{"name":"third","arguments":"{}"}}]}}]}
                """);
            assertTrue(completedTools(continuation).isEmpty(), finishReason);
            assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty(), finishReason);
        }
    }

    @Test
    void should_preserve_an_early_completion_when_later_tools_are_interrupted() {
        for (var terminal : List.of("data: {\"choices\":[{\"index\":0,\"finish_reason\":\"length\",\"delta\":null}]}",
            "event: error", "data: {")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"chat-later-error","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"a","function":{"name":"first","arguments":"{}"}}]}}]}
                """);
            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":1,"id":"b","function":{"name":"second","arguments":"{"}}]}}]}
                """);
            var early = completedTools(next);
            assertEquals(List.of("a"), early.stream().map(tool -> tool.toolCall().id()).toList());
            if ("data: {".equals(terminal))
                assertThrows(RuntimeException.class, () -> processor.processChunk(terminal));
            else
                assertTrue(completedTools(processor.processChunk(terminal)).isEmpty());
            assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
            var calls = processor.buildResponse().choices().get(0).message().toolCalls();
            assertEquals(early.get(0).toolCall(), calls.get(0));
            assertEquals("{", calls.get(1).function().arguments());
        }
    }

    @Test
    void should_reject_late_deltas_for_completed_tools_without_changing_the_emitted_snapshot() {
        for (var lateFields : List.of("", ",\"id\":\"changed-id\"", ",\"function\":{\"name\":\"changed-name\",\"arguments\":\" trailing\"}")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            processor.processChunk("""
                data: {"id":"chat-late","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"a","function":{"name":"first","arguments":"{}"}}]}}]}
                """);
            var next = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":1,"id":"b","function":{"name":"second","arguments":"{"}}]}}]}
                """);
            var early = completedTools(next);
            assertEquals(1, early.size());

            var error = assertThrows(IllegalStateException.class, () -> processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0%s}]}}]}
                """.formatted(lateFields)));

            assertTrue(error.getMessage().contains("0"), "the error identifies the completed tool index");
            assertTrue(error.getMessage().toLowerCase().contains("complet"), "the error explains why the delta is rejected");
            var calls = processor.buildResponse().choices().get(0).message().toolCalls();
            assertEquals(early.get(0).toolCall(), calls.get(0), "a rejected delta cannot change the tool already delivered to the caller");
            assertEquals("{", calls.get(1).function().arguments());
            assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty(), "a late-delta error cannot emit duplicate completions");
        }
    }

    @Test
    void should_complete_an_interleaved_lower_index_once_its_arguments_are_valid_and_a_higher_delta_arrives() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var initial = processor.processChunk("""
            data: {"id":"chat-interleaved","choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"id":"a","function":{"name":"first","arguments":"{"}},
              {"index":1,"id":"b","function":{"name":"second","arguments":"{"}}]}}]}
            """);
        assertTrue(completedTools(initial).isEmpty());
        var lower = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"}"}}]}}]}
            """);
        assertTrue(completedTools(lower).isEmpty(), "returning to a lower index does not finish the higher tool");

        var higher = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":1,"function":{"arguments":"}"}}]}}]}
            """);

        assertEquals(List.of("a"), completedTools(higher).stream().map(tool -> tool.toolCall().id()).toList());
        assertEquals("{}", completedTools(higher).get(0).toolCall().function().arguments());
        assertInstanceOf(CompleteToolCallEvent.class, higher.events().get(0));
        assertInstanceOf(PartialToolCallEvent.class, higher.events().get(1));
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}]}
            """);
        assertEquals(List.of("b"), completedTools(terminal).stream().map(tool -> tool.toolCall().id()).toList());
        assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
    }

    @Test
    void should_complete_all_valid_lower_pending_tools_before_the_next_higher_partial() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        var initial = processor.processChunk("""
            data: {"id":"chat-lower-tools","choices":[{"index":0,"delta":{"tool_calls":[
              {"index":0,"id":"a","function":{"name":"first","arguments":"{"}},
              {"index":1,"id":"b","function":{"name":"second","arguments":"{}"}}]}}]}
            """);
        assertTrue(completedTools(initial).isEmpty());
        var lower = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"}"}}]}}]}
            """);
        assertTrue(completedTools(lower).isEmpty());

        var higher = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[
              {"index":3,"id":"c","function":{"name":"third","arguments":"{}"}}]}}]}
            """);

        assertEquals(List.of("a", "b"), completedTools(higher).stream().map(tool -> tool.toolCall().id()).toList());
        assertInstanceOf(CompleteToolCallEvent.class, higher.events().get(0));
        assertInstanceOf(CompleteToolCallEvent.class, higher.events().get(1));
        assertInstanceOf(PartialToolCallEvent.class, higher.events().get(2));
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}]}
            """);
        assertEquals(List.of("c"), completedTools(terminal).stream().map(tool -> tool.toolCall().id()).toList());
        assertTrue(completedTools(processor.completePendingToolCalls()).isEmpty());
    }

    @Test
    void should_complete_every_parallel_tool_after_all_interleaved_arguments_arrive() {
        for (var terminal : List.of("{\"choices\":[{\"index\":0,\"finish_reason\":\"tool_calls\",\"delta\":null}]}", "{\"choices\":[]}")) {
            var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
            var first = processor.processChunk("""
                data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"call-a","function":{"name":"first","arguments":"{\\"value\\":"}},
                  {"index":1,"id":"call-b","function":{"name":"second","arguments":"{\\"value\\":"}}
                ]}}]}
                """);
            var second = processor.processChunk("""
                data: {"choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":1,"function":{"arguments":"2}"}},{"index":0,"function":{"arguments":"1}"}}
                ]}}]}
                """);

            assertEquals(0, Stream.concat(first.events().stream(), second.events().stream()).filter(CompleteToolCallEvent.class::isInstance).count());
            var completed = processor.processChunk("data: " + terminal).events().stream().filter(CompleteToolCallEvent.class::isInstance)
                .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall()).toList();
            assertEquals(2, completed.size());
            assertEquals("{\"value\":1}", completed.get(0).function().arguments());
            assertEquals("{\"value\":2}", completed.get(1).function().arguments());
            assertEquals(0, processor.processChunk("data: " + terminal).events().stream().filter(CompleteToolCallEvent.class::isInstance).count());
        }
    }

    @Test
    void should_keep_legacy_tools_without_indices_separate_by_id() {
        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);
        processor.processChunk("""
            data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
              {"id":"call-a","function":{"name":"time","arguments":"{"}}
            ]}}]}
            """);
        processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"function":{"arguments":"}"}}]}}]}
            """);
        var nextTool = processor.processChunk("""
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"id":"call-b","function":{"name":"time","arguments":"{}"}}]}}]}
            """);
        var terminal = processor.processChunk("""
            data: {"choices":[{"index":0,"finish_reason":"tool_calls","delta":{}}]}
            """);
        var completed = Stream.concat(nextTool.events().stream(), terminal.events().stream()).filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast).map(event -> event.completeToolCall().toolCall()).toList();

        assertEquals(1, nextTool.events().stream().filter(CompleteToolCallEvent.class::isInstance).count());
        assertEquals(1, terminal.events().stream().filter(CompleteToolCallEvent.class::isInstance).count());
        assertEquals(List.of("call-a", "call-b"), completed.stream().map(call -> call.id()).toList());
        assertEquals(List.of("{}", "{}"), completed.stream().map(call -> call.function().arguments()).toList());
    }

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

        // choices[0], tool 1: its greater index completes the previous tool with both arguments joined.
        var chunk4 = "data: " + """
            {"id":"chatcmpl-5","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","tool_calls":[\
            {"index":1,"id":"call_b","type":"function","function":{"name":"diff","arguments":"{\\"a\\":3,\\"b\\":4}"}}]}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.003Z"}""";

        // Terminal chunk of choices[0]: completes only its last tool.
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

        // choices[0], tool 0: completed when the next greater tool index arrived, with both argument fragments joined.
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
    void should_accept_reasoning_as_alias_for_reasoning_content_on_delta() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var thinkingChunk = "data: " + """
            {"id":"chatcmpl-8","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b","model":"openai/gpt-oss-120b",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","reasoning":"The user is asking for"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.000Z"}""";

        var stopChunk = "data: " + """
            {"id":"chatcmpl-8","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b","model":"openai/gpt-oss-120b",\
            "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant","content":"Hi!"}}],\
            "created":1,"created_at":"2026-07-26T00:00:00.001Z"}""";

        processor.processChunk(thinkingChunk);
        processor.processChunk(stopChunk);

        var response = processor.buildResponse();
        assertEquals("The user is asking for", response.choices().get(0).message().reasoningContent());
        assertEquals("The user is asking for", response.toAssistantMessage().thinking());
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

        // The first tool completes at the next greater index and the last at the terminal chunk, each after its synthetic partial.
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

        // Tools complete at the terminal chunk in the order their first delta arrived.
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

    @Test
    void should_not_throw_npe_when_input_moderation_blocks_before_generation_and_delta_is_null() {

        var processor = new SseEventProcessor(List.of(), null, TextChatResponse::builder);

        var sentinelChunk = "data: "
            + """
                {"id":"089c113655af4032ba63b2676806197d","object":"chat.completion.chunk",\
                "model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
                "choices":[{"index":0,"finish_reason":null}],\
                "created":1786783200,"model_version":"4.0.0","created_at":"2026-08-15T08:40:00.656Z",\
                "usage":{"prompt_tokens":9},\
                "moderations":{"pii":[{"score":0.8,"input":true,"position":{"start":19,"end":29},"entity":"PhoneNumber","word":"3334523123"}]},\
                "detections":{"input":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"3334523123","start":19,"end":29}]}]}}""";

        var result = processor.processChunk(sentinelChunk);
        assertFalse(result.hasError());

        var response = processor.buildResponse();
        assertNotNull(response.moderations());
        assertTrue(response.moderations().containsKey("pii"));
        assertEquals(1, response.moderations().get("pii").size());
        assertEquals("PhoneNumber", response.moderations().get("pii").get(0).entity());
        assertEquals("3334523123", response.moderations().get("pii").get(0).word());
        assertTrue(response.moderations().get("pii").get(0).input());

        assertNotNull(response.detections());
        assertTrue(response.detections().containsKey("input"));
        assertEquals("en_syntax_rbr_pii", response.detections().get("input").get(0).results().get(0).detectorId());
    }

    private static List<CompletedToolCall> completedTools(ProcessResult result) {
        return result.events().stream().filter(CompleteToolCallEvent.class::isInstance)
            .map(CompleteToolCallEvent.class::cast).map(CompleteToolCallEvent::completeToolCall).toList();
    }
}
