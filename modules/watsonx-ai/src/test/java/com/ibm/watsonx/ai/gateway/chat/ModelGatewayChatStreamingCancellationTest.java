/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.UserMessage;

@ExtendWith(MockitoExtension.class)
@Isolated("Asserts the absence of callbacks within timing windows; must run without concurrent CPU contention.")
public class ModelGatewayChatStreamingCancellationTest extends AbstractWatsonxTest {

    /**
     * Counts every callback and cancels the future once a given number of partial responses has been delivered.
     */
    static final class Recorder implements ChatHandler {

        final CompletableFuture<CompletableFuture<ChatResponse>> future = new CompletableFuture<>();
        final AtomicInteger partialResponses = new AtomicInteger();
        final AtomicInteger completeResponses = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch cancelled = new CountDownLatch(1);

        private final int cancelAfterPartialResponses;

        Recorder(int cancelAfterPartialResponses) {
            this.cancelAfterPartialResponses = cancelAfterPartialResponses;
        }

        @Override
        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
            if (partialResponses.incrementAndGet() == cancelAfterPartialResponses) {
                future.join().cancel(true);
                cancelled.countDown();
            }
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            completeResponses.incrementAndGet();
        }

        @Override
        public void onError(Throwable error) {
            errors.incrementAndGet();
        }
    }

    static String body(int deltas) {
        var body = new StringBuilder();

        for (int i = 1; i <= deltas; i++)
            body.append(
                """
                    data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"chunk%d"},"finish_reason":"","logprobs":null}],"created":1749736055,"model":"gpt-4o","usage":null,"cached":false}

                    """
                    .formatted(i));

        body.append(
            """
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"stop","logprobs":null}],"created":1749736055,"model":"gpt-4o","usage":{"prompt_tokens":38,"completion_tokens":3,"total_tokens":41},"cached":false}

                data: [DONE]
                """);

        return body.toString();
    }

    ModelGatewayChatService service() {
        return ModelGatewayChatService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();
    }

    CompletableFuture<ChatResponse> start(Recorder recorder) {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));
        var future = service().chatStreaming(List.<ChatMessage>of(UserMessage.text("Tell me a long story")), recorder);
        recorder.future.complete(future);
        return future;
    }

    @Test
    void should_stop_delivering_callbacks_after_cancel() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withChunkedDribbleDelay(40, 4000)
                .withBody(body(12))));

        var recorder = new Recorder(2);
        var future = start(recorder);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        // Well inside the remaining dribble window: further chunks would still be arriving if the stream had not been stopped.
        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, future::join);
    }

    @Test
    void should_deliver_no_callback_when_cancelled_before_the_first_chunk() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withFixedDelay(800)
                .withBody(body(12))));

        var recorder = new Recorder(0);
        var future = start(recorder);

        future.cancel(true);
        Thread.sleep(2000);

        assertEquals(0, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }
}
