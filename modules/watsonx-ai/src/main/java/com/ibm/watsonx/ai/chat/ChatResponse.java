/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */


package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.stream.IntStream;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.gateway.ModelGatewayChatResponse;

/**
 * Represents the response from a chat completion request.
 *
 * @see TextChatResponse
 * @see ModelGatewayChatResponse
 */
public class ChatResponse {

    /**
     * Represents a single choice returned by the model within a chat response.
     *
     * @param index the position of this choice in the list of choices
     * @param message the message produced by the model for this choice
     * @param finishReason the reason the model stopped generating tokens for this choice
     */
    public record ResultChoice(Integer index, ResultMessage message, String finishReason) {

        /**
         * Returns a copy of this {@link ResultChoice} with a new result message.
         *
         * @param resultMessage the new result message
         * @return a new {@link ResultChoice} instance
         */
        public ResultChoice withResultMessage(ResultMessage resultMessage) {
            return new ResultChoice(index, resultMessage, finishReason);
        }

        /**
         * Returns a copy of this {@link ResultChoice} with a new finish reason.
         *
         * @param finishReason the new finish reason
         * @return a new {@link ResultChoice} instance
         */
        public ResultChoice withFinishReason(FinishReason finishReason) {
            return new ResultChoice(index, message, finishReason.value());
        }
    }

    private final String id;
    private final String object;
    private final String model;
    private final List<ResultChoice> choices;
    private final Long created;
    private final ChatUsage usage;
    private final ExtractionTags extractionTags;

    protected ChatResponse(Builder<?> builder) {
        id = builder.id;
        object = builder.object;
        model = builder.model;
        choices = isNull(builder.choices) ? null : List.copyOf(builder.choices);
        created = builder.created;
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
     * Returns the usage statistics for the response, such as token counts.
     *
     * @return a {@link ChatUsage} object
     */
    public ChatUsage usage() {
        return usage;
    }

    /**
     * Returns the extraction tags used to parse thinking and response content.
     *
     * @return the extraction tags, or {@code null} if not set
     */
    public ExtractionTags extractionTags() {
        return extractionTags;
    }

    /**
     * Retrieves the finish reason for the current chat response.
     *
     * @return a {@link FinishReason} representing the reason why generation finished
     */
    public FinishReason finishReason() {
        if (isNull(choices) || choices.isEmpty())
            return FinishReason.INCOMPLETE;
        return FinishReason.fromValue(choices.get(0).finishReason());
    }

    /**
     * Converts the {@code ChatResponse} into a list of {@link AssistantMessage}.
     * <p>
     * This method processes all choices in the response and converts each one into an {@code AssistantMessage}. Use this method when the chat request
     * was made with the {@code n} parameter greater than 1 to retrieve multiple alternative responses from the model.
     *
     * @return a list of {@code AssistantMessage} instances, one for each choice in the response
     * @throws EmptyChatResponseException if the response contains no choices, or if any choice contains no content, tool calls or refusal
     * @see #toAssistantMessage()
     */
    public List<AssistantMessage> toAssistantMessages() {

        if (isNull(choices) || choices.isEmpty())
            throw new EmptyChatResponseException(
                "The chat response contains no choices",
                this,
                FinishReason.INCOMPLETE,
                EmptyChatResponseException.NO_CHOICE);

        return IntStream.range(0, choices.size())
            .mapToObj(index -> {

                var choice = choices.get(index);
                var message = choice.message();
                var finishReason = FinishReason.fromValue(choice.finishReason());

                if (isNull(message))
                    throw new EmptyChatResponseException(
                        "The choice at index %s contains no message".formatted(index),
                        this,
                        finishReason,
                        index);

                String content;
                String thinking;

                if (isNull(extractionTags)) {
                    content = message.content();
                    thinking = message.reasoningContent();
                } else {
                    content = extractionTags.extractResponse(message.content());
                    content = isNull(content) ? message.content() : content;
                    thinking = extractionTags.extractThinking(message.content());
                }

                if (isNullOrBlank(content) && isNullOrBlank(message.refusal()) && (isNull(message.toolCalls()) || message.toolCalls().isEmpty()))
                    throw new EmptyChatResponseException(
                        "The model generated no content, tool calls or refusal (finish reason: %s)".formatted(finishReason),
                        this,
                        finishReason,
                        index);

                return new AssistantMessage(
                    content,
                    thinking,
                    null,
                    message.refusal(),
                    message.toolCalls());

            }).toList();
    }

    /**
     * Converts the {@code ChatResponse} into an {@link AssistantMessage}.
     * <p>
     * This method returns the first assistant message from the list of choices. If the chat request was made with the {@code n} parameter greater
     * than 1, use {@link #toAssistantMessages()} instead to retrieve all alternative responses.
     *
     * @return an {@code AssistantMessage} containing the assistant's reply content from the first choice
     * @throws EmptyChatResponseException if the response contains no choices, or if the first choice contains no content, tool calls or refusal
     * @see #toAssistantMessages()
     */
    public AssistantMessage toAssistantMessage() {
        return toAssistantMessages().get(0);
    }

    /**
     * Checks whether the given value is {@code null} or contains only whitespace.
     *
     * @param value the value to check
     * @return {@code true} if the value is {@code null} or blank, {@code false} otherwise
     */
    private static boolean isNullOrBlank(String value) {
        return isNull(value) || value.isBlank();
    }

    /**
     * Creates a builder initialized with the current state of this {@code ChatResponse}.
     *
     * @return a new {@link Builder} instance pre-populated with this response's data
     */
    public Builder<?> toBuilder() {
        return new Builder<>()
            .id(this.id)
            .object(this.object)
            .model(this.model)
            .choices(this.choices)
            .created(this.created)
            .usage(this.usage)
            .extractionTags(this.extractionTags);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Builder class for constructing {@link ChatResponse} instances with configurable parameters.
     *
     * @param <B> the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder<B>> {

        /**
         * Creates a new {@code Builder}.
         */
        public Builder() {}

        private String id;
        private String object;
        private String model;
        private List<ResultChoice> choices;
        private Long created;
        private ChatUsage usage;
        private ExtractionTags extractionTags;

        /**
         * Sets the unique identifier of the chat response.
         *
         * @param id unique identifier
         */
        public B id(String id) {
            this.id = id;
            return (B) this;
        }

        /**
         * Sets the type of object returned (e.g., "chat.completion").
         *
         * @param object the object type
         */
        public B object(String object) {
            this.object = object;
            return (B) this;
        }

        /**
         * Sets the model name used to generate the response.
         *
         * @param model the model name
         */
        public B model(String model) {
            this.model = model;
            return (B) this;
        }

        /**
         * Sets the list of result choices returned by the model.
         *
         * @param choices a list of {@link ResultChoice}
         */
        public B choices(List<ResultChoice> choices) {
            this.choices = choices;
            return (B) this;
        }

        /**
         * Sets the Unix timestamp (in seconds) when the response was created.
         *
         * @param created the creation timestamp
         */
        public B created(Long created) {
            this.created = created;
            return (B) this;
        }

        /**
         * Sets the usage statistics for the response, such as token counts.
         *
         * @param usage usage statistics
         */
        public B usage(ChatUsage usage) {
            this.usage = usage;
            return (B) this;
        }

        /**
         * Sets the extraction tags used to parse thinking and response content.
         *
         * @param extractionTags the extraction tags
         */
        public B extractionTags(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
            return (B) this;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((object == null) ? 0 : object.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((choices == null) ? 0 : choices.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((usage == null) ? 0 : usage.hashCode());
        result = prime * result + ((extractionTags == null) ? 0 : extractionTags.hashCode());
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
        ChatResponse other = (ChatResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (object == null) {
            if (other.object != null)
                return false;
        } else if (!object.equals(other.object))
            return false;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (choices == null) {
            if (other.choices != null)
                return false;
        } else if (!choices.equals(other.choices))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (usage == null) {
            if (other.usage != null)
                return false;
        } else if (!usage.equals(other.usage))
            return false;
        if (extractionTags == null) {
            if (other.extractionTags != null)
                return false;
        } else if (!extractionTags.equals(other.extractionTags))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChatResponse [id=" + id + ", object=" + object + ", model=" + model + ", choices=" + choices + ", created=" + created + ", usage="
            + usage + ", extractionTags=" + extractionTags + "]";
    }
}
