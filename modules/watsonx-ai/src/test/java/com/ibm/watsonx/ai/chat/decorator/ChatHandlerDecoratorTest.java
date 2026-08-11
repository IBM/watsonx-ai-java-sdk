/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.decorator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;

public class ChatHandlerDecoratorTest {

    /**
     * Records the order in which the callbacks are delivered.
     */
    static class Recorder implements ChatHandler {

        // Not synchronized on purpose: the decorator must publish each callback to the next one.
        final List<String> events = new ArrayList<>();

        @Override
        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
            events.add("partialResponse");
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall) {
            events.add("partial(" + partialToolCall.toolIndex() + ")");
        }

        @Override
        public void onCompleteToolCall(CompletedToolCall completeToolCall) {
            events.add("complete(" + completeToolCall.toolCall().index() + ")");
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            events.add("completeResponse");
        }

        @Override
        public void onError(Throwable error) {
            events.add("error(" + error.getClass().getSimpleName() + ")");
        }
    }

    static PartialToolCall partial(int toolIndex, String name) {
        return new PartialToolCall("cmpl-1", 0, toolIndex, "call_" + toolIndex, name, "{\"country\": \"Italy\"}");
    }

    static CompletedToolCall complete(int toolIndex, String name) {
        return new CompletedToolCall("cmpl-1", 0, ToolCall.of(toolIndex, "call_" + toolIndex, name, "{\"country\": \"Italy\"}"));
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Emits the sequence produced by SseEventProcessor for a response carrying two sequential tool calls.
     */
    static void emitTwoToolCalls(ChatHandlerDecorator<ChatRequest> decorator) {
        decorator.onPartialResponse("Hello", null);
        decorator.onPartialToolCall(partial(0, "get_weather"));
        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onPartialToolCall(partial(1, "get_current_time"));
        decorator.onCompleteToolCall(complete(1, "get_current_time"));
        decorator.onCompleteResponse(null);
    }

    static final List<String> EXPECTED_ORDER =
        List.of("partialResponse", "partial(0)", "complete(0)", "partial(1)", "complete(1)", "completeResponse");

    @Test
    void should_deliver_every_callback_in_emission_order() {

        var recorder = new Recorder();
        var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, null);

        emitTwoToolCalls(decorator);
        decorator.awaitCallbacks().join();

        assertEquals(EXPECTED_ORDER, recorder.events);
    }

    @Test
    void should_deliver_every_callback_in_emission_order_with_a_slow_interceptor() {

        var recorder = new Recorder();
        ToolInterceptor<ChatRequest> slow = (ctx, functionCall) -> {
            sleep(200);
            return functionCall;
        };

        var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, slow);

        emitTwoToolCalls(decorator);
        decorator.awaitCallbacks().join();

        assertEquals(EXPECTED_ORDER, recorder.events);
    }

    @Test
    void should_never_run_two_callbacks_at_the_same_time() {

        var running = new AtomicInteger();
        var peak = new AtomicInteger();

        Runnable body = () -> {
            peak.accumulateAndGet(running.incrementAndGet(), Math::max);
            sleep(50);
            running.decrementAndGet();
        };

        var decorator = new ChatHandlerDecorator<ChatRequest>(new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                body.run();
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                body.run();
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                body.run();
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                body.run();
            }
        }, null, null);

        emitTwoToolCalls(decorator);
        decorator.awaitCallbacks().join();

        assertEquals(1, peak.get(), "two callbacks overlapped");
    }

    @Test
    void should_report_error_thrown_by_a_callback_and_keep_delivering() {

        var recorder = new Recorder();
        var decorator = new ChatHandlerDecorator<ChatRequest>(new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                recorder.onPartialResponse(partialResponse, partialChatResponse);
                throw new AssertionError("thrown by the handler");
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                recorder.onPartialToolCall(partialToolCall);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                recorder.onCompleteResponse(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                recorder.onError(error);
            }
        }, null, null);

        decorator.onPartialResponse("Hello", null);
        decorator.onPartialToolCall(partial(0, "get_weather"));
        decorator.onCompleteResponse(null);
        decorator.awaitCallbacks().join();

        assertEquals(List.of("partialResponse", "error(AssertionError)", "partial(0)", "completeResponse"), recorder.events);
    }

    @Test
    void should_keep_delivering_when_a_complete_tool_call_callback_throws() {

        var recorder = new Recorder();
        var decorator = new ChatHandlerDecorator<ChatRequest>(new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                recorder.onPartialResponse(partialResponse, partialChatResponse);
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                recorder.onCompleteToolCall(completeToolCall);
                throw new IllegalStateException("thrown by the handler");
            }

            @Override
            public void onError(Throwable error) {
                recorder.onError(error);
            }
        }, null, null);

        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onPartialResponse("Hello", null);

        // The failure is reported through onError only: it must not fail the future the streaming response is built on.
        var toolCalls = assertDoesNotThrow(() -> decorator.awaitCallbacks().join());

        assertEquals(List.of("complete(0)", "error(IllegalStateException)", "partialResponse"), recorder.events);
        assertEquals(List.of(0), toolCalls.stream().map(toolCall -> toolCall.toolCall().index()).toList());
    }

    @Test
    void should_report_interceptor_failure_and_deliver_the_un_normalized_tool_call() {

        var recorder = new Recorder();
        ToolInterceptor<ChatRequest> failing = (ctx, functionCall) -> {
            throw new IllegalStateException("thrown by the interceptor");
        };

        var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, failing);

        decorator.onPartialToolCall(partial(0, "get_weather"));
        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onPartialResponse("Hello", null);

        var toolCalls = assertDoesNotThrow(() -> decorator.awaitCallbacks().join());

        assertEquals(List.of("partial(0)", "error(IllegalStateException)", "complete(0)", "partialResponse"), recorder.events);
        assertEquals(1, toolCalls.size());
        assertEquals("{\"country\": \"Italy\"}", toolCalls.get(0).toolCall().function().arguments());
    }

    @Test
    void should_return_tool_calls_in_receive_order() {

        var decorator = new ChatHandlerDecorator<ChatRequest>(new Recorder(), null, null);

        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onCompleteToolCall(complete(1, "get_current_time"));

        var toolCalls = decorator.awaitCallbacks().join();
        assertEquals(List.of(0, 1), toolCalls.stream().map(toolCall -> toolCall.toolCall().index()).toList());
    }
}
