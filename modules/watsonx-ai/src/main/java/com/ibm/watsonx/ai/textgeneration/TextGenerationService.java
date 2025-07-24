/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.Result;

/**
 * Infer the next tokens for a given deployed model with a set of parameters.
 * <p>
 * This API is legacy, consider using {@link ChatService}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextGenerationService textGenerationService = TextGenerationService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .authenticationProvider(authProvider)
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-13b-instruct-v2")
 *     .build();
 *
 * TextGenerationResponse response = textGenerationService.generate("Hello!");
 * }</pre>
 *
 * @see https://cloud.ibm.com/apidocs/watsonx-ai#text-generation
 */
public final class TextGenerationService extends WatsonxService {

    protected TextGenerationService(Builder builder) {
        super(builder);
    }

    /**
     * Generates text based on the given input string.
     *
     * @param input the input text to generate from
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input) {
        return generate(input, null, null);
    }

    /**
     * Generates text based on the given input string parameters.
     *
     * @param input the input text to generate from
     * @param parameters the parameters to configure text generation behavior
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input, TextGenerationParameters parameters) {
        return generate(input, null, parameters);
    }

    /**
     * Generates text based on the given input string with moderation applied.
     *
     * @param input the input text to generate from
     * @param moderation the moderation settings to apply during generation
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input, Moderation moderation) {
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
    public TextGenerationResponse generate(String input, Moderation moderation, TextGenerationParameters parameters) {

        if (isNull(input) || input.isBlank())
            throw new IllegalArgumentException("The input can not be null or empty");

        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
        var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;

        parameters.setTimeLimit(
            isNull(parameters.getTimeLimit())
                ? timeout.toMillis()
                : parameters.getTimeLimit()
        );

        var textGenerationRequest =
            new TextGenerationRequest(modelId, spaceId, projectId, input, parameters, moderation);

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(url.toString() + "%s/generation?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(textGenerationRequest)))
                .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextGenerationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

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
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationHandler handler) {
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
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationParameters parameters, TextGenerationHandler handler) {

        if (isNull(input) || input.isBlank())
            throw new IllegalArgumentException("The input can not be null or empty");

        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
        var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;

        parameters.setTimeLimit(
            isNull(parameters.getTimeLimit())
                ? timeout.toMillis()
                : parameters.getTimeLimit()
        );

        var textGenerationRequest =
            new TextGenerationRequest(modelId, spaceId, projectId, input, parameters, null);

        var httpRequest =
            HttpRequest
                .newBuilder(
                    URI.create(url.toString() + "%s/generation_stream?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(textGenerationRequest)))
                .build();

        var subscriber = new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
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

        return asyncHttpClient
            .send(httpRequest,
                responseInfo -> logResponses
                    ? BodySubscribers
                        .fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
                    : BodySubscribers.fromLineSubscriber(subscriber)
            ).thenApply(response -> null);
    }


    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextGenerationService textGenerationService = TextGenerationService.builder()
     *     .url("https://...") // or use CloudRegion
     *     .authenticationProvider(authProvider)
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-13b-instruct-v2")
     *     .build();
     *
     * TextGenerationResponse response = textGenerationService.generate("Hello!");
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {

        /**
         * Builds a {@link TextGenerationService} instance using the configured parameters.
         *
         * @return a new instance of {@link TextGenerationService}
         */
        public TextGenerationService build() {
            return new TextGenerationService(this);
        }
    }
}
