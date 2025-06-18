/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.chat;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.Optional;
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
   * If any of these conditions are not met, an empty {@link Optional} is returned.
   *
   * @return an {@code Optional} containing the message content, or empty if not applicable
   */
  public Optional<String> textResponse() {
    if (isNull(choices) || choices.isEmpty())
      return Optional.empty();

    var choice = choices.get(0);
    if (choice.finishReason().equals("tool_calls") || isNull(choice.message()))
      return Optional.empty();

    return Optional.of(choice.message().content());
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
