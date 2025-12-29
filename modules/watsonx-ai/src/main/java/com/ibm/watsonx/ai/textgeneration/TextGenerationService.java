/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service class to interact with IBM watsonx.ai Text Generation APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextGenerationService textGenerationService = TextGenerationService.builder()
 *     .baseUrl("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-13b-instruct-v2")
 *     .build();
 *
 * TextGenerationResponse response = textGenerationService.generate("Hello!");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class TextGenerationService extends ModelService implements TextGenerationProvider {
    private static final Logger logger = LoggerFactory.getLogger(TextGenerationService.class);
    private final TextGenerationRestClient client;

    private TextGenerationService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = TextGenerationRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .build();
    }

    @Override
    public TextGenerationResponse generate(TextGenerationRequest textGenerationRequest) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        if (nonNull(textGenerationRequest.getDeploymentId()))
            logger.info("The deploymentId parameter can not be used with the TextGenerationService. Use the DeploymentService instead");

        var input = requireNonNull(textGenerationRequest.getInput(), "input cannot be null");
        var moderation = textGenerationRequest.getModeration();
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());

        if (nonNull(parameters.getPromptVariables())) {
            parameters.setPromptVariables(null);
            logger.warn("Prompt variables are not supported in Text Generation service");
        }

        var projectSpace = resolveProjectSpace(parameters);
        var projectId = projectSpace.projectId();
        var spaceId = projectSpace.spaceId();
        var modelId = requireNonNullElse(parameters.modelId(), this.modelId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        var textGenRequest =
            new TextRequest(modelId, spaceId, projectId, input, parameters.toSanitized(), moderation);

        return client.generate(parameters.transactionId(), textGenRequest);
    }

    @Override
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest textGenerationRequest, TextGenerationHandler handler) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        if (nonNull(textGenerationRequest.getDeploymentId()))
            logger.info("The deploymentId parameter can not be used with the TextGenerationService. Use the DeploymentService instead");

        var input = requireNonNull(textGenerationRequest.getInput(), "input cannot be null");
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());

        if (nonNull(parameters.getPromptVariables())) {
            parameters.setPromptVariables(null);
            logger.warn("Prompt variables are not supported in Text Generation service");
        }

        var modelId = requireNonNullElse(parameters.modelId(), this.modelId);
        var projectId = ofNullable(parameters.projectId()).orElse(this.projectId);
        var spaceId = ofNullable(parameters.spaceId()).orElse(this.spaceId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        var textGenRequest =
            new TextRequest(modelId, spaceId, projectId, input, parameters.toSanitized(), null);

        return client.generateStreaming(parameters.transactionId(), textGenRequest, handler);
    }

    /**
     * Generates text based on the given input string.
     *
     * @param input the input text to generate from
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input) {
        return generate(input, TextGenerationParameters.builder().build());
    }


    /**
     * Generates text based on the given input and parameters.
     *
     * @param input the input text to generate from
     * @param parameters the parameters to configure text generation behavior
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input, TextGenerationParameters parameters) {
        return generate(
            TextGenerationRequest.builder()
                .input(input)
                .parameters(parameters)
                .build());
    }

    /**
     * Generates text based on the given input.
     *
     * @param input the input text to generate from
     * @param moderation the moderation settings.
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public TextGenerationResponse generate(String input, Moderation moderation) {
        return generate(
            TextGenerationRequest.builder()
                .input(input)
                .moderation(moderation)
                .build());
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
     * @param parameters the parameters to configure text generation behavior
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the generation is done
     */
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationParameters parameters, TextGenerationHandler handler) {
        return generateStreaming(
            TextGenerationRequest.builder()
                .input(input)
                .parameters(parameters)
                .build(),
            handler);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextGenerationService textGenerationService = TextGenerationService.builder()
     *     .baseUrl("https://...")      // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
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
     * Builder class for constructing {@link TextGenerationService} instances with configurable parameters.
     */
    public final static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

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
