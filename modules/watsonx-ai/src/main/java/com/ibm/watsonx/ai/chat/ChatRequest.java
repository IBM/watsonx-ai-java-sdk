/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

/**
 * Represents a chat request for the {@link ChatService}.
 * <p>
 * Instances are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var tool = Tool.of(
 *     "send_email",
 *     "Send an email",
 *     JsonSchema.object()
 *         .property("email", JsonSchema.string())
 *         .property("subject", JsonSchema.string())
 *         .property("body", JsonSchema.string())
 *         .required("email", "subject", "body")
 * );
 *
 * var parameters = ChatParameters.builder()
 *     .temperature(0.7)
 *     .maxCompletionTokens(0)
 *     .build();
 *
 * ChatRequest request = ChatRequest.builder()
 *     .tools(tool)
 *     .parameters(parameters)
 *     .messages(
 *         SystemMessage.of("You are a helpful assistant"),
 *         UserMessage.text("Tell me a joke")
 *     ).build();
 * }</pre>
 *
 * @see ChatService
 * @see NativeChatRequest
 */
public final class ChatRequest extends NativeChatRequest {
    private final ChatModeration moderations;

    private ChatRequest(Builder builder) {
        super(builder);
        moderations = builder.moderations;
    }

    /**
     * Returns the inline moderation configuration.
     *
     * @return the moderation configuration, or {@code null} if not set
     */
    public ChatModeration moderations() {
        return moderations;
    }

    /**
     * Creates a builder initialized with the current state of the {@code ChatRequest}.
     *
     * @return a new {@link Builder} instance pre-populated with this {@code ChatRequest}'s data
     */
    public Builder toBuilder() {
        return new Builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .thinking(thinking)
            .moderations(moderations);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = ChatParameters.builder()
     *     .temperature(0.7)
     *     .maxCompletionTokens(0)
     *     .build();
     *
     * ChatRequest request = ChatRequest.builder()
     *     .parameters(parameters)
     *     .messages(
     *         SystemMessage.of("You are a helpful assistant"),
     *         UserMessage.text("Tell me a joke")
     *     ).build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatRequest} instances.
     */
    public final static class Builder extends NativeChatRequest.Builder<Builder> {
        private ChatModeration moderations;

        private Builder() {}

        /**
         * Sets the inline moderation configuration applied to the request.
         *
         * @param moderations the moderation configuration to apply
         */
        public Builder moderations(ChatModeration moderations) {
            this.moderations = moderations;
            return this;
        }

        /**
         * Builds a {@link ChatRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatRequest}
         */
        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }

    @Override
    public String toString() {
        return "ChatRequest [messages=" + messages + ", tools=" + tools + ", parameters=" + parameters
            + ", thinking=" + thinking + ", moderations=" + moderations + "]";
    }
}
