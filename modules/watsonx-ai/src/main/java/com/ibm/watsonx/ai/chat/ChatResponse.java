/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.core.Json;

/**
 * Represents the response from a chat completion request.
 * <p>
 * This class provides methods to access both the assistant's plain response and (optionally) its reasoning process.
 * <p>
 * If {@link ChatRequest.Builder#thinking(ExtractionTags tags)} is enabled, the {@link ExtractionTags} will be used to separate the reasoning
 * ("thinking") part from the assistant's final response.
 * <p>
 * In that case:
 * <ul>
 * <li>{@link #extractContent()} returns only the final response, excluding reasoning</li>
 * <li>{@link #extractThinking()} returns the reasoning text</li>
 * <li>{@link #toAssistantMessage()} always includes only the final response, never reasoning</li>
 * </ul>
 */
public final class ChatResponse {

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
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier of the chat response.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the type of object returned (e.g., "chat.completion").
     *
     * @param object the object type
     */
    public void setObject(String object) {
        this.object = object;
    }

    /**
     * Returns the type of object returned (e.g., "chat.completion").
     *
     * @return the object type
     */
    public String getObject() {
        return object;
    }

    /**
     * Sets the id of the model used to generate the response.
     *
     * @param modelId the model id
     */
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    /**
     * Returns the id of the model used to generate the response.
     *
     * @return the model id
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Sets the model name used to generate the response.
     *
     * @param model the model name
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Returns the model name used to generate the response.
     *
     * @return the model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the list of result choices returned by the model.
     *
     * @param choices a list of {@link ResultChoice}
     */
    public void setChoices(List<ResultChoice> choices) {
        this.choices = choices;
    }

    /**
     * Returns the list of result choices returned by the model.
     *
     * @return a list of {@link ResultChoice}
     */
    public List<ResultChoice> getChoices() {
        return choices;
    }

    /**
     * Sets the Unix timestamp (in seconds) when the response was created.
     *
     * @param created the creation timestamp
     */
    public void setCreated(Long created) {
        this.created = created;
    }

    /**
     * Returns the Unix timestamp (in seconds) when the response was created.
     *
     * @return the creation timestamp
     */
    public Long getCreated() {
        return created;
    }

    /**
     * Sets the version of the model that generated the response.
     *
     * @param modelVersion the model version
     */
    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    /**
     * Returns the version of the model that generated the response.
     *
     * @return the model version
     */
    public String getModelVersion() {
        return modelVersion;
    }

    /**
     * Sets the formatted creation timestamp of the response.
     *
     * @param createdAt the formatted creation time
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the formatted creation timestamp of the response.
     *
     * @return the formatted creation time
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the usage statistics for the response, such as token counts.
     *
     * @param usage usage statistics
     */
    public void setUsage(ChatUsage usage) {
        this.usage = usage;
    }

    /**
     * Returns the usage statistics for the response, such as token counts.
     *
     * @return a {@link ChatUsage} object
     */
    public ChatUsage getUsage() {
        return usage;
    }

    /**
     * Set the {@code ExtractionTags} object used in the {@code toResponse()}, {@code toThinking()} and {@code toAssistantMessage} functions.
     */
    void setExtractionTags(ExtractionTags extractionTags) {
        this.extractionTags = extractionTags;
    }

    /**
     * Extracts the assistant's response text from the chat completion.
     *
     * @return the assistant's response text as plain content
     * @throws RuntimeException if the response is of type "tool_calls" and contains no textual content
     */
    public String extractContent() {
        var resultMessage = choices.get(0).getMessage();
        if (nonNull(resultMessage.toolCalls()))
            throw new RuntimeException("The response is of the type \"tool_calls\" and contains no text");

        if (isNull(extractionTags))
            return resultMessage.content();
        else {
            var content = extractionTags.extractResponse(resultMessage.content());
            return isNull(content) ? resultMessage.content() : content;
        }
    }

    /**
     * Extracts the assistant's reasoning text (if present) from the chat completion.
     *
     * @return the reasoning text, or {@code null} if not available
     */
    public String extractThinking() {
        var resultMessage = choices.get(0).getMessage();

        if (isNull(extractionTags))
            return resultMessage.reasoningContent();
        else
            return extractionTags.extractThinking(resultMessage.content());
    }

    /**
     * Deserializes the textual content of the chat response into a Java object.
     * <p>
     * This method relies on {@link #extractContent()} to retrieve the textual content of the response and attempts to convert it into an instance of
     * the specified class.
     * <p>
     * Note: This method assumes the content is a valid JSON string matching the structure of the given class. If the content is not valid JSON or
     * does not match the structure of {@code clazz}, a parsing exception may be thrown.
     *
     * @param <T> the type of the object to return
     * @param clazz the target class for deserialization
     * @return an instance of {@code clazz} parsed from the response content
     */
    public <T> T toObject(Class<T> clazz) {
        requireNonNull(clazz);
        return Json.fromJson(extractContent(), clazz);
    }

    /**
     * Converts this {@code ChatResponse} into an {@link AssistantMessage}.
     *
     * @return an {@code AssistantMessage} containing the assistant's reply content
     */
    public AssistantMessage toAssistantMessage() {
        var resultMessage = choices.get(0).getMessage();

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
     * Retrieves the finish reason for the current chat response.
     *
     * @return a {@code String} representing the reason why the response generation finished d
     */
    public FinishReason finishReason() {
        var resultMessage = choices.get(0);
        return FinishReason.fromValue(resultMessage.getFinishReason());
    }

    public static class ResultChoice {
        private Integer index;
        private ResultMessage message;
        private String finishReason;

        public ResultChoice() {}

        public ResultChoice(Integer index, ResultMessage message, String finishReason) {
            this.index = index;
            this.message = message;
            this.finishReason = finishReason;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public ResultMessage getMessage() {
            return message;
        }

        public void setMessage(ResultMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public String toString() {
            return "ResultChoice [index=" + index + ", message=" + message + ", finishReason=" + finishReason + "]";
        }
    }

    @Override
    public String toString() {
        return "ChatResponse [id=" + id + ", object=" + object + ", modelId=" + modelId + ", model=" + model + ", choices=" + choices + ", created="
            + created + ", modelVersion=" + modelVersion + ", createdAt=" + createdAt + ", usage=" + usage + "]";
    }
}
