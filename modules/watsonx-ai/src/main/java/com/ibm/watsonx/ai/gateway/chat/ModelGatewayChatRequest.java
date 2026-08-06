/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import com.ibm.watsonx.ai.chat.BaseChatRequest;

/**
 * Represents a chat request for the {@link ModelGatewayService}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = ModelGatewayParameters.builder()
 *     .temperature(0.7)
 *     .reasoningEffort(ReasoningEffort.MEDIUM)
 *     .build();
 *
 * ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
 *     .parameters(parameters)
 *     .messages(
 *         SystemMessage.of("You are a helpful assistant"),
 *         UserMessage.text("Tell me a joke")
 *     ).build();
 * }</pre>
 *
 * @see ModelGatewayService
 * @see ModelGatewayParameters
 */
public final class ModelGatewayChatRequest extends BaseChatRequest {
    private final ModelGatewayParameters parameters;

    private ModelGatewayChatRequest(Builder builder) {
        super(builder);
        parameters = builder.parameters;
    }

    /**
     * Returns the gateway chat parameters.
     *
     * @return the gateway chat parameters, or {@code null} if not set
     */
    @Override
    public ModelGatewayParameters parameters() {
        return parameters;
    }

    /**
     * Creates a builder initialized with the current state of the {@code ModelGatewayChatRequest}.
     *
     * @return a new {@link Builder} instance pre-populated with this {@code ModelGatewayChatRequest}'s data
     */
    public Builder toBuilder() {
        return new Builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = ModelGatewayParameters.builder()
     *     .temperature(0.7)
     *     .reasoningEffort(ReasoningEffort.MEDIUM)
     *     .build();
     *
     * ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
     *     .parameters(parameters)
     *     .messages(
     *         SystemMessage.of("You are a helpful assistant"),
     *         UserMessage.text("Tell me a joke")
     *     ).build();
     * }</pre>
     *
     * @see ModelGatewayService
     * @see ModelGatewayParameters
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayChatRequest} instances.
     */
    public final static class Builder extends BaseChatRequest.Builder<Builder> {
        private ModelGatewayParameters parameters;

        private Builder() {}

        /**
         * Sets the gateway parameters controlling the chat model's behavior.
         *
         * @see ModelGatewayParameters
         * @param parameters a {@link ModelGatewayParameters}.
         */
        public Builder parameters(ModelGatewayParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayChatRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayChatRequest}
         */
        public ModelGatewayChatRequest build() {
            return new ModelGatewayChatRequest(this);
        }
    }

    @Override
    public String toString() {
        return "ModelGatewayChatRequest [messages=" + messages + ", tools=" + tools + ", parameters=" + parameters + "]";
    }
}
