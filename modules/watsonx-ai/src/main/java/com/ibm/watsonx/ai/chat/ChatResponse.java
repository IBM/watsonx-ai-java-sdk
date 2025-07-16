/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.XmlUtils;

/**
 * Represents the response from a chat completion request.
 */
public final class ChatResponse {

    public record ResultChoice(Integer index, ResultMessage message, String finishReason) {}

    private String id;
    private String object;
    private String modelId;
    private String model;
    private List<ResultChoice> choices;
    private Long created;
    private String modelVersion;
    private String createdAt;
    private ChatUsage usage;

    /**
     * Returns the unique identifier of the chat response.
     *
     * @return id
     */
    public String getId() {
        return id;
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
     * Returns the id of the model used to generate the response.
     *
     * @return the model id
     */
    public String getModelId() {
        return modelId;
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
     * Returns the list of result choices returned by the model.
     *
     * @return a list of {@link ResultChoice}
     */
    public List<ResultChoice> getChoices() {
        return choices;
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
     * Returns the version of the model that generated the response.
     *
     * @return the model version
     */
    public String getModelVersion() {
        return modelVersion;
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
     * Returns the usage statistics for the response, such as token counts.
     *
     * @return a {@link ChatUsage} object
     */
    public ChatUsage getUsage() {
        return usage;
    }

    /**
     * Returns the textual content of the assistant's chat response, if available.
     * <p>
     * This method retrieves the content of the {@link ResultChoice} only if:
     * <ul>
     * <li>The choices list is not null or empty</li>
     * <li>The finish reason is not {@code tool_calls}</li>
     * </ul>
     * A {@link RuntimeException} is thrown if any of these conditions are not met.
     *
     * @return the assistant's message content as plain text
     */
    public String toText() {

        var assistantMessage = toAssistantMessage();
        if (nonNull(assistantMessage.toolCalls()))
            throw new RuntimeException("The response is of the type \"tool_calls\" and contains no text");

        return assistantMessage.content();
    }

    /**
     * Extracts the textual content enclosed within the specified XML-like tags from the assistant's response.
     * <p>
     * This method is particularly useful when working with models that output segmented content using tags such as {@code <think>} or
     * {@code <response>}. The input should contain only the tag names (e.g., {@code "think"}, {@code "response"}), not the angle brackets.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var tags = Set.of("think", "response");
     * var parts = instance.toTextByTags(tags);
     * String think = parts.get("think");
     * }</pre>
     *
     *
     * @param tags a set of tag names to extract content from, without angle brackets
     * @return a map where each key is a tag name and its value is the corresponding extracted text
     */
    public Map<String, String> toTextByTags(Set<String> tags) {
        requireNonNull(tags, "tags cannot be null");

        var wrappedXml = "<root>" + toText() + "</root>";

        Document doc = XmlUtils.parse(wrappedXml);
        Map<String, String> result = new HashMap<>();

        for (String tag : tags) {
            NodeList nodes = doc.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String textContent = element.getTextContent().trim();
                result.put(tag, textContent);
            }
        }

        return result;
    }

    /**
     * Extracts the textual content enclosed within a single specified XML-like tag from the assistant's response.
     * <p>
     * This method is particularly useful when working with models that output segmented content using tags such as {@code <think>} or
     * {@code <response>}. The input should contain only the tag names (e.g., {@code "think"}, {@code "response"}), not the angle brackets.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>
     * String response = instance.toTextByTag("response");
     * </pre>
     *
     * @param tag the tag name to extract content from, without angle brackets
     * @return the textual content inside the specified tag, or {@code null} if not present
     * @throws RuntimeException if the underlying text is not valid XML or parsing fails
     */
    public String toTextByTag(String tag) {
        return toTextByTags(Set.of(tag)).get(tag);
    }

    /**
     * Deserializes the textual content of the chat response into a Java object.
     * <p>
     * This method relies on {@link #toText()} to retrieve the textual content of the response and attempts to convert it into an instance of the
     * specified class.
     * <p>
     * Note: This method assumes the content is a valid JSON string matching the structure of the given class. If the content is not valid JSON or
     * does not match the structure of {@code clazz}, a parsing exception may be thrown.
     *
     * @param <T> the type of the object to return
     * @param clazz the target class for deserialization
     * @return an instance of {@code clazz} parsed from the response content
     */
    public <T> T toText(Class<T> clazz) {
        requireNonNull(clazz);
        return Json.fromJson(toText(), clazz);
    }

    /**
     * Converts the content of the response into an {@link AssistantMessage}.
     *
     * @return an {@code AssistantMessage} representing the assistant's reply
     */
    public AssistantMessage toAssistantMessage() {
        if (isNull(choices) || choices.isEmpty())
            throw new RuntimeException("The \"choices\" field is null or empty");

        var resultMessage = choices.get(0).message();
        return new AssistantMessage(AssistantMessage.ROLE, resultMessage.content(), null, resultMessage.refusal(),
            resultMessage.toolCalls());
    }

    void setId(String id) {
        this.id = id;
    }

    void setObject(String object) {
        this.object = object;
    }

    void setModelId(String modelId) {
        this.modelId = modelId;
    }

    void setModel(String model) {
        this.model = model;
    }

    void setChoices(List<ResultChoice> choices) {
        this.choices = choices;
    }

    void setCreated(Long created) {
        this.created = created;
    }

    void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    void setUsage(ChatUsage usage) {
        this.usage = usage;
    }
}
