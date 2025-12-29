/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.util.List;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.Result;

/**
 * Subscriber abstraction for consuming raw SSE messages from the watsonx.ai Text Generation Streaming API.
 */
public interface TextGenerationSubscriber {

    /**
     * Called when a new message chunk is received from the server.
     *
     * @param partialMessage the raw SSE event payload
     */
    void onNext(String partialMessage);

    /**
     * Called if an error occurs during streaming.
     *
     * @param throwable the error that occurred
     */
    void onError(Throwable throwable);

    /**
     * Called once the streaming session has completed successfully.
     */
    void onComplete();

    /**
     * Handles an error by invoking the {@link TextGenerationHandler}'s {@code onError} callback if the given throwable is non-null.
     *
     * @param t the {@link Throwable} to handle
     * @param handler the {@link TextGenerationHandler} that should be notified of the error
     * @return always {@code null}, enabling direct use in async exception handlers
     */
    static Void handleError(Throwable t, TextGenerationHandler handler) {
        ofNullable(t).ifPresent(handler::onError);
        return null;
    }

    /**
     * Creates a {@link TextGenerationSubscriber} that processes streamed chat responses from the watsonx.ai API.
     *
     * @param handler the {@link TextGenerationHandler} that will receive callbacks during streaming
     * @return a new {@link TextGenerationSubscriber} instance for processing SSE events
     */
    static TextGenerationSubscriber createSubscriber(TextGenerationHandler handler) {
        return new TextGenerationSubscriber() {
            private volatile String modelId;
            private volatile int inputTokenCount;
            private volatile int generatedTokenCount;
            private volatile String stopReason;
            private volatile boolean pendingSSEError = false;
            private final StringBuffer buffer = new StringBuffer();

            @Override
            public void onNext(String partialMessage) {


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

                    handler.onPartialResponse(result.generatedText());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                handler.onError(throwable);
            }

            @Override
            public void onComplete() {
                try {

                    var result = List.of(new Result(buffer.toString(), stopReason, generatedTokenCount,
                        inputTokenCount, null, null, null, null));

                    handler.onCompleteResponse(new TextGenerationResponse(modelId, null, null, result));

                } catch (RuntimeException e) {
                    onError(e);
                }
            }
        };
    }
}
