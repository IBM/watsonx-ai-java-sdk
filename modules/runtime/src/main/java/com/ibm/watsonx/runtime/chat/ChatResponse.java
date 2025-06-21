/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.runtime.chat.model.AssistantMessage;
import com.ibm.watsonx.runtime.chat.model.ChatUsage;
import com.ibm.watsonx.runtime.chat.model.ResultMessage;

/**
 * Represents the response from a chat completion request.
 */
public final class ChatResponse {

  public record ResultChoice(Integer index, ResultMessage message, String finishReason) {
  }

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
   * Returns the textual content of chat response, if available.
   * <p>
   * This method retrieves the content of the {@link ResultChoice} only if:
   * <ul>
   * <li>The choices list is not null or empty</li>
   * <li>The finish reason is not {@code "tool_calls"}</li>
   * <li>The message contains a non-null content</li>
   * </ul>
   * A RuntimeException will be thrown if any of these conditions are not met.
   *
   * @return The message content
   */
  public String toText() {


    var assistantMessage = toAssistantMessage();
    if (nonNull(assistantMessage.toolCalls()))
      throw new RuntimeException("The response is of the type \"tool_calls\" and contains no text");

    if (isNull(assistantMessage.content()))
      throw new RuntimeException("The response doesn't contain text");

    return assistantMessage.content();
  }

  /**
   * Deserializes the textual content of the chat response into a Java object.
   * <p>
   * This method relies on {@link #toText()} to retrieve the textual content of the response and attempts to convert it into an instance of the
   * specified class.
   * <p>
   * Note: This method assumes the content is a valid JSON string matching the structure of the given class. If the content is not valid JSON or does
   * not match the structure of {@code clazz}, a parsing exception may be thrown.
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
