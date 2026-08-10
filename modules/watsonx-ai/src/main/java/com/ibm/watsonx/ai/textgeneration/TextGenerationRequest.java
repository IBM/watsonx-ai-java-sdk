/*
 * Copyright 2025 IBM Corporation
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

    private TextGenerationRequest(Builder builder) {
        input = builder.input;
        moderation = builder.moderation;
        parameters = builder.parameters;
        deploymentId = builder.deploymentId;
    }

    /**
     * Gets the deployment identifier.
     *
     * @return the deployment ID
     */
    public String deploymentId() {
        return deploymentId;
    }

    /**
     * Gets the input text prompt.
     *
     * @return the input text
     */
    public String input() {
        return input;
    }

    /**
     * Gets the moderation configuration.
     *
     * @return the moderation settings
     */
    public Moderation moderation() {
        return moderation;
    }

    /**
     * Gets the text generation parameters.
     *
     * @return the generation parameters
     */
    public TextGenerationParameters parameters() {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deploymentId == null) ? 0 : deploymentId.hashCode());
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + ((moderation == null) ? 0 : moderation.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextGenerationRequest other = (TextGenerationRequest) obj;
        if (deploymentId == null) {
            if (other.deploymentId != null)
                return false;
        } else if (!deploymentId.equals(other.deploymentId))
            return false;
        if (input == null) {
            if (other.input != null)
                return false;
        } else if (!input.equals(other.input))
            return false;
        if (moderation == null) {
            if (other.moderation != null)
                return false;
        } else if (!moderation.equals(other.moderation))
            return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextGenerationRequest [deploymentId=" + deploymentId + ", input=" + input + ", moderation=" + moderation + ", parameters="
            + parameters + "]";
    }
}
