/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ResultMessage;

/**
 * Represents the response from a chat completion request.
 */
public final class ChatResponse {

    public static record ResultChoice(Integer index, ResultMessage message, String finishReason) {

        /**
         * Returns a copy of this {@link ResultChoice} with a result message.
         *
         * @param newResultMessage the new result message
         * @return a new {@link ResultChoice} instance
         */
        public ResultChoice withResultMessage(ResultMessage newResultMessage) {
            return new ResultChoice(index, newResultMessage, finishReason);
        }
    }

    private final String id;
    private final String object;
    private final String modelId;
    private final String model;
    private final List<ResultChoice> choices;
    private final Long created;
    private final String modelVersion;
    private final String createdAt;
    private final ChatUsage usage;
    private final ExtractionTags extractionTags;

    private ChatResponse(Builder builder) {
        id = builder.id;
        object = builder.object;
        modelId = builder.modelId;
        model = builder.model;
        choices = List.copyOf(builder.choices);
        created = builder.created;
        modelVersion = builder.modelVersion;
        createdAt = builder.createdAt;
        usage = builder.usage;
        extractionTags = builder.extractionTags;
    }

    /**
     * Returns the unique identifier of the chat response.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the type of object returned (e.g., "chat.completion").
     *
     * @return the object type
     */
    public String object() {
        return object;
    }

    /**
     * Returns the id of the model used to generate the response.
     *
     * @return the model id
     */
    public String modelId() {
        return modelId;
    }

    /**
     * Returns the model name used to generate the response.
     *
     * @return the model name
     */
    public String model() {
        return model;
    }

    /**
     * Returns the list of result choices returned by the model.
     *
     * @return a list of {@link ResultChoice}
     */
    public List<ResultChoice> choices() {
        return choices;
    }

    /**
     * Returns the Unix timestamp (in seconds) when the response was created.
     *
     * @return the creation timestamp
     */
    public Long created() {
        return created;
    }

    /**
     * Returns the version of the model that generated the response.
     *
     * @return the model version
     */
    public String modelVersion() {
        return modelVersion;
    }

    /**
     * Returns the formatted creation timestamp of the response.
     *
     * @return the formatted creation time
     */
    public String createdAt() {
        return createdAt;
    }

    /**
     * Returns the usage statistics for the response, such as token counts.
     *
     * @return a {@link ChatUsage} object
     */
    public ChatUsage usage() {
        return usage;
    }

    /**
     * Retrieves the finish reason for the current chat response.
     *
     * @return a {@code String} representing the reason why the response generation finished d
     */
    public FinishReason finishReason() {
        var resultMessage = choices.get(0);
        return FinishReason.fromValue(resultMessage.finishReason());
    }

    /**
     * Converts this {@code ChatResponse} into an {@link AssistantMessage}.
     *
     * @return an {@code AssistantMessage} containing the assistant's reply content
     */
    public AssistantMessage toAssistantMessage() {
        var resultMessage = choices.get(0).message();

        String content;
        String thinking;
        if (isNull(extractionTags)) {
            content = resultMessage.content();
            thinking = resultMessage.reasoningContent();
        } else {
            content = extractionTags.extractResponse(resultMessage.content());
            content = isNull(content) ? resultMessage.content() : content;
            thinking = extractionTags.extractThinking(resultMessage.content());
        }

        return new AssistantMessage(
            content,
            thinking,
            null,
            resultMessage.refusal(),
            resultMessage.toolCalls());
    }

    /**
     * Creates a builder initialized with the current state of the ChatResponse.
     *
     * @return a new {@link Builder} instance pre-populated with this response's data
     */
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .object(this.object)
            .modelId(this.modelId)
            .model(this.model)
            .choices(this.choices)
            .created(this.created)
            .modelVersion(this.modelVersion)
            .createdAt(this.createdAt)
            .usage(this.usage)
            .extractionTags(this.extractionTags);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder build() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatResponse} instances with configurable parameters.
     */
    public static class Builder {
        private String id;
        private String object;
        private String modelId;
        private String model;
        private List<ResultChoice> choices;
        private Long created;
        private String modelVersion;
        private String createdAt;
        private ChatUsage usage;
        private ExtractionTags extractionTags;

        /**
         * Sets the unique identifier of the chat response.
         *
         * @param id unique identifier
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the type of object returned (e.g., "chat.completion").
         *
         * @param object the object type
         */
        public Builder object(String object) {
            this.object = object;
            return this;
        }

        /**
         * Sets the id of the model used to generate the response.
         *
         * @param modelId the model id
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Sets the model name used to generate the response.
         *
         * @param model the model name
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the list of result choices returned by the model.
         *
         * @param choices a list of {@link ResultChoice}
         */
        public Builder choices(List<ResultChoice> choices) {
            this.choices = choices;
            return this;
        }

        /**
         * Sets the Unix timestamp (in seconds) when the response was created.
         *
         * @param created the creation timestamp
         */
        public Builder created(Long created) {
            this.created = created;
            return this;
        }

        /**
         * Sets the version of the model that generated the response.
         *
         * @param modelVersion the model version
         */
        public Builder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        /**
         * Sets the formatted creation timestamp of the response.
         *
         * @param createdAt the formatted creation time
         */
        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the usage statistics for the response, such as token counts.
         *
         * @param usage usage statistics
         */
        public Builder usage(ChatUsage usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Set the {@code ExtractionTags}.
         */
        Builder extractionTags(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
            return this;
        }

        /**
         * Builds a {@link ChatResponse} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatResponse}
         */
        public ChatResponse build() {
            return new ChatResponse(this);
        }
    }

    @Override
    public String toString() {
        return "ChatResponse [id=" + id + ", object=" + object + ", modelId=" + modelId + ", model=" + model + ", choices=" + choices + ", created="
            + created + ", modelVersion=" + modelVersion + ", createdAt=" + createdAt + ", usage=" + usage + "]";
    }
}
