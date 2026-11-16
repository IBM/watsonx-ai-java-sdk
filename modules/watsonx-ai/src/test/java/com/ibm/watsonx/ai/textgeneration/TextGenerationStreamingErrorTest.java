/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.deployment.DeploymentService;

@SuppressWarnings("unchecked")
@DisabledInNativeImage
class TextGenerationStreamingErrorTest extends AbstractWatsonxTest {

    @Test
    void should_fail_the_future_when_a_chunk_is_malformed() {
        assertTerminalFailure("data: {\n\n", false);
    }

    @Test
    void should_fail_the_future_when_a_chunk_is_malformed_and_the_error_callback_throws() {
        assertTerminalFailure("data: {\n\n", true);
    }

    @Test
    void should_fail_the_future_when_the_server_sends_an_sse_error() {
        assertTerminalFailure("event: error\ndata: model failed\n\n", false);
    }

    @Test
    void should_fail_the_future_when_the_server_sends_an_sse_error_and_the_error_callback_throws() {
        assertTerminalFailure("event: error\ndata: model failed\n\n", true);
    }

    @Test
    void should_continue_after_a_recoverable_chunk_error() {
        assertRecoverableFailure(false);
    }

    @Test
    void should_continue_after_a_recoverable_chunk_error_even_when_the_error_callback_throws() {
        assertRecoverableFailure(true);
    }

    @Test
    void should_fail_the_future_when_the_completion_callback_throws() {
        assertCompletionFailure(false);
    }

    @Test
    void should_fail_the_future_when_both_terminal_callbacks_throw() {
        assertCompletionFailure(true);
    }

    private void assertCompletionFailure(boolean errorCallbackThrows) {
        for (boolean deployment : List.of(false, true)) {
            for (boolean httpCompletesFirst : List.of(false, true)) {
                for (boolean callbackThrowsError : List.of(false, true)) {
                    var body = new AtomicReference<BodySubscriber<Void>>();
                    var transport = prepareTransport(body);
                    var subscription = mock(Subscription.class);
                    var errors = new AtomicInteger();
                    var completions = new AtomicInteger();
                    var reportedFailure = new AtomicReference<Throwable>();
                    Throwable completionFailure = callbackThrowsError ? new AssertionError("Completion callback failed")
                        : new IllegalStateException("Completion callback failed");
                    Throwable errorCallbackFailure = !errorCallbackThrows ? null
                        : callbackThrowsError ? new AssertionError("Error callback failed") : new IllegalStateException("Error callback failed");
                    var partials = new ArrayList<String>();

                    withWatsonxServiceMock(() -> {
                        var response = startStream(deployment, new TextGenerationHandler() {
                            @Override
                            public void onPartialResponse(String partialResponse) {
                                partials.add(partialResponse);
                            }

                            @Override
                            public void onCompleteResponse(TextGenerationResponse completeResponse) {
                                completions.incrementAndGet();
                                throwCallbackFailure(completionFailure);
                            }

                            @Override
                            public void onError(Throwable error) {
                                errors.incrementAndGet();
                                reportedFailure.set(error);
                                if (errorCallbackFailure != null)
                                    throwCallbackFailure(errorCallbackFailure);
                            }
                        });
                        if (httpCompletesFirst) {
                            transport.complete(mock(HttpResponse.class));
                            assertFalse(response.isDone(), "HTTP completion must wait for the terminal callback");
                        }
                        body.get().onSubscribe(subscription);
                        body.get().onNext(bytes("data: {\"model_id\":\"model\",\"results\":[{\"generated_text\":\"Hello\"}]}\n\n"));
                        body.get().onComplete();
                        transport.complete(mock(HttpResponse.class));

                        var failure = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));
                        assertSame(completionFailure, failure.getCause());
                        assertSame(completionFailure, reportedFailure.get());
                        assertEquals(errorCallbackThrows ? List.of(errorCallbackFailure) : List.of(),
                            List.of(failure.getCause().getSuppressed()));
                        assertEquals(List.of("Hello"), partials);
                        assertEquals(1, errors.get());
                        assertEquals(1, completions.get());
                        verify(subscription, atLeastOnce()).cancel();
                    });
                }
            }
        }
    }

    private void throwCallbackFailure(Throwable failure) {
        if (failure instanceof Error error)
            throw error;
        throw (RuntimeException) failure;
    }

    private void assertTerminalFailure(String payload, boolean callbackThrows) {
        assertTerminalFailure(payload, callbackThrows, false);
        assertTerminalFailure(payload, callbackThrows, true);
    }

    private void assertTerminalFailure(String payload, boolean callbackThrows, boolean httpCompletesFirst) {
        for (boolean deployment : List.of(false, true)) {
            var body = new AtomicReference<BodySubscriber<Void>>();
            var transport = prepareTransport(body);
            var subscription = mock(Subscription.class);
            var errors = new AtomicInteger();
            var completions = new AtomicInteger();
            var reportedFailure = new AtomicReference<Throwable>();
            var callbackFailure = callbackThrows ? new IllegalStateException("Error callback failed") : null;

            withWatsonxServiceMock(() -> {
                var response = startStream(deployment, handler(true, errors, completions, reportedFailure, callbackFailure, new ArrayList<>()));
                assertNotNull(body.get());
                if (httpCompletesFirst) {
                    transport.complete(mock(HttpResponse.class));
                    assertFalse(response.isDone(), "HTTP completion must wait for queued line processing");
                }
                body.get().onSubscribe(subscription);
                body.get().onNext(bytes(payload));

                assertTrue(response.isCompletedExceptionally(), "A terminal chunk error must fail the public future before HTTP completion");
                var failure = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));
                assertSame(reportedFailure.get(), failure.getCause());
                assertEquals(callbackThrows ? List.of(callbackFailure) : List.of(), List.of(failure.getCause().getSuppressed()));
                assertEquals(1, errors.get());
                assertEquals(0, completions.get());
                verify(subscription, atLeastOnce()).cancel();

                body.get().onComplete();
                transport.complete(mock(HttpResponse.class));
                var finalFailure = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));
                assertSame(failure.getCause(), finalFailure.getCause());
                assertEquals(1, errors.get());
                assertEquals(0, completions.get());
            });
        }
    }

    private void assertRecoverableFailure(boolean callbackThrows) {
        for (boolean deployment : List.of(false, true)) {
            var body = new AtomicReference<BodySubscriber<Void>>();
            var transport = prepareTransport(body);
            var subscription = mock(Subscription.class);
            var errors = new AtomicInteger();
            var completions = new AtomicInteger();
            var reportedFailure = new AtomicReference<Throwable>();
            var callbackFailure = callbackThrows ? new IllegalStateException("Error callback failed") : null;
            var partials = new ArrayList<String>();

            withWatsonxServiceMock(() -> {
                var response = startStream(deployment, handler(false, errors, completions, reportedFailure, callbackFailure, partials));
                transport.complete(mock(HttpResponse.class));
                assertFalse(response.isDone(), "HTTP completion must wait for queued line processing");
                body.get().onSubscribe(subscription);
                body.get().onNext(bytes("data: {\n\n"));
                assertFalse(response.isDone());
                body.get().onNext(bytes("data: {\"model_id\":\"model\",\"results\":[{\"generated_text\":\"Hello\"}]}\n\n"));
                assertFalse(response.isDone(), "The public future must wait for the subscriber completion callback");
                body.get().onComplete();
                transport.complete(mock(HttpResponse.class));

                assertNull(response.join());
                assertEquals(List.of("Hello"), partials);
                assertEquals(1, errors.get());
                assertEquals(1, completions.get());
                verify(subscription, never()).cancel();
            });
        }
    }

    private CompletableFuture<HttpResponse<Void>> prepareTransport(AtomicReference<BodySubscriber<Void>> body) {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));
        var transport = new CompletableFuture<HttpResponse<Void>>();
        var responseInfo = mock(ResponseInfo.class);
        when(responseInfo.statusCode()).thenReturn(200);
        doAnswer(invocation -> {
            BodyHandler<Void> bodyHandler = invocation.getArgument(1);
            body.set(bodyHandler.apply(responseInfo));
            return transport;
        }).when(mockSecureHttpClient).sendAsync(any(), any(BodyHandler.class));
        return transport;
    }

    private CompletableFuture<Void> startStream(boolean deployment, TextGenerationHandler handler) {
        if (deployment)
            return DeploymentService.builder().baseUrl("http://localhost").authenticator(mockAuthenticator).build()
                .generateStreaming(TextGenerationRequest.builder().deploymentId("deployment").input("Hello").build(), handler);
        return TextGenerationService.builder().baseUrl("http://localhost").authenticator(mockAuthenticator)
            .projectId("project").modelId("model").build().generateStreaming("Hello", handler);
    }

    private TextGenerationHandler handler(boolean failOnFirstError, AtomicInteger errors, AtomicInteger completions,
        AtomicReference<Throwable> reportedFailure, RuntimeException callbackFailure, List<String> partials) {
        return new TextGenerationHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                partials.add(partialResponse);
            }

            @Override
            public void onCompleteResponse(TextGenerationResponse completeResponse) {
                completions.incrementAndGet();
            }

            @Override
            public void onError(Throwable error) {
                errors.incrementAndGet();
                reportedFailure.set(error);
                if (callbackFailure != null)
                    throw callbackFailure;
            }

            @Override
            public boolean failOnFirstError() {
                return failOnFirstError;
            }
        };
    }

    private List<ByteBuffer> bytes(String payload) {
        return List.of(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
