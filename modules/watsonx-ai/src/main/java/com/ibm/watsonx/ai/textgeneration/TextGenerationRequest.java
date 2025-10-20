/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Represents a text generation request.
 * <p>
 * Instances are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = TextGenerationParameters.builder()
 *     .decodingMethod("greedy")
 *     .maxNewTokens(512)
 *     .timeLimit(Duration.ofSeconds(10))
 *     .build();
 *
 * TextGenerationRequest request = TextGenerationRequest.builder()
 *     .input("Tell me a joke")
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 */
public final class TextGenerationRequest {
    private final String deploymentId;
    private final String input;
    private final Moderation moderation;
    private final TextGenerationParameters parameters;

    protected TextGenerationRequest(Builder builder) {
        input = builder.input;
        moderation = builder.moderation;
        parameters = builder.parameters;
        deploymentId = builder.deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getInput() {
        return input;
    }

    public Moderation getModeration() {
        return moderation;
    }

    public TextGenerationParameters getParameters() {
        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = TextGenerationParameters.builder()
     *     .decodingMethod("greedy")
     *     .maxNewTokens(512)
     *     .timeLimit(Duration.ofSeconds(10))
     *     .build();
     *
     * TextGenerationRequest request = TextGenerationRequest.builder()
     *     .input("Tell me a joke")
     *     .parameters(parameters)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextGenerationRequest} instances.
     */
    public final static class Builder {
        private String deploymentId;
        private String input;
        private Moderation moderation;
        private TextGenerationParameters parameters;

        private Builder() {}

        /**
         * Sets the deployment identifier for the text generation request.
         * <p>
         * This value is required if the request will be sent via a {@link DeploymentService}. For other services, this value may be ignored.
         *
         * @param deploymentId the unique identifier of the deployment
         */
        public Builder deploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
            return this;
        }

        /**
         * Sets the text input for the generation request.
         * <p>
         * This is the prompt that will be sent to the model for text generation.
         *
         * @param input the text prompt to generate from
         */
        public Builder input(String input) {
            this.input = input;
            return this;
        }

        /**
         * Sets moderation options for the generation request.
         * <p>
         * The {@link Moderation} object can be used to apply content filtering or safety checks on the generated text.
         *
         * @param moderation a {@link Moderation} instance specifying moderation settings
         */
        public Builder moderation(Moderation moderation) {
            this.moderation = moderation;
            return this;
        }

        /**
         * Sets the parameters controlling the text generation behavior.
         *
         * @param parameters a {@link TextGenerationParameters} instance
         */
        public Builder parameters(TextGenerationParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link TextGenerationRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link TextGenerationRequest}
         */
        public TextGenerationRequest build() {
            return new TextGenerationRequest(this);
        }
    }
}
