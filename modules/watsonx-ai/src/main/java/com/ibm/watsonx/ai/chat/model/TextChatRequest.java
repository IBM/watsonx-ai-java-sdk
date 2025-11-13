/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ibm.watsonx.ai.chat.model.ChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ResponseFormat;

/**
 * Represents a chat request to used by the watsonx.ai API.
 */
public final class TextChatRequest {

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
    private final Map<String, Object> chatTemplateKwargs;
    private final Boolean includeReasoning;
    private final String reasoningEffort;
    private final Set<String> guidedChoice;
    private final String guidedRegex;
    private final String guidedGrammar;
    private final String context;

    private TextChatRequest(Builder builder) {
        modelId = builder.modelId;
        spaceId = builder.spaceId;
        projectId = builder.projectId;
        messages = builder.messages;
        tools = builder.tools;
        toolChoiceOption = builder.toolChoiceOption;
        toolChoice = builder.toolChoice;
        frequencyPenalty = builder.frequencyPenalty;
        logitBias = builder.logitBias;
        logprobs = builder.logprobs;
        topLogprobs = builder.topLogprobs;
        maxCompletionTokens = builder.maxCompletionTokens;
        n = builder.n;
        presencePenalty = builder.presencePenalty;
        seed = builder.seed;
        stop = builder.stop;
        temperature = builder.temperature;
        topP = builder.topP;
        timeLimit = builder.timeLimit;
        context = builder.context;
        chatTemplateKwargs = builder.chatTemplateKwargs;
        includeReasoning = builder.includeReasoning;
        reasoningEffort = builder.reasoningEffort;

        if (nonNull(builder.responseFormat)) {
            responseFormat = builder.responseFormat.equals(ResponseFormat.JSON_SCHEMA.type())
                ? Map.of("type", builder.responseFormat, "json_schema", builder.jsonSchema)
                : Map.of("type", builder.responseFormat);
        } else {
            responseFormat = null;
        }

        guidedChoice = builder.guidedChoice;
        guidedRegex = builder.guidedRegex;
        guidedGrammar = builder.guidedGrammar;
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

    public Map<String, Object> getChatTemplateKwargs() {
        return chatTemplateKwargs;
    }

    public Boolean getIncludeReasoning() {
        return includeReasoning;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public Set<String> getGuidedChoice() {
        return guidedChoice;
    }

    public String getGuidedRegex() {
        return guidedRegex;
    }

    public String getGuidedGrammar() {
        return guidedGrammar;
    }

    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder {
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
        private Map<String, Object> chatTemplateKwargs;
        private Boolean includeReasoning;
        private String reasoningEffort;
        private JsonSchemaObject jsonSchema;
        private Set<String> guidedChoice;
        private String guidedRegex;
        private String guidedGrammar;

        private String context;

        private Builder() {}

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

        public Builder includeReasoning(Boolean includeReasoning) {
            this.includeReasoning = includeReasoning;
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public Builder chatTemplateKwargs(Map<String, Object> chatTemplateKwargs) {
            this.chatTemplateKwargs = chatTemplateKwargs;
            return this;
        }

        public Builder parameters(ChatParameters parameters) {
            toolChoiceOption = parameters.getToolChoiceOption();
            toolChoice = parameters.getToolChoice();
            frequencyPenalty = parameters.getFrequencyPenalty();
            logitBias = parameters.getLogitBias();
            logprobs = parameters.getLogprobs();
            topLogprobs = parameters.getTopLogprobs();
            maxCompletionTokens = parameters.getMaxCompletionTokens();
            n = parameters.getN();
            presencePenalty = parameters.getPresencePenalty();
            seed = parameters.getSeed();
            stop = parameters.getStop();
            temperature = parameters.getTemperature();
            topP = parameters.getTopP();
            responseFormat = parameters.getResponseFormat();
            jsonSchema = parameters.getJsonSchema();
            context = parameters.getContext();
            timeLimit = parameters.getTimeLimit();
            guidedChoice = parameters.getGuidedChoice();
            guidedRegex = parameters.getGuidedRegex();
            guidedGrammar = parameters.getGuidedGrammar();
            return this;
        }

        public TextChatRequest build() {
            return new TextChatRequest(this);
        }
    }
}
