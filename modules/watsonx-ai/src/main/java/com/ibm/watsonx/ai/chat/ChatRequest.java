/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.Tool;

/**
 * Represents a chat request sent to the model.
 * <p>
 * Instances are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var tool = Tool.of(
 *     "send_email",
 *     "Send an email",
 *     JsonSchema.builder()
 *         .addStringProperty("email")
 *         .addStringProperty("subject")
 *         .addStringProperty("body")
 *         .required("email", "subject", "body")
 * );
 *
 * var parameters = ChatParameters.builder()
 *     .temperature(0.7)
 *     .maxCompletionTokens(0)
 *     .build();
 *
 * ChatRequest request = ChatRequest.builder()
 *     .messages(
 *         SystemMessage.of("You are a helpful assistant"),
 *         UserMessage.text("Tell me a joke")
 *     )
 *     .tools(tool)
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 */
public class ChatRequest {
    private final List<ChatMessage> messages;
    private final List<Tool> tools;
    private final ChatParameters parameters;
    private final ExtractionTags extractionTags;

    protected ChatRequest(Builder builder) {
        this.messages = requireNonNull(builder.messages, "messages cannot be null");
        if (this.messages.isEmpty())
            throw new RuntimeException("messages cannot be empty");
        this.tools = builder.tools;
        this.parameters = builder.parameters;
        this.extractionTags = builder.extractionTags;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public ChatParameters getParameters() {
        return parameters;
    }

    public ExtractionTags getExtractionTags() {
        return extractionTags;
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
     *     JsonSchema.builder()
     *         .addStringProperty("email")
     *         .addStringProperty("subject")
     *         .addStringProperty("body")
     *         .required("email", "subject", "body")
     * );
     *
     * var parameters = ChatParameters.builder()
     *     .temperature(0.7)
     *     .maxCompletionTokens(0)
     *     .build();
     *
     * ChatRequest request = ChatRequest.builder()
     *     .messages(
     *         SystemMessage.of("You are a helpful assistant"),
     *         UserMessage.text("Tell me a joke")
     *     )
     *     .tools(tool)
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
     * Builder class for constructing {@link ChatRequest} instances.
     */
    public static class Builder {
        private List<ChatMessage> messages;
        private List<Tool> tools;
        private ChatParameters parameters;
        private ExtractionTags extractionTags;

        /**
         * Sets the conversation messages for the request.
         *
         * @param messages list of {@link ChatMessage} objects
         */
        public Builder messages(ChatMessage... messages) {
            return messages(List.of(messages));
        }

        /**
         * Sets the conversation messages for the request.
         *
         * @param messages list of {@link ChatMessage} objects
         */
        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
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
         * Sets the tag names used to extract segmented content from the assistant's output.
         * <p>
         * The provided {@link ExtractionTags} define which XML-like tags (such as {@code <think>} and {@code <response>}) will be used to separate
         * the reasoning portion from the final response.
         * <p>
         * If the {@code response} tag is not specified in {@link ExtractionTags}, the final response will be considered as all the content outside
         * the {@code reasoning} tag.
         * <p>
         * <b>Example</b>
         *
         * <pre>{@code
         * // Explicitly set both tags
         * builder.thinking(ExtractionTags.of("think", "response")).build();
         *
         * // Only set reasoning tag:
         * // the response will be everything outside <think>...</think>
         * builder.thinking(ExtractionTags.of("think")).build();
         * }</pre>
         *
         * @param extractionTags an {@link ExtractionTags} instance containing the reasoning tag and, optionally, the response tag
         */
        public Builder thinking(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
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
