/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ibm.watsonx.ai.Crypto;
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
    private final Double repetitionPenalty;
    private final Double lengthPenalty;
    private final String context;
    private final Crypto crypto;

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
            responseFormat = builder.responseFormat.equals(ResponseFormat.JSON_SCHEMA.value())
                ? Map.of("type", builder.responseFormat, "json_schema", builder.jsonSchema)
                : Map.of("type", builder.responseFormat);
        } else {
            responseFormat = null;
        }

        guidedChoice = builder.guidedChoice;
        guidedRegex = builder.guidedRegex;
        guidedGrammar = builder.guidedGrammar;
        repetitionPenalty = builder.repetitionPenalty;
        lengthPenalty = builder.lengthPenalty;
        crypto = nonNull(builder.crypto) ? new Crypto(builder.crypto) : null;
    }

    public String modelId() {
        return modelId;
    }

    public String spaceId() {
        return spaceId;
    }

    public String projectId() {
        return projectId;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public List<Tool> tools() {
        return tools;
    }

    public String toolChoiceOption() {
        return toolChoiceOption;
    }

    public Map<String, Object> toolChoice() {
        return toolChoice;
    }

    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    public Map<String, Integer> logitBias() {
        return logitBias;
    }

    public Boolean logprobs() {
        return logprobs;
    }

    public Integer topLogprobs() {
        return topLogprobs;
    }

    public Integer maxCompletionTokens() {
        return maxCompletionTokens;
    }

    public Integer n() {
        return n;
    }

    public Double presencePenalty() {
        return presencePenalty;
    }

    public Integer seed() {
        return seed;
    }

    public List<String> stop() {
        return stop;
    }

    public Double temperature() {
        return temperature;
    }

    public Double topP() {
        return topP;
    }

    public Long timeLimit() {
        return timeLimit;
    }

    public Map<String, Object> responseFormat() {
        return responseFormat;
    }

    public String context() {
        return context;
    }

    public Map<String, Object> chatTemplateKwargs() {
        return chatTemplateKwargs;
    }

    public Boolean includeReasoning() {
        return includeReasoning;
    }

    public String reasoningEffort() {
        return reasoningEffort;
    }

    public Set<String> guidedChoice() {
        return guidedChoice;
    }

    public String guidedRegex() {
        return guidedRegex;
    }

    public String guidedGrammar() {
        return guidedGrammar;
    }

    public Double repetitionPenalty() {
        return repetitionPenalty;
    }

    public Double lengthPenalty() {
        return lengthPenalty;
    }

    public Crypto crypto() {
        return crypto;
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
        private Double repetitionPenalty;
        private Double lengthPenalty;
        private String context;
        private String crypto;

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

        public Builder toolChoiceOption(String toolChoiceOption) {
            this.toolChoiceOption = toolChoiceOption;
            return this;
        }

        public Builder toolChoice(Map<String, Object> toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder timeLimit(Long timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder chatTemplateKwargs(Map<String, Object> chatTemplateKwargs) {
            this.chatTemplateKwargs = chatTemplateKwargs;
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

        public Builder guidedChoice(Set<String> guidedChoice) {
            this.guidedChoice = guidedChoice;
            return this;
        }

        public Builder guidedRegex(String guidedRegex) {
            this.guidedRegex = guidedRegex;
            return this;
        }

        public Builder guidedGrammar(String guidedGrammar) {
            this.guidedGrammar = guidedGrammar;
            return this;
        }

        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        public Builder lengthPenalty(Double lengthPenalty) {
            this.lengthPenalty = lengthPenalty;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder jsonSchema(JsonSchemaObject jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public Builder crypto(String crypto) {
            this.crypto = crypto;
            return this;
        }

        public TextChatRequest build() {
            return new TextChatRequest(this);
        }
    }
}
