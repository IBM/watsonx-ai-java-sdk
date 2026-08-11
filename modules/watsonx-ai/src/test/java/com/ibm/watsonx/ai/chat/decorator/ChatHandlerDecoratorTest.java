/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.decorator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;

public class ChatHandlerDecoratorTest {

    /**
     * Records the order in which the callbacks are delivered.
     */
    static class Recorder implements ChatHandler {

        final List<String> events = Collections.synchronizedList(new ArrayList<>());

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

    static boolean awaitQuietly(CountDownLatch latch) {
        try {
            return latch.await(5, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    void should_deliver_complete_tool_call_after_its_partial_fragments() {

        // Repeated because the callbacks run on the callback executor: a single run would not tell a fix from a lucky interleaving.
        for (int i = 0; i < 50; i++) {

            var recorder = new Recorder();
            var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, null);

            // The order in which SseEventProcessor emits two sequential tool calls.
            decorator.onPartialToolCall(partial(0, "get_weather"));
            decorator.onCompleteToolCall(complete(0, "get_weather"));
            decorator.onPartialToolCall(partial(1, "get_current_time"));
            decorator.onCompleteToolCall(complete(1, "get_current_time"));
            decorator.awaitCallbacks().join();

            var events = recorder.events;
            assertEquals(4, events.size(), events::toString);
            assertTrue(events.indexOf("partial(0)") < events.indexOf("complete(0)"), events::toString);
            assertTrue(events.indexOf("partial(1)") < events.indexOf("complete(1)"), events::toString);
        }
    }

    @Test
    void should_run_complete_tool_call_callbacks_in_parallel() {

        var completed = Collections.synchronizedList(new ArrayList<Integer>());
        var firstToolCallStarted = new CountDownLatch(1);
        var secondToolCallReturned = new CountDownLatch(1);
        var ranInParallel = new AtomicBoolean();

        var decorator = new ChatHandlerDecorator<ChatRequest>(new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {

                if (completeToolCall.toolCall().index() == 0) {
                    firstToolCallStarted.countDown();
                    // The second tool call can only return while the first one is still running if the two are delivered in parallel.
                    ranInParallel.set(awaitQuietly(secondToolCallReturned));
                    completed.add(0);
                } else {
                    awaitQuietly(firstToolCallStarted);
                    completed.add(1);
                    secondToolCallReturned.countDown();
                }
            }
        }, null, null);

        decorator.onPartialToolCall(partial(0, "get_weather"));
        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onPartialToolCall(partial(1, "get_current_time"));
        decorator.onCompleteToolCall(complete(1, "get_current_time"));
        decorator.awaitCallbacks().join();

        assertTrue(ranInParallel.get(), "the two onCompleteToolCall invocations did not overlap");
        assertEquals(List.of(1, 0), completed);
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
    void should_return_tool_calls_in_receive_order() {

        var decorator = new ChatHandlerDecorator<ChatRequest>(new Recorder(), null, null);

        decorator.onCompleteToolCall(complete(0, "get_weather"));
        decorator.onCompleteToolCall(complete(1, "get_current_time"));

        var toolCalls = decorator.awaitCallbacks().join();
        assertEquals(List.of(0, 1), toolCalls.stream().map(toolCall -> toolCall.toolCall().index()).toList());
    }
}
