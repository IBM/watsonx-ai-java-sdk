/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ResponseFormat;
import com.ibm.watsonx.ai.chat.model.Tool;

/**
 * Represents a chat request to a conversational model.
 */
public final class ChatRequest {

    private final String modelId;
    private final String spaceId;
    private final String projectId;
    private final List<ChatMessage> messages;
    private final List<Tool> tools;
    private final String toolChoiceOption;
    private final Map<String, Object> toolChoice;
    private final Double frequencyPenalty;
    private final Map<String, Integer> logitBias;
    private final Boolean logprobs;
    private final Integer topLogprobs;
    private final Integer maxCompletionTokens;
    private final Integer n;
    private final Double presencePenalty;
    private final Integer seed;
    private final List<String> stop;
    private final Double temperature;
    private final Double topP;
    private final Long timeLimit;
    private final Map<String, Object> responseFormat;
    private final String context;

    public ChatRequest(Builder builder) {
        this.modelId = builder.modelId;
        this.spaceId = builder.spaceId;
        this.projectId = builder.projectId;
        this.messages = builder.messages;
        this.tools = builder.tools;
        this.toolChoiceOption = builder.toolChoiceOption;
        this.toolChoice = builder.toolChoice;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.logitBias = builder.logitBias;
        this.logprobs = builder.logprobs;
        this.topLogprobs = builder.topLogprobs;
        this.maxCompletionTokens = builder.maxCompletionTokens;
        this.n = builder.n;
        this.presencePenalty = builder.presencePenalty;
        this.seed = builder.seed;
        this.stop = builder.stop;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.timeLimit = builder.timeLimit;
        this.context = builder.context;

        if (nonNull(builder.responseFormat)) {
            this.responseFormat = builder.responseFormat.equals(ResponseFormat.JSON_SCHEMA.type())
                ? Map.of("type", builder.responseFormat, "json_schema", builder.jsonSchema)
                : Map.of("type", builder.responseFormat);
        } else {
            this.responseFormat = null;
        }
    }

    public String getModelId() {
        return modelId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public String getToolChoiceOption() {
        return toolChoiceOption;
    }

    public Map<String, Object> getToolChoice() {
        return toolChoice;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public Map<String, Integer> getLogitBias() {
        return logitBias;
    }

    public Boolean getLogprobs() {
        return logprobs;
    }

    public Integer getTopLogprobs() {
        return topLogprobs;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public Integer getN() {
        return n;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public Integer getSeed() {
        return seed;
    }

    public List<String> getStop() {
        return stop;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public Map<String, Object> getResponseFormat() {
        return responseFormat;
    }

    public String getContext() {
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelId;
        private String spaceId;
        private String projectId;
        private List<ChatMessage> messages;
        private List<Tool> tools;
        private String toolChoiceOption;
        private Map<String, Object> toolChoice;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private Boolean logprobs;
        private Integer topLogprobs;
        private Integer maxCompletionTokens;
        private Integer n;
        private Double presencePenalty;
        private Integer seed;
        private List<String> stop;
        private Double temperature;
        private Double topP;
        private Long timeLimit;
        private String responseFormat;
        private JsonSchemaObject jsonSchema;
        private String context;

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder timeLimit(Long timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }

        public Builder parameters(ChatParameters parameters) {
            this.toolChoiceOption = parameters.getToolChoiceOption();
            this.toolChoice = parameters.getToolChoice();
            this.frequencyPenalty = parameters.getFrequencyPenalty();
            this.logitBias = parameters.getLogitBias();
            this.logprobs = parameters.getLogprobs();
            this.topLogprobs = parameters.getTopLogprobs();
            this.maxCompletionTokens = parameters.getMaxCompletionTokens();
            this.n = parameters.getN();
            this.presencePenalty = parameters.getPresencePenalty();
            this.seed = parameters.getSeed();
            this.stop = parameters.getStop();
            this.temperature = parameters.getTemperature();
            this.topP = parameters.getTopP();
            this.responseFormat = parameters.getResponseFormat();
            this.jsonSchema = parameters.getJsonSchema();
            this.context = parameters.getContext();
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }
}
