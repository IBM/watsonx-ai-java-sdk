/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Represents a set of parameters used to control the behavior of a chat model during text generation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ChatParameters params = ChatParameters.builder()
 *     .temperature(0.7)
 *     .maxCompletionTokens(0)
 *     .toolChoiceOption(ToolChoiceOption.AUTO)
 *     .responseAsJson()
 *     .build();
 * }</pre>
 */
public final class ChatParameters extends BaseChatParameters {

    private final String projectId;
    private final String spaceId;
    private final String crypto;
    private final Set<String> guidedChoice;
    private final String guidedRegex;
    private final String guidedGrammar;
    private final Double repetitionPenalty;
    private final Double lengthPenalty;
    private final String context;

    private ChatParameters(Builder builder) {
        super(builder);
        projectId = builder.projectId;
        spaceId = builder.spaceId;
        crypto = builder.crypto;
        guidedChoice = isNull(builder.guidedChoice) ? null : Set.copyOf(builder.guidedChoice);
        guidedRegex = builder.guidedRegex;
        guidedGrammar = builder.guidedGrammar;
        repetitionPenalty = builder.repetitionPenalty;
        lengthPenalty = builder.lengthPenalty;
        context = builder.context;
    }

    /**
     * Returns the project id.
     *
     * @return the project id
     */
    public String projectId() {
        return projectId;
    }

    /**
     * Returns the space id.
     *
     * @return the space id
     */
    public String spaceId() {
        return spaceId;
    }

    /**
     * Returns the crypto key reference for encrypting inference requests.
     *
     * @return the crypto key reference identifier
     */
    public String crypto() {
        return crypto;
    }

    /**
     * Returns the set of allowed output choices.
     *
     * @return the set of allowed output choices
     */
    public Set<String> guidedChoice() {
        return guidedChoice;
    }

    /**
     * Returns the regular expression pattern that the output must match.
     *
     * @return the regular expression pattern
     */
    public String guidedRegex() {
        return guidedRegex;
    }

    /**
     * Returns the context-free grammar that the output must follow.
     *
     * @return the context-free grammar
     */
    public String guidedGrammar() {
        return guidedGrammar;
    }

    /**
     * Returns the repetition penalty applied during text generation.
     *
     * @return the repetition penalty
     */
    public Double repetitionPenalty() {
        return repetitionPenalty;
    }

    /**
     * Returns the length penalty applied during text generation.
     *
     * @return the length penalty
     */
    public Double lengthPenalty() {
        return lengthPenalty;
    }

    /**
     * Returns the context string inserted into the messages during chat generation.
     *
     * @return the context string
     */
    public String context() {
        return context;
    }

    /**
     * Creates a builder initialized with the current state of the {@code ChatParameters}.
     *
     * @return a new {@link Builder} instance pre-populated with this {@code ChatParameters}' data
     */
    public Builder toBuilder() {
        var builder = new Builder()
            .context(context)
            .crypto(crypto)
            .frequencyPenalty(frequencyPenalty)
            .guidedChoice(guidedChoice)
            .guidedGrammar(guidedGrammar)
            .guidedRegex(guidedRegex)
            .lengthPenalty(lengthPenalty)
            .logitBias(logitBias)
            .logprobs(logprobs)
            .maxCompletionTokens(maxCompletionTokens)
            .modelId(modelId)
            .n(n)
            .presencePenalty(presencePenalty)
            .projectId(projectId)
            .repetitionPenalty(repetitionPenalty)
            .seed(seed)
            .spaceId(spaceId)
            .stop(stop)
            .temperature(temperature)
            .timeLimit(nonNull(timeLimit) ? Duration.ofMillis(timeLimit) : null)
            .toolChoiceOption(nonNull(toolChoiceOption) ? BaseChatParameters.ToolChoiceOption.fromValue(toolChoiceOption) : null)
            .topLogprobs(topLogprobs)
            .topP(topP)
            .transactionId(transactionId);

        if (nonNull(toolChoice)) {
            var function = toolChoice.get("function");
            if (function instanceof Map<?, ?> functionMap && nonNull(functionMap.get("name")))
                builder.toolChoice(String.valueOf(functionMap.get("name")));
        }

        builder.responseFormat = nonNull(responseFormat) ? BaseChatParameters.ResponseFormat.from(responseFormat) : null;
        builder.jsonSchema = jsonSchema;
        return builder;
    }

    @Override
    public String toString() {
        return "ChatParameters [modelId=" + modelId + ", transactionId=" + transactionId + ", projectId=" + projectId + ", spaceId=" + spaceId
            + ", crypto=" + crypto + ", toolChoiceOption=" + toolChoiceOption + ", toolChoice=" + toolChoice + ", guidedChoice=" + guidedChoice
            + ", guidedRegex=" + guidedRegex + ", guidedGrammar=" + guidedGrammar + ", frequencyPenalty=" + frequencyPenalty + ", logitBias="
            + logitBias
            + ", logprobs=" + logprobs + ", topLogprobs=" + topLogprobs + ", maxCompletionTokens=" + maxCompletionTokens + ", n=" + n
            + ", presencePenalty=" + presencePenalty + ", seed=" + seed + ", stop=" + stop + ", temperature=" + temperature + ", topP=" + topP
            + ", timeLimit=" + timeLimit + ", responseFormat=" + responseFormat + ", jsonSchema=" + jsonSchema + ", repetitionPenalty="
            + repetitionPenalty + ", lengthPenalty=" + lengthPenalty + ", context=" + context + "]";
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ChatParameters params = ChatParameters.builder()
     *     .temperature(0.7)
     *     .maxCompletionTokens(0)
     *     .toolChoiceOption(ToolChoiceOption.AUTO)
     *     .responseAsJson()
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatParameters} instances with configurable parameters.
     */
    public final static class Builder extends BaseChatParameters.Builder<Builder> {
        private String projectId;
        private String spaceId;
        private String crypto;
        private Set<String> guidedChoice;
        private String guidedRegex;
        private String guidedGrammar;
        private Double repetitionPenalty;
        private Double lengthPenalty;
        private String context;

        private Builder() {}

        /**
         * Sets the project id.
         *
         * @param projectId project id value
         */
        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        /**
         * Sets the space id.
         *
         * @param spaceId space id value
         */
        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        /**
         * Sets the crypto key reference for encrypting inference requests.
         *
         * @param crypto the key reference identifier (e.g. CRN format for IBM Key Protect)
         */
        public Builder crypto(String crypto) {
            this.crypto = crypto;
            return this;
        }

        /**
         * Specifies a set of allowed output choices.
         *
         * @param guidedChoice a variable number of allowed output strings
         */
        public Builder guidedChoice(String... guidedChoice) {
            return guidedChoice(Set.of(guidedChoice));
        }

        /**
         * Specifies a set of allowed output choices.
         *
         * @param guidedChoices the set of allowed output strings
         */
        public Builder guidedChoice(Set<String> guidedChoices) {
            this.guidedChoice = isNull(guidedChoices) ? null : Set.copyOf(guidedChoices);
            return this;
        }

        /**
         * Constrains the model output to match a regular expression pattern.
         *
         * @param guidedRegex the regex pattern
         */
        public Builder guidedRegex(String guidedRegex) {
            this.guidedRegex = guidedRegex;
            return this;
        }

        /**
         * Constrains the model output to follow a context-free grammar.
         *
         * @param guidedGrammar the context-free grammar string
         */
        public Builder guidedGrammar(String guidedGrammar) {
            this.guidedGrammar = guidedGrammar;
            return this;
        }

        /**
         * Sets the response format to {@code JSON_SCHEMA} and defines the JSON Schema used to validate the model's output.
         *
         * @param schema the JSON Schema describing the expected output structure
         */
        public Builder responseAsJsonSchema(JsonSchema schema) {
            return responseAsJsonSchema(UUID.randomUUID().toString(), schema, true);
        }

        /**
         * Sets the response format to {@code JSON_SCHEMA} with a custom schema name and strictness.
         *
         * @param name the identifier name for the schema
         * @param schema the JSON Schema describing the expected output structure
         * @param strict whether to enforce strict schema validation
         */
        public Builder responseAsJsonSchema(String name, JsonSchema schema, boolean strict) {
            this.responseFormat = BaseChatParameters.ResponseFormat.JSON_SCHEMA;
            this.jsonSchema = new JsonSchemaObject(name, schema, strict);
            return this;
        }

        /**
         * Sets the response format to {@code JSON_SCHEMA} with a raw map schema.
         *
         * @param name the identifier name for the schema
         * @param schema the schema as a raw map
         * @param strict whether to enforce strict schema validation
         */
        public Builder responseAsJsonSchema(String name, Map<String, Object> schema, boolean strict) {
            this.responseFormat = BaseChatParameters.ResponseFormat.JSON_SCHEMA;
            this.jsonSchema = new JsonSchemaObject(name, schema, strict);
            return this;
        }

        /**
         * Sets the repetition penalty to discourage the model from repeating tokens.
         *
         * @param repetitionPenalty the repetition penalty value
         */
        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        /**
         * Sets the length penalty applied during text generation.
         *
         * @param lengthPenalty the length penalty value
         */
        public Builder lengthPenalty(Double lengthPenalty) {
            this.lengthPenalty = lengthPenalty;
            return this;
        }

        /**
         * Sets the context string to be inserted into the messages during chat generation.
         * <p>
         * <b>Note:</b> This parameter is only supported when using {@link DeploymentService}.
         *
         * @param context the context string to insert
         */
        public Builder context(String context) {
            this.context = context;
            return this;
        }

        /**
         * Builds a {@link ChatParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatParameters}
         */
        public ChatParameters build() {
            return new ChatParameters(this);
        }
    }
}
