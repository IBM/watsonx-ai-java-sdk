/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.net.URI;
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
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;

@ExtendWith(MockitoExtension.class)
@Isolated("Asserts the absence of callbacks within timing windows; must run without concurrent CPU contention.")
public class DeploymentStreamingCancellationTest extends AbstractWatsonxTest {

    static final String DEPLOYMENT_ID = "deployment-id";

    /**
     * Counts every callback and cancels the future once a given number of partial responses has been delivered.
     */
    static final class ChatRecorder implements ChatHandler {

        final CompletableFuture<CompletableFuture<ChatResponse>> future = new CompletableFuture<>();
        final AtomicInteger partialResponses = new AtomicInteger();
        final AtomicInteger completeResponses = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch cancelled = new CountDownLatch(1);

        private final int cancelAfterPartialResponses;

        ChatRecorder(int cancelAfterPartialResponses) {
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

    /**
     * Counts every callback and cancels the future once a given number of partial responses has been delivered.
     */
    static final class TextGenerationRecorder implements TextGenerationHandler {

        final CompletableFuture<CompletableFuture<Void>> future = new CompletableFuture<>();
        final AtomicInteger partialResponses = new AtomicInteger();
        final AtomicInteger completeResponses = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch cancelled = new CountDownLatch(1);

        private final int cancelAfterPartialResponses;

        TextGenerationRecorder(int cancelAfterPartialResponses) {
            this.cancelAfterPartialResponses = cancelAfterPartialResponses;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            if (partialResponses.incrementAndGet() == cancelAfterPartialResponses) {
                future.join().cancel(true);
                cancelled.countDown();
            }
        }

        @Override
        public void onCompleteResponse(TextGenerationResponse completeResponse) {
            completeResponses.incrementAndGet();
        }

        @Override
        public void onError(Throwable error) {
            errors.incrementAndGet();
        }
    }

    static String chatBody(int deltas) {
        var body = new StringBuilder();

        for (int i = 1; i <= deltas; i++)
            body.append(
                """
                    id: %d
                    event: message
                    data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":null,"delta":{"content":"chunk%d"}}],"created":1749736055,"created_at":"2025-06-12T13:47:35.542Z"}

                    """
                    .formatted(i, i));

        body.append(
            """
                id: %d
                event: message
                data: {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"m","model":"m","choices":[{"index":0,"finish_reason":"stop","delta":{"content":""}}],"created":1749736055,"created_at":"2025-06-12T13:47:35.563Z"}

                """
                .formatted(deltas + 1));

        return body.toString();
    }

    static String generationBody(int deltas) {
        var body = new StringBuilder();

        for (int i = 1; i <= deltas; i++)
            body.append(
                """
                    id: %d
                    event: message
                    data: {"model_id":"m","created_at":"2025-06-24T15:30:13.552Z","results":[{"generated_text":"chunk%d","generated_token_count":%d,"input_token_count":0,"stop_reason":"not_finished"}]}

                    """
                    .formatted(i, i, i));

        body.append(
            """
                id: %d
                event: message
                data: {"model_id":"m","created_at":"2025-06-24T15:30:13.588Z","results":[{"generated_text":".","generated_token_count":%d,"input_token_count":0,"stop_reason":"eos_token"}]}

                """
                .formatted(deltas + 1, deltas + 1));

        return body.toString();
    }

    DeploymentService service() {
        return DeploymentService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();
    }

    @Test
    void should_stop_delivering_chat_callbacks_after_cancel() throws Exception {

        wireMock.stubFor(post("/ml/v1/deployments/%s/text/chat_stream?version=%s".formatted(DEPLOYMENT_ID, API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withChunkedDribbleDelay(40, 4000)
                .withBody(chatBody(12))));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));

        var recorder = new ChatRecorder(2);
        var request = DeploymentChatRequest.builder()
            .deploymentId(DEPLOYMENT_ID)
            .messages(UserMessage.text("Tell me a long story"))
            .build();

        var future = service().chatStreaming(request, recorder);
        recorder.future.complete(future);

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
    void should_deliver_no_chat_callback_when_cancelled_before_the_first_chunk() throws Exception {

        wireMock.stubFor(post("/ml/v1/deployments/%s/text/chat_stream?version=%s".formatted(DEPLOYMENT_ID, API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withFixedDelay(800)
                .withBody(chatBody(12))));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));

        var recorder = new ChatRecorder(0);
        var request = DeploymentChatRequest.builder()
            .deploymentId(DEPLOYMENT_ID)
            .messages(UserMessage.text("Tell me a long story"))
            .build();

        var future = service().chatStreaming(request, recorder);
        recorder.future.complete(future);

        future.cancel(true);
        Thread.sleep(2000);

        assertEquals(0, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
    }

    @Test
    void should_stop_delivering_text_generation_callbacks_after_cancel() throws Exception {

        wireMock.stubFor(post("/ml/v1/deployments/%s/text/generation_stream?version=%s".formatted(DEPLOYMENT_ID, API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withChunkedDribbleDelay(40, 4000)
                .withBody(generationBody(12))));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));

        var recorder = new TextGenerationRecorder(2);
        var request = TextGenerationRequest.builder()
            .deploymentId(DEPLOYMENT_ID)
            .input("Tell me a long story")
            .build();

        var future = service().generateStreaming(request, recorder);
        recorder.future.complete(future);

        assertTrue(recorder.cancelled.await(10, TimeUnit.SECONDS), "the second partial response was never delivered");

        Thread.sleep(1000);

        assertEquals(2, recorder.partialResponses.get());
        assertEquals(0, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, future::join);
    }
}
