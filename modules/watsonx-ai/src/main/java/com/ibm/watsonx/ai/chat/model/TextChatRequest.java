/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ibm.watsonx.ai.Crypto;
import com.ibm.watsonx.ai.chat.ChatModeration;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ResponseFormat;

/**
 * Represents a chat request to used by the watsonx.ai API.
 */
public final class TextChatRequest {

    private final String modelId;
    private final String model;
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
    private final ChatModeration moderations;
    private final Crypto crypto;

    private TextChatRequest(Builder builder) {
        modelId = builder.modelId;
        model = builder.modelId;
        spaceId = builder.spaceId;
        projectId = builder.projectId;
        messages = isNull(builder.messages) ? null : List.copyOf(builder.messages);
        tools = isNull(builder.tools) ? null : List.copyOf(builder.tools);
        toolChoiceOption = builder.toolChoiceOption;
        toolChoice = isNull(builder.toolChoice) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.toolChoice));
        frequencyPenalty = builder.frequencyPenalty;
        logitBias = isNull(builder.logitBias) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.logitBias));
        logprobs = builder.logprobs;
        topLogprobs = builder.topLogprobs;
        maxCompletionTokens = builder.maxCompletionTokens;
        n = builder.n;
        presencePenalty = builder.presencePenalty;
        seed = builder.seed;
        stop = isNull(builder.stop) ? null : List.copyOf(builder.stop);
        temperature = builder.temperature;
        topP = builder.topP;
        timeLimit = builder.timeLimit;
        context = builder.context;
        chatTemplateKwargs = isNull(builder.chatTemplateKwargs) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.chatTemplateKwargs));
        includeReasoning = builder.includeReasoning;
        reasoningEffort = builder.reasoningEffort;

        if (nonNull(builder.responseFormat)) {
            responseFormat = builder.responseFormat.equals(ResponseFormat.JSON_SCHEMA.value())
                ? Map.of("type", builder.responseFormat, "json_schema", builder.jsonSchema)
                : Map.of("type", builder.responseFormat);
        } else {
            responseFormat = null;
        }

        guidedChoice = isNull(builder.guidedChoice) ? null : Collections.unmodifiableSet(new LinkedHashSet<>(builder.guidedChoice));
        guidedRegex = builder.guidedRegex;
        guidedGrammar = builder.guidedGrammar;
        repetitionPenalty = builder.repetitionPenalty;
        lengthPenalty = builder.lengthPenalty;
        moderations = builder.moderations;
        crypto = nonNull(builder.crypto) ? new Crypto(builder.crypto) : null;
    }

    public String modelId() {
        return modelId;
    }

    public String model() {
        return model;
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

    public ChatModeration moderations() {
        return moderations;
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
        private ChatModeration moderations;
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

        public Builder moderations(ChatModeration moderations) {
            this.moderations = moderations;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((spaceId == null) ? 0 : spaceId.hashCode());
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((messages == null) ? 0 : messages.hashCode());
        result = prime * result + ((tools == null) ? 0 : tools.hashCode());
        result = prime * result + ((toolChoiceOption == null) ? 0 : toolChoiceOption.hashCode());
        result = prime * result + ((toolChoice == null) ? 0 : toolChoice.hashCode());
        result = prime * result + ((frequencyPenalty == null) ? 0 : frequencyPenalty.hashCode());
        result = prime * result + ((logitBias == null) ? 0 : logitBias.hashCode());
        result = prime * result + ((logprobs == null) ? 0 : logprobs.hashCode());
        result = prime * result + ((topLogprobs == null) ? 0 : topLogprobs.hashCode());
        result = prime * result + ((maxCompletionTokens == null) ? 0 : maxCompletionTokens.hashCode());
        result = prime * result + ((n == null) ? 0 : n.hashCode());
        result = prime * result + ((presencePenalty == null) ? 0 : presencePenalty.hashCode());
        result = prime * result + ((seed == null) ? 0 : seed.hashCode());
        result = prime * result + ((stop == null) ? 0 : stop.hashCode());
        result = prime * result + ((temperature == null) ? 0 : temperature.hashCode());
        result = prime * result + ((topP == null) ? 0 : topP.hashCode());
        result = prime * result + ((timeLimit == null) ? 0 : timeLimit.hashCode());
        result = prime * result + ((responseFormat == null) ? 0 : responseFormat.hashCode());
        result = prime * result + ((chatTemplateKwargs == null) ? 0 : chatTemplateKwargs.hashCode());
        result = prime * result + ((includeReasoning == null) ? 0 : includeReasoning.hashCode());
        result = prime * result + ((reasoningEffort == null) ? 0 : reasoningEffort.hashCode());
        result = prime * result + ((guidedChoice == null) ? 0 : guidedChoice.hashCode());
        result = prime * result + ((guidedRegex == null) ? 0 : guidedRegex.hashCode());
        result = prime * result + ((guidedGrammar == null) ? 0 : guidedGrammar.hashCode());
        result = prime * result + ((repetitionPenalty == null) ? 0 : repetitionPenalty.hashCode());
        result = prime * result + ((lengthPenalty == null) ? 0 : lengthPenalty.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((moderations == null) ? 0 : moderations.hashCode());
        result = prime * result + ((crypto == null) ? 0 : crypto.hashCode());
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
        TextChatRequest other = (TextChatRequest) obj;
        if (modelId == null) {
            if (other.modelId != null)
                return false;
        } else if (!modelId.equals(other.modelId))
            return false;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (spaceId == null) {
            if (other.spaceId != null)
                return false;
        } else if (!spaceId.equals(other.spaceId))
            return false;
        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId))
            return false;
        if (messages == null) {
            if (other.messages != null)
                return false;
        } else if (!messages.equals(other.messages))
            return false;
        if (tools == null) {
            if (other.tools != null)
                return false;
        } else if (!tools.equals(other.tools))
            return false;
        if (toolChoiceOption == null) {
            if (other.toolChoiceOption != null)
                return false;
        } else if (!toolChoiceOption.equals(other.toolChoiceOption))
            return false;
        if (toolChoice == null) {
            if (other.toolChoice != null)
                return false;
        } else if (!toolChoice.equals(other.toolChoice))
            return false;
        if (frequencyPenalty == null) {
            if (other.frequencyPenalty != null)
                return false;
        } else if (!frequencyPenalty.equals(other.frequencyPenalty))
            return false;
        if (logitBias == null) {
            if (other.logitBias != null)
                return false;
        } else if (!logitBias.equals(other.logitBias))
            return false;
        if (logprobs == null) {
            if (other.logprobs != null)
                return false;
        } else if (!logprobs.equals(other.logprobs))
            return false;
        if (topLogprobs == null) {
            if (other.topLogprobs != null)
                return false;
        } else if (!topLogprobs.equals(other.topLogprobs))
            return false;
        if (maxCompletionTokens == null) {
            if (other.maxCompletionTokens != null)
                return false;
        } else if (!maxCompletionTokens.equals(other.maxCompletionTokens))
            return false;
        if (n == null) {
            if (other.n != null)
                return false;
        } else if (!n.equals(other.n))
            return false;
        if (presencePenalty == null) {
            if (other.presencePenalty != null)
                return false;
        } else if (!presencePenalty.equals(other.presencePenalty))
            return false;
        if (seed == null) {
            if (other.seed != null)
                return false;
        } else if (!seed.equals(other.seed))
            return false;
        if (stop == null) {
            if (other.stop != null)
                return false;
        } else if (!stop.equals(other.stop))
            return false;
        if (temperature == null) {
            if (other.temperature != null)
                return false;
        } else if (!temperature.equals(other.temperature))
            return false;
        if (topP == null) {
            if (other.topP != null)
                return false;
        } else if (!topP.equals(other.topP))
            return false;
        if (timeLimit == null) {
            if (other.timeLimit != null)
                return false;
        } else if (!timeLimit.equals(other.timeLimit))
            return false;
        if (responseFormat == null) {
            if (other.responseFormat != null)
                return false;
        } else if (!responseFormat.equals(other.responseFormat))
            return false;
        if (chatTemplateKwargs == null) {
            if (other.chatTemplateKwargs != null)
                return false;
        } else if (!chatTemplateKwargs.equals(other.chatTemplateKwargs))
            return false;
        if (includeReasoning == null) {
            if (other.includeReasoning != null)
                return false;
        } else if (!includeReasoning.equals(other.includeReasoning))
            return false;
        if (reasoningEffort == null) {
            if (other.reasoningEffort != null)
                return false;
        } else if (!reasoningEffort.equals(other.reasoningEffort))
            return false;
        if (guidedChoice == null) {
            if (other.guidedChoice != null)
                return false;
        } else if (!guidedChoice.equals(other.guidedChoice))
            return false;
        if (guidedRegex == null) {
            if (other.guidedRegex != null)
                return false;
        } else if (!guidedRegex.equals(other.guidedRegex))
            return false;
        if (guidedGrammar == null) {
            if (other.guidedGrammar != null)
                return false;
        } else if (!guidedGrammar.equals(other.guidedGrammar))
            return false;
        if (repetitionPenalty == null) {
            if (other.repetitionPenalty != null)
                return false;
        } else if (!repetitionPenalty.equals(other.repetitionPenalty))
            return false;
        if (lengthPenalty == null) {
            if (other.lengthPenalty != null)
                return false;
        } else if (!lengthPenalty.equals(other.lengthPenalty))
            return false;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (moderations == null) {
            if (other.moderations != null)
                return false;
        } else if (!moderations.equals(other.moderations))
            return false;
        if (crypto == null) {
            if (other.crypto != null)
                return false;
        } else if (!crypto.equals(other.crypto))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextChatRequest [modelId=" + modelId + ", model=" + model + ", spaceId=" + spaceId + ", projectId=" + projectId
            + ", messages=" + messages + ", tools=" + tools + ", toolChoiceOption=" + toolChoiceOption + ", toolChoice=" + toolChoice
            + ", frequencyPenalty=" + frequencyPenalty + ", logitBias=" + logitBias + ", logprobs=" + logprobs + ", topLogprobs=" + topLogprobs
            + ", maxCompletionTokens=" + maxCompletionTokens + ", n=" + n + ", presencePenalty=" + presencePenalty + ", seed=" + seed
            + ", stop=" + stop + ", temperature=" + temperature + ", topP=" + topP + ", timeLimit=" + timeLimit + ", responseFormat=" + responseFormat
            + ", chatTemplateKwargs=" + chatTemplateKwargs + ", includeReasoning=" + includeReasoning + ", reasoningEffort=" + reasoningEffort
            + ", guidedChoice=" + guidedChoice + ", guidedRegex=" + guidedRegex + ", guidedGrammar=" + guidedGrammar
            + ", repetitionPenalty=" + repetitionPenalty + ", lengthPenalty=" + lengthPenalty + ", context=" + context
            + ", moderations=" + moderations + ", crypto=" + crypto + "]";
    }
}
