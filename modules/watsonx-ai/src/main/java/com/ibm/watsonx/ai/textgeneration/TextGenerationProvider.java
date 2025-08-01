/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
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
     * Generates text based on the given input string.
     *
     * @param input the input text to generate from
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public default TextGenerationResponse generate(String input) {
        return generate(input, null, null);
    }

    /**
     * Generates text based on the given input string parameters.
     *
     * @param input the input text to generate from
     * @param parameters the parameters to configure text generation behavior
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public default TextGenerationResponse generate(String input, TextGenerationParameters parameters) {
        return generate(input, null, parameters);
    }

    /**
     * Generates text based on the given input string with moderation applied.
     *
     * @param input the input text to generate from
     * @param moderation the moderation settings to apply during generation
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public default TextGenerationResponse generate(String input, Moderation moderation) {
        return generate(input, moderation, null);
    }

    /**
     * Generates text based on the given input string, moderation and parameters.
     *
     * @param input the input text to generate from
     * @param moderation the moderation settings to apply during generation
     * @param parameters the parameters to configure text generation behavior
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input, Moderation moderation, TextGenerationParameters parameters);

    /**
     * Sends a streaming text generation request using the provided messages
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link TextGenerationHandler}.
     *
     * @param input the input prompt to send to the model
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the generation is done
     */
    public default CompletableFuture<Void> generateStreaming(String input, TextGenerationHandler handler) {
        return generateStreaming(input, null, handler);
    }

    /**
     * Sends a streaming text generation request using the provided messages
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link TextGenerationHandler}.
     *
     * @param input the input prompt to send to the model
     * @param parameters the parameters to control the generation behavior
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the generation is done
     */
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationParameters parameters, TextGenerationHandler handler);

    /**
     * Returns a {@link Flow.Subscriber} implementation that processes streaming text generation responses.
     *
     * @param modelId the identifier of the model used for generation
     * @param handler the callback handler to receive updates and final result
     * @return a {@link Flow.Subscriber} for processing streamed text generation responses
     */
    public default Flow.Subscriber<String> subscriber(TextGenerationHandler handler) {
        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private String modelId;
            private int inputTokenCount;
            private int generatedTokenCount;
            private String stopReason;
            private boolean success = true;
            private final StringBuilder stringBuilder = new StringBuilder();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {

                try {

                    if (isNull(partialMessage) || partialMessage.isBlank() || !partialMessage.startsWith("data: "))
                        return;

                    var messageData = partialMessage.split("data: ")[1];
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
                        stringBuilder.append(result.generatedText());
                        handler.onPartialResponse(result.generatedText());
                    }

                } catch (RuntimeException e) {

                    success = false;
                    onError(e);

                } finally {

                    if (success)
                        subscription.request(1);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                handler.onError(throwable);
            }

            @Override
            public void onComplete() {
                try {

                    var result = List.of(new Result(stringBuilder.toString(), stopReason, generatedTokenCount,
                        inputTokenCount, null, null, null, null));
                    handler.onCompleteResponse(new TextGenerationResponse(modelId, null, null, result));

                } catch (RuntimeException e) {
                    handler.onError(e);
                }
            }
        };
    }
}
