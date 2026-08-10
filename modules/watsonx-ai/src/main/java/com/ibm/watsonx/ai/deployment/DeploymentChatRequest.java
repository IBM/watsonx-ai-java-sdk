/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.chat.NativeChatRequest;

/**
 * Represents a chat request for the {@link DeploymentService}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = ChatParameters.builder()
 *     .temperature(0.7)
 *     .build();
 *
 * DeploymentChatRequest request = DeploymentChatRequest.builder()
 *     .deploymentId("my-deployment-id")
 *     .parameters(parameters)
 *     .messages(
 *         SystemMessage.of("You are a helpful assistant"),
 *         UserMessage.text("Tell me a joke")
 *     ).build();
 * }</pre>
 *
 * @see DeploymentService
 * @see NativeChatRequest
 */
public final class DeploymentChatRequest extends NativeChatRequest {
    private final String deploymentId;

    private DeploymentChatRequest(Builder builder) {
        super(builder);
        deploymentId = requireNonNull(builder.deploymentId, "deploymentId must be provided");
    }

    /**
     * Returns the deployment id.
     *
     * @return the deployment id
     */
    public String deploymentId() {
        return deploymentId;
    }

    /**
     * Creates a builder initialized with the current state of the {@code DeploymentChatRequest}.
     *
     * @return a new {@link Builder} instance pre-populated with this {@code DeploymentChatRequest}'s data
     */
    public Builder toBuilder() {
        return new Builder()
            .deploymentId(deploymentId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .thinking(thinking);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = ChatParameters.builder()
     *     .temperature(0.7)
     *     .build();
     *
     * DeploymentChatRequest request = DeploymentChatRequest.builder()
     *     .deploymentId("my-deployment-id")
     *     .parameters(parameters)
     *     .messages(
     *         SystemMessage.of("You are a helpful assistant"),
     *         UserMessage.text("Tell me a joke")
     *     ).build();
     * }</pre>
     *
     * @see DeploymentService
     * @see NativeChatRequest
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DeploymentChatRequest} instances.
     */
    public final static class Builder extends NativeChatRequest.Builder<Builder> {
        private String deploymentId;

        private Builder() {}

        /**
         * Sets the deployment identifier for the chat request.
         *
         * @param deploymentId the unique identifier of the deployment
         */
        public Builder deploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
            return this;
        }

        /**
         * Builds a {@link DeploymentChatRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link DeploymentChatRequest}
         * @throws NullPointerException if {@code deploymentId} was not provided
         */
        public DeploymentChatRequest build() {
            return new DeploymentChatRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((deploymentId == null) ? 0 : deploymentId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        DeploymentChatRequest other = (DeploymentChatRequest) obj;
        if (deploymentId == null) {
            if (other.deploymentId != null)
                return false;
        } else if (!deploymentId.equals(other.deploymentId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DeploymentChatRequest [deploymentId=" + deploymentId + ", messages=" + messages + ", tools=" + tools
            + ", parameters=" + parameters + ", thinking=" + thinking + "]";
    }
}
