/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.Thinking;
import com.ibm.watsonx.ai.chat.model.ThinkingEffort;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Represents a chat request.
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
 */
public final class ChatRequest {
    private final String deploymentId;
    private final List<ChatMessage> messages;
    private final List<Tool> tools;
    private final ChatParameters parameters;
    private final Thinking thinking;

    private ChatRequest(Builder builder) {
        messages = requireNonNull(builder.messages, "messages cannot be null");
        tools = builder.tools;
        parameters = builder.parameters;
        deploymentId = builder.deploymentId;
        thinking = builder.thinking;
    }

    public String deploymentId() {
        return deploymentId;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public List<Tool> tools() {
        return tools;
    }

    public ChatParameters parameters() {
        return parameters;
    }

    public Thinking thinking() {
        return thinking;
    }

    /**
     * Returns a new {@link Builder} instance.
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
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatRequest} instances.
     */
    public final static class Builder {
        private String deploymentId;
        private List<ChatMessage> messages;
        private List<Tool> tools;
        private ChatParameters parameters;
        private Thinking thinking;

        private Builder() {}

        /**
         * Sets the deployment identifier for the chat request.
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
         * Sets the conversation messages for the request, replacing any existing messages.
         * <p>
         * This method completely overwrites the current list of messages with the provided ones.
         * <p>
         * Use {@link #addMessages(ChatMessage...)} or {@link #addMessages(List)} to append messages instead.
         *
         * @param messages one or more {@link ChatMessage} objects to set
         */
        public Builder messages(ChatMessage... messages) {
            return messages(Arrays.asList(messages));
        }

        /**
         * Sets the conversation messages for the request, replacing any existing messages.
         * <p>
         * This method completely overwrites the current list of messages with the provided ones.
         * <p>
         * Use {@link #addMessages(ChatMessage...)} or {@link #addMessages(List)} to append messages instead.
         *
         * @param messages one or more {@link ChatMessage} objects to set
         */
        public Builder messages(List<ChatMessage> messages) {
            if (nonNull(messages))
                this.messages = new LinkedList<>(messages);
            return this;
        }

        /**
         * Adds one or more messages to the existing list of messages for the chat request.
         * <p>
         * Unlike {@link #messages(ChatMessage...)}, which replaces the current list of messages, this method appends the provided messages to the
         * existing list.
         *
         * @param messages one or more {@link ChatMessage} objects to add
         */
        public Builder addMessages(ChatMessage... messages) {
            return addMessages(Arrays.asList(messages));
        }

        /**
         * Adds one or more messages to the existing list of messages for the chat request.
         * <p>
         * Unlike {@link #messages(ChatMessage...)}, which replaces the current list of messages, this method appends the provided messages to the
         * existing list.
         *
         * @param messages one or more {@link ChatMessage} objects to add
         */
        public Builder addMessages(List<ChatMessage> messages) {
            if (isNull(messages) || messages.isEmpty())
                return this;

            this.messages = requireNonNullElse(this.messages, new LinkedList<>());
            this.messages.addAll(messages);
            return this;
        }

        /**
         * Sets the tools available for invocation by the model.
         *
         * @param tools list of {@link Tool} objects
         */
        public Builder tools(Tool... tools) {
            return tools(List.of(tools));
        }

        /**
         * Sets the tools available for invocation by the model.
         *
         * @param tools list of {@link Tool} objects
         */
        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        /**
         * Sets the parameters controlling the chat model's behavior.
         *
         * @param parameters a {@link ChatParameters} instance
         */
        public Builder parameters(ChatParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Enables or disables reasoning for the chat request.
         * <p>
         * This method provides a simple way to toggle reasoning behavior without specifying any particular configuration. When {@code true},
         * reasoning is enabled using the default {@link Thinking} settings. When {@code false}, reasoning is disabled entirely.
         *
         * @param enabled {@code true} to enable reasoning with default settings, {@code false} to disable reasoning
         */
        public Builder thinking(boolean enabled) {
            return thinking(Thinking.builder().enabled(enabled).build());
        }

        /**
         * Sets the reasoning extraction tags for the chat request.
         * <p>
         * This method is intended for models that return reasoning and response content within the same text string. The provided
         * {@link ExtractionTags} define which XML-like tags (for example, {@code <think>} and {@code <response>}) should be used to automatically
         * extract the reasoning and response segments.
         *
         * <p>
         * Equivalent to calling:
         *
         * <pre>{@code
         * builder.thinking(Thinking.of(tags));
         * }</pre>
         *
         * @param tags an {@link ExtractionTags} instance defining the reasoning and response tags
         */
        public Builder thinking(ExtractionTags tags) {
            if (isNull(tags)) {
                thinking = null;
                return this;
            }

            return thinking(Thinking.of(tags));
        }

        /**
         * Sets the reasoning effort for the chat request.
         * <p>
         * The provided {@link ThinkingEffort} controls how much reasoning the model applies when generating a response. This method should be used
         * with models that already separate reasoning and response automatically.
         *
         * <p>
         * Equivalent to calling:
         *
         * <pre>{@code
         * builder.thinking(Thinking.of(ThinkingEffort));
         * }</pre>
         *
         * @param thinkingEffort the desired {@link ThinkingEffort} level
         */
        public Builder thinking(ThinkingEffort thinkingEffort) {
            if (isNull(thinkingEffort)) {
                thinking = null;
                return this;
            }

            return thinking(Thinking.of(thinkingEffort));
        }

        /**
         * Sets the reasoning configuration for the chat request.
         * <p>
         * The provided {@link Thinking} instance defines how the LLM should handle reasoning output.
         * <p>
         * If the {@link Thinking} instance includes {@link ExtractionTags}, they will be used to automatically extract reasoning and response
         * segments from models that return both parts within a single text string (for example, models in the <b>ibm/granite-3-3-8b-instruct</b>).
         * <p>
         * If {@link ExtractionTags} are omitted, the model is assumed to already provide reasoning and response as separate fields.
         *
         * @param thinking a {@link Thinking} configuration defining how reasoning output is extracted and the level of reasoning effort
         *
         */
        public Builder thinking(Thinking thinking) {
            this.thinking = thinking;
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
}
