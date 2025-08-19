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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.XmlUtils;

/**
 * Represents the response from a chat completion request.
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
     * Extracts textual content enclosed within the specified XML-like tags from the assistant's response.
     * <p>
     * This method parses the assistant's output as XML (wrapped in a synthetic {@code <root>} tag) and returns the textual content associated with
     * each requested tag.
     * <ul>
     * <li>For normal tags, the returned value is the full text inside the tag, including nested elements' text.</li>
     * <li>For the synthetic {@code root} tag (i.e., the top-level element), only the <b>direct text nodes</b> outside of any child elements are
     * included.</li>
     * </ul>
     * <p>
     * This behavior is particularly useful when working with models that output segmented content using tags such as {@code <think>} or
     * {@code <response>}, and when you need to distinguish between top-level text and text inside nested tags.
     * <p>
     * The input should contain only the tag names (e.g., {@code "think"}, {@code "response"}), not the angle brackets.
     *
     * <p>
     * <b>Example usage:</b>
     * </p>
     *
     * <pre>{@code
     * var tags = Set.of("think", "response", "root");
     * var parts = chatResponse.toTextByTags(tags);
     * String think = parts.get("think");   // text inside <think>...</think>
     * String resp = parts.get("response"); // text inside <response>...</response>
     * String rootText = parts.get("root"); // only direct text outside child tags
     * }</pre>
     *
     * @param tags a set of tag names to extract content from, without angle brackets
     * @return a map where each key is a tag name and its value is the corresponding extracted text
     */
    public Map<String, String> toTextByTags(Set<String> tags) {
        requireNonNull(tags, "tags cannot be null");

        var wrappedXml = "<root>" + toText() + "</root>";

        Document doc = XmlUtils.parse(wrappedXml);
        Element root = doc.getDocumentElement();
        Map<String, String> result = new HashMap<>();

        for (String tag : tags) {

            NodeList nodes = doc.getElementsByTagName(tag);

            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String textContent;
                if (element == root) {
                    StringBuilder sb = new StringBuilder();
                    NodeList children = element.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);

                        if (child.getNodeType() != Node.TEXT_NODE)
                            continue;

                        String text = child.getTextContent().trim();
                        if (!text.isEmpty())
                            sb.append(text);
                    }
                    textContent = sb.isEmpty() ? null : sb.toString();
                } else {
                    textContent = element.getTextContent().trim();
                }

                result.put(tag, textContent);
            }
        }

        return result;
    }

    /**
     * Extracts the textual content enclosed within a single specified XML-like tag from the assistant's response.
     * <p>
     * Behaves like {@link #toTextByTags(Set)} but for a single tag. If the specified tag is the synthetic {@code root} element, only the direct text
     * nodes outside of child tags are included.
     * <p>
     * The input should contain only the tag name (e.g., {@code "think"}), not the angle brackets.
     *
     * <p>
     * <b>Example usage:</b>
     * </p>
     *
     * <pre>{@code
     * String think = instance.toTextByTag("think");
     * }</pre>
     *
     * @param tag the tag name to extract content from, without angle brackets
     * @return the textual content inside the specified tag
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

        var resultMessage = choices.get(0).getMessage();
        return new AssistantMessage(AssistantMessage.ROLE, resultMessage.content(), null, resultMessage.refusal(),
            resultMessage.toolCalls());
    }

    /**
     * Retrieves the finish reason for the current chat response.
     *
     * @return a {@code String} representing the reason why the response generation finished d
     */
    public FinishReason finishReason() {
        if (isNull(choices) || choices.isEmpty())
            throw new RuntimeException("The \"choices\" field is null or empty");

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
    }
}
