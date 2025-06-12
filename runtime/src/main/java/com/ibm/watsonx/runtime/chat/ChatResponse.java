package com.ibm.watsonx.runtime.chat;

import java.util.List;
import com.ibm.watsonx.runtime.chat.model.ChatUsage;
import com.ibm.watsonx.runtime.chat.model.ResultMessage;

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

    public String getId() {
        return id;
    }

    public String getObject() {
        return object;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModel() {
        return model;
    }

    public List<ResultChoice> getChoices() {
        return choices;
    }

    public Long getCreated() {
        return created;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public ChatUsage getUsage() {
        return usage;
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
