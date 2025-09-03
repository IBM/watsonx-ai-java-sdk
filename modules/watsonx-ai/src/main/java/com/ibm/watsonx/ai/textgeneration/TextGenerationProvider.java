/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.Result;

/**
 * A provider interface for text generation functionality.
 *
 * @see TextGenerationService
 * @see DeploymentService
 */
public interface TextGenerationProvider {

    /**
     * Generates text based on the provided {@link TextGenerationRequest}.
     *
     * @param request the {@link TextGenerationRequest} containing input, moderation, parameters.
     * @return a {@link TextGenerationResponse} containing the generated text and associated metadata
     */
    public TextGenerationResponse generate(TextGenerationRequest request);

    /**
     * Sends a streaming text generation request based on the provided {@link TextGenerationRequest}.
     * <p>
     * This method initiates an asynchronous text generation operation where partial responses are delivered incrementally through the provided
     * {@link TextGenerationHandler}.
     *
     * @param request the {@link TextGenerationRequest} containing input, moderation, parameters, and optional deployment ID
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the streaming generation is finished
     */
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest request, TextGenerationHandler handler);

    /**
     * Handles an error by invoking the {@link ChatHandler}'s {@code onError} callback if the given throwable is non-null.
     *
     * @param t the {@link Throwable} to handle
     * @param handler the {@link ChatHandler} that should be notified of the error
     * @return always {@code null}, enabling direct use in async exception handlers
     */
    public default Void handlerError(Throwable t, TextGenerationHandler handler) {
        ofNullable(t).ifPresent(handler::onError);
        return null;
    }

    /**
     * Returns a {@link Flow.Subscriber} implementation that processes streaming text generation responses.
     * <p>
     * This subscriber is designed to be thread-safe for a single streaming request. It correctly handles state aggregation and memory visibility for
     * its internal fields.
     * <p>
     * <b>Thread Safety Considerations:</b>
     * <p>
     * This implementation guarantees that all callback methods on the provided {@code TextGenerationHandler} instance will be invoked sequentially,
     * even if the {@code TextGenerationHandler} is shared across multiple concurrent streaming requests. This is achieved by using a synchronized
     * lock on the handler instance itself, which serializes access to its methods.
     *
     * @param handler the callback handler to receive updates and final result
     * @return a {@link Flow.Subscriber} for processing streamed text generation responses
     */
    public default Flow.Subscriber<String> subscriber(TextGenerationHandler handler) {
        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile String modelId;
            private volatile int inputTokenCount;
            private volatile int generatedTokenCount;
            private volatile String stopReason;
            private volatile boolean success = true;
            private volatile boolean pendingSSEError = false;
            private final StringBuffer buffer = new StringBuffer();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {

                try {

                    if (isNull(partialMessage) || partialMessage.isBlank())
                        return;

                    if (partialMessage.startsWith("event: error")) {
                        pendingSSEError = true;
                        return;
                    }

                    if (!partialMessage.startsWith("data:"))
                        return;

                    var messageData = partialMessage.split("data: ")[1];

                    if (pendingSSEError) {
                        pendingSSEError = false;
                        throw new RuntimeException(messageData);
                    }

                    // TODO: Use the ExecutorProvider.cpuExecutor().
                    var chunk = Json.fromJson(messageData, TextGenerationResponse.class);

                    if (chunk.results().size() == 0) {
                        return;
                    }

                    if (isNull(modelId) && nonNull(chunk.modelId()))
                        modelId = chunk.modelId();

                    var result = chunk.results().get(0);

                    if (nonNull(result.inputTokenCount()))
                        inputTokenCount += result.inputTokenCount();

                    if (nonNull(result.generatedTokenCount()))
                        generatedTokenCount += result.generatedTokenCount();

                    if (nonNull(result.stopReason()))
                        stopReason = result.stopReason();

                    if (nonNull(result.generatedText()) && !result.generatedText().isEmpty()) {
                        buffer.append(result.generatedText());
                        synchronized (handler) {
                            handler.onPartialResponse(result.generatedText());
                        }
                    }

                } catch (RuntimeException e) {
                    onError(e);
                    success = !handler.failOnFirstError();
                } finally {
                    if (success) {
                        subscription.request(1);
                    } else {
                        subscription.cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                synchronized (handler) {
                    handler.onError(throwable);
                }
            }

            @Override
            public void onComplete() {
                try {

                    var result = List.of(new Result(buffer.toString(), stopReason, generatedTokenCount,
                        inputTokenCount, null, null, null, null));
                    synchronized (handler) {
                        handler.onCompleteResponse(new TextGenerationResponse(modelId, null, null, result));
                    }

                } catch (RuntimeException e) {
                    onError(e);
                }
            }
        };
    }
}
