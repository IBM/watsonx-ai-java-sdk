/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;

@ExtendWith(MockitoExtension.class)
@Isolated("Asserts the absence of callbacks within timing windows; must run without concurrent CPU contention.")
public class TextGenerationStreamingCancellationTest extends AbstractWatsonxTest {

    /**
     * Counts every callback and cancels the future once a given number of partial responses has been delivered.
     */
    static final class Recorder implements TextGenerationHandler {

        final CompletableFuture<CompletableFuture<Void>> future = new CompletableFuture<>();
        final AtomicInteger partialResponses = new AtomicInteger();
        final AtomicInteger completeResponses = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch cancelled = new CountDownLatch(1);
        final CountDownLatch terminated = new CountDownLatch(1);

        private final int cancelAfterPartialResponses;

        Recorder(int cancelAfterPartialResponses) {
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
            terminated.countDown();
        }

        @Override
        public void onError(Throwable error) {
            errors.incrementAndGet();
            terminated.countDown();
        }
    }

    static String body(int deltas) {
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

    void stubGenerationStream(String body, int chunks, int totalMillis) {
        wireMock.stubFor(post("/ml/v1/text/generation_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/event-stream")
                .withChunkedDribbleDelay(chunks, totalMillis)
                .withBody(body)));
    }

    TextGenerationService service() {
        return TextGenerationService.builder()
            .authenticator(mockAuthenticator)
            .modelId("m")
            .projectId("project-id")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();
    }

    CompletableFuture<Void> start(Recorder recorder) {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-token"));
        var future = service().generateStreaming("Tell me a long story", recorder);
        recorder.future.complete(future);
        return future;
    }

    // Test 15 and 18: cancellation through the service API, triggered from inside a callback.
    @Test
    void should_stop_delivering_callbacks_after_cancel() throws Exception {

        stubGenerationStream(body(12), 40, 4000);

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

    // Test 16: cancelling before the first chunk reaches the subscriber.
    @Test
    void should_deliver_no_callback_when_cancelled_before_the_first_chunk() throws Exception {

        wireMock.stubFor(post("/ml/v1/text/generation_stream?version=%s".formatted(API_VERSION))
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

    // Test 17: the future the caller now receives still tracks the outcome of a stream nobody cancels.
    @Test
    void should_complete_normally_at_the_end_of_the_stream() throws Exception {

        stubGenerationStream(body(3), 4, 100);

        var recorder = new Recorder(0);
        var future = start(recorder);

        future.get(10, TimeUnit.SECONDS);

        assertTrue(recorder.terminated.await(10, TimeUnit.SECONDS), "the stream never terminated");
        assertTrue(future.isDone() && !future.isCompletedExceptionally());
        assertFalse(future.isCancelled());
        assertEquals(4, recorder.partialResponses.get());
        assertEquals(1, recorder.completeResponses.get());
        assertEquals(0, recorder.errors.get());
    }

    // Test 17: a failing stream is reported both to the handler and through the returned future.
    @Test
    void should_complete_exceptionally_when_the_stream_fails() throws Exception {

        wireMock.stubFor(post("/ml/v1/text/generation_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "errors": [ { "code": "invalid_request_entity", "message": "input is required" } ],
                      "trace": "trace-id",
                      "status_code": 400
                    }
                    """)));

        var recorder = new Recorder(0);
        var future = start(recorder);

        assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));

        assertTrue(recorder.terminated.await(10, TimeUnit.SECONDS), "the failure was never reported to the handler");
        assertTrue(future.isCompletedExceptionally());
        assertFalse(future.isCancelled());
        assertEquals(1, recorder.errors.get());
        assertEquals(0, recorder.completeResponses.get());
    }
}
