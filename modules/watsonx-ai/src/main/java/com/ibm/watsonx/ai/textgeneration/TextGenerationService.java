/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

/**
 * Service class to interact with IBM watsonx.ai Text Generation APIs.
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
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-generation" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class TextGenerationService extends ModelService implements TextGenerationProvider {

    private static final Logger logger = LoggerFactory.getLogger(TextGenerationService.class);

    protected TextGenerationService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
    }

    @Override
    public TextGenerationResponse generate(String input, Moderation moderation, TextGenerationParameters parameters) {

        if (isNull(input) || input.isBlank())
            throw new IllegalArgumentException("The input can not be null or empty");

        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());

        if (nonNull(parameters.getPromptVariables())) {
            parameters.setPromptVariables(null);
            logger.warn("Prompt variables are not supported in Text Generation service");
        }

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
        var spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        var textGenerationRequest =
            new TextGenerationRequest(modelId, spaceId, projectId, input, parameters.toSanitized(), moderation);

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(url.toString() + "%s/generation?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeout))
                .POST(BodyPublishers.ofString(toJson(textGenerationRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextGenerationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationParameters parameters, TextGenerationHandler handler) {

        requireNonNull(input, "input cannot be null");
        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());

        if (nonNull(parameters.getPromptVariables())) {
            parameters.setPromptVariables(null);
            logger.warn("Prompt variables are not supported in Text Generation service");
        }

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
        var spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        var textGenerationRequest =
            new TextGenerationRequest(modelId, spaceId, projectId, input, parameters.toSanitized(), null);

        var httpRequest = HttpRequest.newBuilder(URI.create(url.toString() + "%s/generation_stream?version=%s".formatted(ML_API_TEXT_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(textGenerationRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        var subscriber = subscriber(handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handlerError(t, handler));
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
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatService} instances with configurable parameters.
     */
    public static class Builder extends ModelService.Builder<Builder> {

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
