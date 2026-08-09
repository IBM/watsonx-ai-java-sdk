/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

/**
 * Represents an image generation request for the {@link ModelGatewayImageService}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = ModelGatewayImageParameters.builder()
 *     .n(1)
 *     .size(Size.SIZE_1024X1024)
 *     .responseFormat(ResponseFormat.URL)
 *     .build();
 *
 * ModelGatewayImageRequest request = ModelGatewayImageRequest.builder()
 *     .prompt("A futuristic city at sunset")
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 *
 * @see ModelGatewayImageService
 * @see ModelGatewayImageParameters
 */
public final class ModelGatewayImageRequest {

    private final String prompt;
    private final ModelGatewayImageParameters parameters;

    private ModelGatewayImageRequest(Builder builder) {
        prompt = builder.prompt;
        parameters = builder.parameters;
    }

    /**
     * Returns the text description of the desired image.
     *
     * @return the prompt, or {@code null} if not set
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Returns the image generation parameters.
     *
     * @return the image generation parameters, or {@code null} if not set
     */
    public ModelGatewayImageParameters parameters() {
        return parameters;
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
     * Builder class for constructing {@link ModelGatewayImageRequest} instances.
     */
    public static final class Builder {

        private String prompt;
        private ModelGatewayImageParameters parameters;

        private Builder() {}

        /**
         * Sets the text description of the desired image.
         *
         * @param prompt the prompt
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Sets the parameters controlling the image generation model behavior.
         *
         * @param parameters a {@link ModelGatewayImageParameters} instance
         */
        public Builder parameters(ModelGatewayImageParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayImageRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayImageRequest}
         */
        public ModelGatewayImageRequest build() {
            return new ModelGatewayImageRequest(this);
        }
    }

    @Override
    public String toString() {
        return "ModelGatewayImageRequest [prompt=" + prompt + ", parameters=" + parameters + "]";
    }
}
