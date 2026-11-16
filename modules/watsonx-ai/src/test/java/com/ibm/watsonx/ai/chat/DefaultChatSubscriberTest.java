/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.streaming.DefaultChatSubscriber;
import com.ibm.watsonx.ai.core.Json;

class DefaultChatSubscriberTest {

    @Test
    void should_preserve_pending_tools_alongside_intercepted_early_completions() throws Exception {
        for (var finishReason : List.of("length", "error", "")) {
            var events = new ArrayList<String>();
            var interceptions = new AtomicInteger();
            var completed = new AtomicReference<ChatResponse>();
            var request = ChatRequest.builder().messages(UserMessage.text("Plan two trips")).build();
            var handler = new ChatHandlerDecorator<>(new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onPartialToolCall(PartialToolCall toolCall) {
                    events.add("partial:" + toolCall.toolIndex());
                }

                @Override
                public void onCompleteToolCall(CompletedToolCall toolCall) {
                    events.add("tool:" + toolCall.toolCall().index());
                    assertEquals("{\"city\":\"Rome\"}", toolCall.toolCall().function().arguments());
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    completed.set(response);
                    events.add("response");
                }
            }, new InterceptorContext<>(null, request, null), null, null, (context, function) -> {
                interceptions.incrementAndGet();
                return function.withArguments("{\"city\":\"Rome\"}");
            });
            var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
            subscriber.onNext("""
                data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":2,"id":"call-1","function":{"name":"plan_trip","arguments":"{}"}},
                  {"index":7,"id":"call-2","function":{"name":"plan_trip","arguments":"{"}}]}}]}
                """);

            // The first tool is available while generation of the second tool is still in progress.
            handler.awaitCallbacks().get(5, TimeUnit.SECONDS);
            assertEquals(List.of("partial:2", "tool:2", "partial:7"), events, finishReason);
            if (!finishReason.isEmpty())
                subscriber.onNext("data: {\"choices\":[{\"index\":0,\"finish_reason\":\"" + finishReason + "\",\"delta\":{}}]}");
            subscriber.onNext("data: [DONE]");

            var response = subscriber.onComplete().get(5, TimeUnit.SECONDS);

            assertEquals(List.of("partial:2", "tool:2", "partial:7", "response"), events, finishReason);
            assertEquals(1, interceptions.get(), finishReason);
            assertSame(response, completed.get(), finishReason);
            assertEquals(finishReason.isEmpty() ? null : finishReason, response.choices().get(0).finishReason(), finishReason);
            var tools = response.choices().get(0).message().toolCalls();
            assertEquals(List.of(2, 7), tools.stream().map(tool -> tool.index()).toList(), finishReason);
            assertEquals("{\"city\":\"Rome\"}", tools.get(0).function().arguments(), finishReason);
            assertEquals("{", tools.get(1).function().arguments(), finishReason);
        }
    }

    @Test
    void should_not_complete_partial_tools_after_a_recoverable_stream_error() throws Exception {
        for (var errorChunks : List.of(List.of("event: error", "data: generation failed"), List.of("data: {"))) {
            for (var endSignal : List.of("", "data: [DONE]", "data: {\"choices\":[],\"usage\":{\"total_tokens\":1}}")) {
                var errors = new AtomicInteger();
                var completedTools = new AtomicInteger();
                var completedResponses = new AtomicInteger();
                var request = ChatRequest.builder().messages(UserMessage.text("Charge the card")).build();
                var chatHandler = new ChatHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                    @Override
                    public void onError(Throwable error) {
                        errors.incrementAndGet();
                    }

                    @Override
                    public void onCompleteToolCall(CompletedToolCall toolCall) {
                        completedTools.incrementAndGet();
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        completedResponses.incrementAndGet();
                    }
                };
                assertFalse(chatHandler.failOnFirstError());
                var handler = new ChatHandlerDecorator<>(chatHandler, new InterceptorContext<>(null, request, null), null, null, null);
                var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
                var future = new CompletableFuture<ChatResponse>();
                var stream = subscriber.asFlowSubscriber(future, !chatHandler.failOnFirstError());
                stream.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {}

                    @Override
                    public void cancel() {}
                });
                stream.onNext("""
                    data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
                      {"index":0,"id":"call-1","function":{"name":"charge","arguments":"{"}}]}}]}
                    """);
                errorChunks.forEach(stream::onNext);
                if (!endSignal.isEmpty())
                    stream.onNext(endSignal);
                stream.onComplete();

                var response = future.get(5, TimeUnit.SECONDS);

                assertEquals(1, errors.get());
                assertEquals(0, completedTools.get());
                assertEquals(1, completedResponses.get());
                assertEquals(FinishReason.ERROR, response.finishReason());
                assertEquals("{", response.choices().get(0).message().toolCalls().get(0).function().arguments());
            }
        }
    }

    @Test
    void should_not_deliver_completion_callbacks_for_aborted_tools_when_the_stream_ends() throws Exception {
        for (var finishReason : List.of("length", "time_limit", "cancelled", "error", "content_filter")) {
            for (var terminalDelta : List.of("{}", "null")) {
                for (var argumentsField : List.of(",\"arguments\":\"{\"", ",\"arguments\":\"\"", "")) {
                    var completedTools = new AtomicInteger();
                    var partialArguments = new ArrayList<String>();
                    var request = ChatRequest.builder().messages(UserMessage.text("Charge the card")).build();
                    var handler = new ChatHandlerDecorator<>(new ChatHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                        @Override
                        public void onPartialToolCall(PartialToolCall toolCall) {
                            partialArguments.add(toolCall.arguments());
                        }

                        @Override
                        public void onCompleteToolCall(CompletedToolCall toolCall) {
                            completedTools.incrementAndGet();
                        }
                    }, new InterceptorContext<>(null, request, null), null, null, null);
                    var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
                    subscriber.onNext("""
                        data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
                          {"index":0,"id":"call-1","function":{"name":"charge"%s}}]}}]}
                        """.formatted(argumentsField));
                    subscriber.onNext("""
                        data: {"choices":[{"index":0,"finish_reason":"%s","delta":%s}]}
                        """.formatted(finishReason, terminalDelta));

                    var response = subscriber.onComplete().get(5, TimeUnit.SECONDS);

                    var scenario = "finish reason=" + finishReason + ", delta=" + terminalDelta + ", arguments=" + argumentsField;
                    assertEquals(0, completedTools.get(), scenario);
                    assertEquals(argumentsField.contains("{") ? List.of("{") : List.of(), partialArguments, scenario);
                    assertEquals(finishReason, response.choices().get(0).finishReason(), scenario);
                }
            }
        }
    }

    @Test
    void should_keep_truncated_tool_arguments_incomplete_without_delivering_tool_callbacks() throws Exception {
        for (var endSignal : List.of("", "data: [DONE]", "data: {\"choices\":[],\"usage\":{\"total_tokens\":1}}")) {
            var events = new ArrayList<String>();
            var interceptions = new AtomicInteger();
            var completed = new AtomicReference<ChatResponse>();
            var request = ChatRequest.builder().messages(UserMessage.text("Charge the card")).build();
            var handler = new ChatHandlerDecorator<>(new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onPartialToolCall(PartialToolCall toolCall) {
                    events.add("partial:" + toolCall.arguments());
                }

                @Override
                public void onCompleteToolCall(CompletedToolCall toolCall) {
                    events.add("tool");
                }

                @Override
                public void onError(Throwable error) {
                    events.add("error");
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    completed.set(response);
                    events.add("response");
                }
            }, new InterceptorContext<>(null, request, null), null, null, (context, function) -> {
                interceptions.incrementAndGet();
                return function;
            });
            var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
            subscriber.onNext("""
                data: {"id":"chat-1","choices":[{"index":0,"delta":{"tool_calls":[
                  {"index":0,"id":"call-1","function":{"name":"charge","arguments":"{"}}]}}]}
                """);
            if (!endSignal.isEmpty())
                subscriber.onNext(endSignal);

            var response = subscriber.onComplete().get(5, TimeUnit.SECONDS);

            assertEquals(List.of("partial:{", "response"), events, endSignal);
            assertEquals(0, interceptions.get(), endSignal);
            assertSame(response, completed.get(), endSignal);
            assertEquals(FinishReason.INCOMPLETE, response.finishReason(), endSignal);
            assertEquals("{", response.choices().get(0).message().toolCalls().get(0).function().arguments(), endSignal);
        }
    }

    @Test
    void should_complete_and_intercept_pending_tools_when_stream_ends_without_a_finish_reason() throws Exception {
        for (var arguments : List.of("{}", Json.toJson("{\"timezone\":\"UTC\"}"))) {
            for (var endSignal : List.of("", "data: [DONE]")) {
                var events = new ArrayList<String>();
                var request = ChatRequest.builder().messages(UserMessage.text("What time is it?")).build();
                var handler = new ChatHandlerDecorator<>(new ChatHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                    @Override
                    public void onCompleteToolCall(CompletedToolCall toolCall) {
                        events.add("tool:" + toolCall.toolCall().function().arguments());
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        events.add("response");
                    }
                }, new InterceptorContext<>(null, request, null), null, null,
                    (context, function) -> function.withArguments("{\"timezone\":\"UTC\"}"));
                var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
                subscriber.onNext("""
                    data: {"id":"chat-1","choices":[{"index":0,"delta":{"role":"assistant","tool_calls":[
                      {"index":0,"id":"call-1","type":"function","function":{"name":"time","arguments":%s}}]}}]}
                    """.formatted(Json.toJson(arguments)));
                if (!endSignal.isEmpty())
                    subscriber.onNext(endSignal);

                var response = subscriber.onComplete().get(5, TimeUnit.SECONDS);

                assertEquals(List.of("tool:{\"timezone\":\"UTC\"}", "response"), events);
                assertEquals("{\"timezone\":\"UTC\"}", response.choices().get(0).message().toolCalls().get(0).function().arguments());
            }
        }
    }

    @Test
    void should_return_the_same_intercepted_tool_calls_in_the_future_and_callback() throws Exception {
        var completed = new AtomicReference<ChatResponse>();
        var request = ChatRequest.builder().messages(UserMessage.text("What time is it?")).build();
        var handler = new ChatHandlerDecorator<>(new ChatHandler() {
            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

            @Override
            public void onCompleteResponse(ChatResponse response) {
                completed.set(response);
            }
        }, new InterceptorContext<>(null, request, null), null, null,
            (context, function) -> function.withArguments("{\"timezone\":\"UTC\"}"));
        var subscriber = new DefaultChatSubscriber(new SseEventProcessor(List.of(), null, TextChatResponse::builder), handler);
        subscriber.onNext("""
            data: {"id":"chat-1","choices":[{"index":0,"delta":{"role":"assistant","tool_calls":[{"index":0,
              "id":"call-1","type":"function","function":{"name":"time","arguments":"{}"}}]},"finish_reason":"tool_calls"},
              {"index":1,"delta":{"role":"assistant","content":"No tool needed"},"finish_reason":"stop"}]}
            """);

        var response = subscriber.onComplete().get(5, TimeUnit.SECONDS);

        assertSame(response, completed.get());
        assertEquals("{\"timezone\":\"UTC\"}", response.choices().get(0).message().toolCalls().get(0).function().arguments());
        assertNull(response.choices().get(1).message().toolCalls());
    }
}
