/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with IBM watsonx.ai Model Gateway image generation APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayImageService imageService = ModelGatewayImageService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .modelId("gpt-image-1")
 *     .build();
 *
 * ModelGatewayImageResponse response = imageService.generate("A futuristic city at sunset");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ModelGatewayImageService extends WatsonxService {

    private final ModelGatewayImageRestClient client;
    private final String modelId;

    private ModelGatewayImageService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        modelId = requireNonNull(builder.modelId, "The modelId must be provided");

        client = ModelGatewayImageRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Generates an image from the provided prompt text.
     *
     * @param prompt the text description of the desired image
     * @return a {@link ModelGatewayImageResponse} containing the generated images
     */
    public ModelGatewayImageResponse generate(String prompt) {
        return generate(prompt, null);
    }

    /**
     * Generates an image from the provided prompt text.
     *
     * @param prompt the text description of the desired image
     * @param parameters the parameters for the image generation request
     * @return a {@link ModelGatewayImageResponse} containing the generated images
     */
    public ModelGatewayImageResponse generate(String prompt, ModelGatewayImageParameters parameters) {
        return generate(
            ModelGatewayImageRequest.builder()
                .prompt(prompt)
                .parameters(parameters)
                .build()
        );
    }

    /**
     * Generates an image from the provided request.
     *
     * @param request the {@link ModelGatewayImageRequest} containing the prompt and optional parameters
     * @return a {@link ModelGatewayImageResponse} containing the generated images
     */
    public ModelGatewayImageResponse generate(ModelGatewayImageRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.prompt(), "prompt cannot be null");

        if (request.prompt().isBlank())
            throw new IllegalArgumentException("The prompt must not be blank");

        return client.generate(ModelGatewayImageGenerationRequest.of(modelId, request));
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayImageService} instances with configurable parameters.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {

        private String modelId;

        private Builder() {}

        /**
         * Sets the model identifier to use for image generation.
         *
         * @param modelId the model id
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayImageService} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayImageService}
         */
        public ModelGatewayImageService build() {
            return new ModelGatewayImageService(this);
        }
    }
}
