/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.streaming.DefaultChatSubscriber;

@ExtendWith(MockitoExtension.class)
@Isolated("Asserts the absence of callbacks within timing windows; must run without concurrent CPU contention.")
public class ChatStreamingCancellationTest extends AbstractWatsonxTest {

    /**
     * Counts every callback and cancels the future once a given number of partial responses has been delivered.
     */
    static final class Recorder implements ChatHandler {

        final CompletableFuture<CompletableFuture<ChatResponse>> future = new CompletableFuture<>();
        final AtomicInteger partialResponses = new AtomicInteger();
        final AtomicInteger partialToolCalls = new AtomicInteger();
        final AtomicInteger completeToolCalls = new AtomicInteger();
        final AtomicInteger completeResponses = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch cancelled = new CountDownLatch(1);

        private final int cancelAfterPartialResponses;
        private final int cancelAfterPartialToolCalls;
        private final boolean mayInterruptIfRunning;
        private final boolean failOnFirstError;

        Recorder(int cancelAfterPartialResponses) {
            this(cancelAfterPartialResponses, 0, true, false);
        }

        Recorder(int cancelAfterPartialResponses, int cancelAfterPartialToolCalls, boolean mayInterruptIfRunning, boolean failOnFirstError) {
            this.cancelAfterPartialResponses = cancelAfterPartialResponses;
            this.cancelAfterPartialToolCalls = cancelAfterPartialToolCalls;
            this.mayInterruptIfRunning = mayInterruptIfRunning;
            this.failOnFirstError = failOnFirstError;
        }

        @Override
        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
            if (partialResponses.incrementAndGet() == cancelAfterPartialResponses)
                cancelNow();
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall) {
            if (partialToolCalls.incrementAndGet() == cancelAfterPartialToolCalls)
                cancelNow();
        }

        @Override
        public void onCompleteToolCall(CompletedToolCall completeToolCall) {
            completeToolCalls.incrementAndGet();
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            completeResponses.incrementAndGet();
        }

        @Override
        public void onError(Throwable error) {
            errors.incrementAndGet();
        }

        @Override
        public boolean failOnFirstError() {
            return failOnFirstError;
        }

        /**
         * Cancels from inside the callback, which is the pattern a caller uses to stop as soon as a condition is met.
         */
        private void cancelNow() {
            future.join().cancel(mayInterruptIfRunning);
            cancelled.countDown();
        }
    }

    /**
     * Records the interactions of the body subscription so that the cancellation mechanism itself can be asserted.
     */
    static final class RecordingSubscription implements Flow.Subscription {

        final AtomicInteger requests = new AtomicInteger();
        final AtomicInteger cancellations = new AtomicInteger();

        @Override
        public void request(long n) {
            requests.incrementAndGet();
        }

        @Override
        public void cancel() {
            cancellations.incrementAndGet();
        }
    }

    static String contentEvent(int id, String content) {
        return """
            id: %d
            event: message
            data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":null,"delta":{"content":"%s"}}],"created":1749736055,"created_at":"2025-06-12T13:47:35.542Z"}

            """
            .formatted(id, content);
    }

    /**
     * Builds an SSE body carrying the given number of content deltas, followed by the stop and usage events.
     */
    static String contentBody(int deltas) {
        var body = new StringBuilder();

        for (int i = 1; i <= deltas; i++)
            body.append(contentEvent(i, "chunk" + i));

        body.append(
            """
                id: %d
                event: message
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":"stop","delta":{"content":""}}],"created":1749736055,"created_at":"2025-06-12T13:47:35.563Z"}

                id: %d
                event: message
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[],"created":1749736055,"created_at":"2025-06-12T13:47:35.564Z","usage":{"completion_tokens":3,"prompt_tokens":38,"total_tokens":41}}

                """
                .formatted(deltas + 1, deltas + 2));

        return body.toString();
    }

    /**
     * Builds an SSE body that streams the fragments of a single tool call, so that a cancellation can land while it is being assembled.
     */
    static String toolCallBody(int fragments) {
        var body = new StringBuilder();

        body.append(
            """
                id: 1
                event: message
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"id":"call-1","type":"function","function":{"name":"sum","arguments":""}}]}}],"created":1749764735,"created_at":"2025-06-12T21:45:35.348Z"}

                """);

        for (int i = 1; i <= fragments; i++)
            body.append(
                """
                    id: %d
                    event: message
                    data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"function":{"name":"","arguments":"a"}}]}}],"created":1749764735,"created_at":"2025-06-12T21:45:35.357Z"}

                    """
                    .formatted(i + 1));

        body.append(
            """
                id: %d
                event: message
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":"tool_calls","delta":{"content":""}}],"created":1749764735,"created_at":"2025-06-12T21:45:35.555Z"}

                """
                .formatted(fragments + 2));

        return body.toString();
    }

    void stubChatStream(String body, int chunks, int totalMillis) {
        wireMock.stubFor(post(urlMatching("/ml/v1/text/chat_stream.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withChunkedDribbleDelay(chunks, totalMillis)
                .withBody(body)));
    }

    ChatService chatService(boolean logResponses) {
        return ChatService.builder()
            .authenticator(mockAuthenticator)
            .modelId("m")
            .projectId("project-id")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .logResponses(logResponses)
            .build();
    }

    List<ChatMessage> messages() {
        return List.of(UserMessage.text("Tell me a long story"));
    }

    /**
     * Starts a stream through the public service API and returns the future the caller would cancel.
     */
    CompletableFuture<ChatResponse> start(Recorder recorder, boolean logResponses) {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));
        var future = chatService(logResponses).chatStreaming(ChatRequest.builder().messages(messages()).build(), recorder);
        recorder.future.complete(future);
        return future;
    }

    // Test 1, 5 and 11: cancellation through the service API, triggered from inside a callback.
    @Test
    void should_stop_delivering_callbacks_after_cancel() throws Exception {

        stubChatStream(contentBody(12), 40, 4000);

        var recorder = new Recorder(2);
        var future = start(recorder, false);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        // Well inside the remaining dribble window: further chunks would still be arriving if the stream had not been stopped.
        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, future::join);
    }

    // Test 2: cancelling before the first chunk reaches the subscriber.
    @Test
    void should_deliver_no_callback_when_cancelled_before_the_first_chunk() throws Exception {

        wireMock.stubFor(post(urlMatching("/ml/v1/text/chat_stream.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withFixedDelay(800)
                .withBody(contentBody(12))));

        var recorder = new Recorder(0);
        var future = start(recorder, false);

        future.cancel(true);
        Thread.sleep(2000);

        assertEquals(0, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    // Test 3: cancelling after normal completion changes nothing.
    @Test
    void should_be_a_no_op_when_cancelled_after_completion() throws Exception {

        stubChatStream(contentBody(3), 4, 100);

        var recorder = new Recorder(0);
        var future = start(recorder, false);

        var response = future.get(10, TimeUnit.SECONDS);

        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
        assertTrue(future.isDone() && !future.isCompletedExceptionally());
        assertNotNull(response);
        assertEquals("chunk1chunk2chunk3", response.toAssistantMessage().content());
        assertEquals(1, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
    }

    // Test 4: two threads cancelling concurrently.
    @Test
    void should_take_effect_once_when_two_threads_cancel() throws Exception {

        stubChatStream(contentBody(12), 40, 4000);

        var recorder = new Recorder(0);
        var future = start(recorder, false);

        var start = new CountDownLatch(1);
        var outcomes = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = CompletableFuture.runAsync(() -> {
                awaitQuietly(start);
                if (future.cancel(true))
                    outcomes.incrementAndGet();
            }, executor);

            var second = CompletableFuture.runAsync(() -> {
                awaitQuietly(start);
                if (future.cancel(false))
                    outcomes.incrementAndGet();
            }, executor);

            start.countDown();
            CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // cancel reports the state, not the transition: once the future is cancelled every caller gets true.
        assertEquals(2, outcomes.get());
        assertTrue(future.isCancelled());

        // Let a callback that was already running finish, then check the stream stays stopped for the rest of the window.
        Thread.sleep(500);
        var delivered = recorder.partialResponses.get();
        Thread.sleep(1000);

        assertEquals(delivered, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
    }

    // Test 6: cancelling while a tool call is being assembled.
    @Test
    void should_not_deliver_the_complete_tool_call_when_cancelled_while_assembling() throws Exception {

        stubChatStream(toolCallBody(12), 40, 4000);

        var recorder = new Recorder(0, 2, true, false);
        var future = start(recorder, false);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial tool call was never delivered");

        Thread.sleep(1000);

        assertEquals(2, recorder.partialToolCalls.get());
        assertEquals(0, recorder.completeToolCalls.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    // Test 7: failOnFirstError does not change the cancellation behaviour.
    @Test
    void should_cancel_the_same_way_when_fail_on_first_error_is_enabled() throws Exception {

        stubChatStream(contentBody(12), 40, 4000);

        var recorder = new Recorder(2, 0, true, true);
        var future = start(recorder, false);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    // Test 8: the SseEventLogger path behaves the same. Log output is not part of the contract, only handler behaviour.
    @Test
    void should_cancel_the_same_way_when_log_responses_is_enabled() throws Exception {

        stubChatStream(contentBody(12), 40, 4000);

        var recorder = new Recorder(2);
        var future = start(recorder, true);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    // Test 12: cancel(false) is equivalent to cancel(true), because CompletableFuture ignores the interrupt flag.
    @Test
    void should_treat_cancel_false_like_cancel_true() throws Exception {

        stubChatStream(contentBody(12), 40, 4000);

        var recorder = new Recorder(2, 0, false, false);
        var future = start(recorder, false);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    // Test 9: the body subscription is really cancelled, and no further element is requested.
    @Test
    void should_cancel_the_body_subscription() {

        var recorder = new Recorder(0);
        var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, null);
        var subscriber = new DefaultChatSubscriber(new SseEventProcessor(null, null, TextChatResponse::builder), decorator);
        var response = cancellableResponse(subscriber);
        var flowSubscriber = subscriber.asFlowSubscriber(response, true);
        var subscription = new RecordingSubscription();

        flowSubscriber.onSubscribe(subscription);
        flowSubscriber.onNext(dataLine("chunk1"));

        assertEquals(2, subscription.requests.get());
        assertEquals(0, subscription.cancellations.get());

        response.cancel(true);

        assertTrue(subscription.cancellations.get() > 0, "the subscription was not cancelled");
        assertTrue(subscriber.isCancelled());

        var requestsAtCancel = subscription.requests.get();
        flowSubscriber.onNext(dataLine("chunk2"));

        assertEquals(requestsAtCancel, subscription.requests.get());
    }

    // Test 10: the signals the JDK is still allowed to deliver after cancel are dropped.
    @Test
    void should_drop_the_signals_delivered_after_cancel() {

        var recorder = new Recorder(0);
        var decorator = new ChatHandlerDecorator<ChatRequest>(recorder, null, null);
        var subscriber = new DefaultChatSubscriber(new SseEventProcessor(null, null, TextChatResponse::builder), decorator);
        var response = cancellableResponse(subscriber);
        var flowSubscriber = subscriber.asFlowSubscriber(response, true);

        flowSubscriber.onSubscribe(new RecordingSubscription());
        flowSubscriber.onNext(dataLine("chunk1"));

        decorator.awaitCallbacks().join();
        assertEquals(1, recorder.partialResponses.get());

        response.cancel(true);

        flowSubscriber.onNext(dataLine("chunk2"));
        flowSubscriber.onNext(dataLine("chunk3"));
        flowSubscriber.onError(new RuntimeException("boom"));
        flowSubscriber.onComplete();

        decorator.awaitCallbacks().join();

        assertEquals(1, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(response.isCancelled());
    }

    /**
     * Wires the cancellation hook the routes install, so that the subscriber can be driven by hand.
     */
    static CompletableFuture<ChatResponse> cancellableResponse(DefaultChatSubscriber subscriber) {
        var response = new CompletableFuture<ChatResponse>();

        response.whenComplete((r, t) -> {
            if (response.isCancelled())
                subscriber.cancelStream();
        });

        return response;
    }

    static String dataLine(String content) {
        return "data: {\"id\":\"chatcmpl-1\",\"object\":\"chat.completion.chunk\",\"model_id\":\"m\",\"model\":\"m\","
            + "\"choices\":[{\"index\":0,\"finish_reason\":null,\"delta\":{\"content\":\"%s\"}}],\"created\":1749736055,"
            + "\"created_at\":\"2025-06-12T13:47:35.542Z\"}".formatted(content);
    }

    static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
