/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters;

/**
 * Abstract base class that holds every chat parameter shared between the Text Chat API and the Model Gateway endpoint.
 *
 * @see ChatParameters
 * @see ModelGatewayChatParameters
 */
public abstract class BaseChatParameters {

    /**
     * Represents a JSON schema used to validate the model's structured output.
     *
     * @param name the schema name
     * @param schema the JSON schema object
     * @param strict whether strict schema adherence is enforced
     */
    public record JsonSchemaObject(String name, Object schema, boolean strict) {
        public JsonSchemaObject {
            if (schema instanceof Map<?, ?> map)
                schema = Collections.unmodifiableMap(new LinkedHashMap<>(map));
        }
    }

    protected final String modelId;
    protected final String transactionId;
    protected final String toolChoiceOption;
    protected final Map<String, Object> toolChoice;
    protected final Double frequencyPenalty;
    protected final Map<String, Integer> logitBias;
    protected final Boolean logprobs;
    protected final Integer topLogprobs;
    protected final Integer maxCompletionTokens;
    protected final Integer n;
    protected final Double presencePenalty;
    protected final Integer seed;
    protected final List<String> stop;
    protected final Double temperature;
    protected final Double topP;
    protected final Long timeLimit;
    protected final String responseFormat;
    protected final JsonSchemaObject jsonSchema;

    protected <B extends Builder<B>> BaseChatParameters(Builder<B> builder) {
        modelId = builder.modelId;
        transactionId = builder.transactionId;
        toolChoiceOption = nonNull(builder.toolChoiceOption) ? builder.toolChoiceOption.value() : null;
        frequencyPenalty = builder.frequencyPenalty;
        logitBias = isNull(builder.logitBias) ? null : Map.copyOf(builder.logitBias);
        logprobs = builder.logprobs;
        topLogprobs = builder.topLogprobs;
        maxCompletionTokens = builder.maxCompletionTokens;
        n = builder.n;
        presencePenalty = builder.presencePenalty;
        temperature = builder.temperature;
        topP = builder.topP;
        timeLimit = builder.timeLimit;
        seed = builder.seed;
        stop = isNull(builder.stop) ? null : List.copyOf(builder.stop);

        if (nonNull(builder.responseFormat)) {
            responseFormat = builder.responseFormat.value();
            jsonSchema = builder.jsonSchema;
        } else {
            responseFormat = null;
            jsonSchema = null;
        }

        toolChoice = nonNull(builder.toolChoice)
            ? Map.of("type", "function", "function", Map.of("name", builder.toolChoice))
            : null;
    }

    /**
     * Returns the model identifier.
     *
     * @return the model id
     */
    public String modelId() {
        return modelId;
    }

    /**
     * Returns the transaction identifier used for request tracing.
     *
     * @return the transaction id
     */
    public String transactionId() {
        return transactionId;
    }

    /**
     * Returns the tool selection strategy for the model.
     * <p>
     * Wire values: {@code "auto"}, {@code "required"}, {@code "none"} - see {@link ToolChoiceOption}.
     *
     * @return the tool selection strategy string
     */
    public String toolChoiceOption() {
        return toolChoiceOption;
    }

    /**
     * Returns the specific tool the model is forced to call, encoded as a map of the form
     * {@code {"type":"function","function":{"name":"<tool-name>"}}}.
     *
     * @return the tool choice map, or {@code null} if no specific tool is forced
     */
    public Map<String, Object> toolChoice() {
        return toolChoice;
    }

    /**
     * Returns the logit bias applied to specific tokens during generation.
     * <p>
     * Maps token IDs (as strings) to bias values in the range {@code [-100, 100]}. Positive values increase and negative values decrease the
     * likelihood of the associated token being selected.
     *
     * @return the logit bias map, or {@code null} if not set
     */
    public Map<String, Integer> logitBias() {
        return logitBias;
    }

    /**
     * Returns the frequency penalty used to reduce repetition of tokens. Valid range: {@code (-2.0, 2.0)}.
     * <p>
     * Positive values penalize tokens in proportion to how often they have already appeared in the text, making the model less likely to repeat the
     * same token verbatim.
     *
     * @return the frequency penalty, or {@code null} if not set
     */
    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    /**
     * Returns whether log probabilities are returned for the generated tokens.
     * <p>
     * When {@code true}, the response includes the log probability of each generated token. Use {@link #topLogprobs()} to also receive the top-N
     * alternative tokens at each position.
     *
     * @return {@code true} if log probabilities are enabled, {@code null} if not set
     */
    public Boolean logprobs() {
        return logprobs;
    }

    /**
     * Returns the number of most likely tokens to return at each token position, along with their log probabilities.
     * <p>
     * Requires {@link #logprobs()} to be {@code true}. Valid range: {@code [1, 20]}.
     *
     * @return the top logprobs count, or {@code null} if not set
     */
    public Integer topLogprobs() {
        return topLogprobs;
    }

    /**
     * Returns the maximum number of tokens that can be generated in the chat completion.
     * <p>
     * A value of {@code 0} means the model's maximum is used. Includes both visible output tokens and any internal reasoning tokens.
     *
     * @return the max completion tokens, or {@code null} if not set
     */
    public Integer maxCompletionTokens() {
        return maxCompletionTokens;
    }

    /**
     * Returns the number of chat completion choices to generate for each input message.
     * <p>
     * Generating multiple completions consumes more tokens. Consider this when setting the value together with {@link #maxCompletionTokens()}.
     *
     * @return the number of completions, or {@code null} if not set
     */
    public Integer n() {
        return n;
    }

    /**
     * Returns the presence penalty used to encourage new topic generation. Valid range: {@code (-2.0, 2.0)}.
     * <p>
     * Unlike frequency penalty, presence penalty applies a flat penalty for any token that has appeared at least once, regardless of how often.
     *
     * @return the presence penalty, or {@code null} if not set
     */
    public Double presencePenalty() {
        return presencePenalty;
    }

    /**
     * Returns the sampling temperature. Higher values make the output more random. Valid range: {@code (0.0, 2.0)}.
     * <p>
     * Use {@code 0.0} (or close to it) for deterministic outputs and values above {@code 1.0} for more creative or diverse responses. Avoid combining
     * with {@link #topP()} - use one or the other.
     *
     * @return the sampling temperature, or {@code null} if not set
     */
    public Double temperature() {
        return temperature;
    }

    /**
     * Returns the nucleus sampling threshold. Valid range: {@code (0.0, 1.0)}.
     * <p>
     * The model considers only the smallest set of tokens whose cumulative probability mass reaches this threshold. For example, {@code 0.1} means
     * only the top 10% probability mass tokens are considered. Avoid combining with {@link #temperature()}.
     *
     * @return the top-p value, or {@code null} if not set
     */
    public Double topP() {
        return topP;
    }

    /**
     * Returns the maximum time limit for the completion generation, in milliseconds.
     * <p>
     * If generation does not complete within this limit, the request is cut off and a partial response may be returned.
     *
     * @return the time limit in milliseconds, or {@code null} if not set
     */
    public Long timeLimit() {
        return timeLimit;
    }

    /**
     * Returns the random number generator seed used in sampling mode.
     * <p>
     * When the same seed is used with identical inputs and parameters, the model will attempt to return the same result deterministically. Results
     * are not guaranteed to be identical across model versions or API changes.
     *
     * @return the seed, or {@code null} if not set
     */
    public Integer seed() {
        return seed;
    }

    /**
     * Returns the stop sequences that end the generation when encountered.
     * <p>
     * Generation halts at the first occurrence of any stop sequence in the output. The stop sequence itself is not included in the response. Up to 4
     * sequences are supported.
     *
     * @return the stop sequences list, or {@code null} if not set
     */
    public List<String> stop() {
        return stop;
    }

    /**
     * Returns the wire-format string value describing the response format.
     * <p>
     * Possible values: {@code "text"}, {@code "json_object"}, {@code "json_schema"}. Use the builder methods {@link Builder#responseAsText()},
     * {@link Builder#responseAsJson()}, and {@link Builder#responseAsJsonSchema(String, Object, boolean)} to set this field.
     *
     * @return the response format string, or {@code null} if not set
     */
    public String responseFormat() {
        return responseFormat;
    }

    /**
     * Returns the JSON schema used to validate and constrain the model's structured output.
     * <p>
     * Only present when {@link #responseFormat()} is {@code "json_schema"}. Set via {@link Builder#responseAsJsonSchema(String, Object, boolean)}.
     *
     * @return the JSON schema object, or {@code null} if not set
     */
    public JsonSchemaObject jsonSchema() {
        return jsonSchema;
    }

    /**
     * Abstract builder for constructing {@link BaseChatParameters} subclasses.
     *
     * @param <T> the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        protected String modelId;
        protected String transactionId;
        protected ToolChoiceOption toolChoiceOption;
        protected String toolChoice;
        protected Double frequencyPenalty;
        protected Map<String, Integer> logitBias;
        protected Boolean logprobs;
        protected Integer topLogprobs;
        protected Integer maxCompletionTokens;
        protected Integer n;
        protected Double presencePenalty;
        protected ResponseFormat responseFormat;
        protected Integer seed;
        protected List<String> stop;
        protected Double temperature;
        protected Double topP;
        protected Long timeLimit;
        protected JsonSchemaObject jsonSchema;

        protected Builder() {}

        /**
         * Sets the model identifier.
         *
         * @param modelId the model id
         */
        public T modelId(String modelId) {
            this.modelId = modelId;
            return (T) this;
        }

        /**
         * Sets the transaction identifier for request tracing.
         *
         * @param transactionId the transaction id
         */
        public T transactionId(String transactionId) {
            this.transactionId = transactionId;
            return (T) this;
        }

        /**
         * Specifies the tool selection strategy for the model.
         *
         * @param toolChoiceOption the {@link ToolChoiceOption}
         */
        public T toolChoiceOption(ToolChoiceOption toolChoiceOption) {
            this.toolChoiceOption = toolChoiceOption;
            return (T) this;
        }

        /**
         * Forces the model to call a specific tool by its identifier.
         *
         * @param toolChoice the tool name to invoke
         */
        public T toolChoice(String toolChoice) {
            this.toolChoice = toolChoice;
            return (T) this;
        }

        /**
         * Sets the frequency penalty to reduce repetition of tokens. Valid range: {@code (-2.0, 2.0)}.
         * <p>
         * Positive values discourage repeated tokens in proportion to how often they appear. Use to reduce verbatim repetition. Avoid combining with
         * {@link #presencePenalty(Double)}.
         *
         * @param frequencyPenalty the frequency penalty
         */
        public T frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return (T) this;
        }

        /**
         * Sets the logit bias to increase or decrease the probability of specific tokens.
         *
         * @param logitBias a map from token ids to bias values
         */
        public T logitBias(Map<String, Integer> logitBias) {
            this.logitBias = isNull(logitBias) ? null : Map.copyOf(logitBias);
            return (T) this;
        }

        /**
         * Enables or disables the return of log probabilities for generated tokens.
         *
         * @param logprobs whether to return log probabilities
         */
        public T logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return (T) this;
        }

        /**
         * Specifies the number of most likely tokens to return at each token position.
         *
         * @param topLogprobs the number of top tokens with log probabilities to return
         */
        public T topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return (T) this;
        }

        /**
         * Sets the maximum number of tokens that can be generated in the chat completion.
         *
         * @param maxCompletionTokens the maximum number of tokens
         */
        public T maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return (T) this;
        }

        /**
         * Sets the number of completions to generate for each input.
         *
         * @param n the number of completions to generate
         */
        public T n(Integer n) {
            this.n = n;
            return (T) this;
        }

        /**
         * Sets the presence penalty to encourage new topic generation. Valid range: {@code (-2.0, 2.0)}.
         * <p>
         * Applies a flat penalty for any token that has appeared at least once, encouraging the model to introduce new subjects. Avoid combining with
         * {@link #frequencyPenalty(Double)}.
         *
         * @param presencePenalty the presence penalty
         */
        public T presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return (T) this;
        }

        /**
         * Sets the sampling temperature. Valid range: {@code (0.0, 2.0)}.
         * <p>
         * Higher values produce more random, creative output. Lower values (closer to {@code 0.0}) produce more deterministic output. Avoid combining
         * with {@link #topP(Double)}.
         *
         * @param temperature the sampling temperature
         */
        public T temperature(Double temperature) {
            this.temperature = temperature;
            return (T) this;
        }

        /**
         * Sets the nucleus sampling threshold (top-p). Valid range: {@code (0.0, 1.0)}.
         * <p>
         * Only tokens within the top-p probability mass are considered during sampling. A value of {@code 0.1} restricts sampling to the top 10% most
         * likely tokens. Avoid combining with {@link #temperature(Double)}.
         *
         * @param topP the nucleus sampling threshold
         */
        public T topP(Double topP) {
            this.topP = topP;
            return (T) this;
        }

        /**
         * Sets a maximum time limit for completion generation.
         *
         * @param timeLimit the {@link Duration} time limit
         */
        public T timeLimit(Duration timeLimit) {
            this.timeLimit = isNull(timeLimit) ? null : timeLimit.toMillis();
            return (T) this;
        }

        /**
         * Sets the random number generator seed for sampling mode.
         *
         * @param seed the seed value
         */
        public T seed(Integer seed) {
            this.seed = seed;
            return (T) this;
        }

        /**
         * Defines stop sequences that end generation when encountered.
         *
         * @param stop list of stop sequences
         */
        public T stop(List<String> stop) {
            this.stop = isNull(stop) ? null : List.copyOf(stop);
            return (T) this;
        }

        /**
         * Sets the response format to {@code TEXT}.
         */
        public T responseAsText() {
            this.responseFormat = ResponseFormat.TEXT;
            return (T) this;
        }

        /**
         * Sets the response format to {@code JSON} object mode.
         */
        public T responseAsJson() {
            this.responseFormat = ResponseFormat.JSON;
            return (T) this;
        }

        /**
         * Sets the response format to {@code JSON_SCHEMA} with the provided schema.
         *
         * @param name the schema name
         * @param schema the JSON schema object
         * @param strict whether to enforce strict schema validation
         */
        public T responseAsJsonSchema(String name, Object schema, boolean strict) {
            this.responseFormat = ResponseFormat.JSON_SCHEMA;
            this.jsonSchema = new JsonSchemaObject(name, schema, strict);
            return (T) this;
        }
    }

    /**
     * Specifies the format in which the model should return the response.
     */
    public static enum ResponseFormat {
        TEXT("text"),
        JSON("json_object"),
        JSON_SCHEMA("json_schema");

        private final String value;

        ResponseFormat(String value) {
            this.value = value;
        }

        /**
         * Resolves a {@link ResponseFormat} from its string value.
         *
         * @param value the string value to resolve
         * @return the matching {@link ResponseFormat}
         * @throws IllegalArgumentException if the value does not match any known response format
         */
        public static ResponseFormat from(String value) {
            for (ResponseFormat format : ResponseFormat.values()) {
                if (format.value.equals(value)) {
                    return format;
                }
            }
            throw new IllegalArgumentException("Unknown response format: " + value);
        }

        /**
         * Returns the string value of this response format.
         *
         * @return the string value
         */
        public String value() {
            return value;
        }
    }

    /**
     * Specifies how the model should decide whether to use a tool during generation.
     */
    public static enum ToolChoiceOption {
        AUTO("auto"),
        REQUIRED("required"),
        NONE("none");

        private final String value;

        ToolChoiceOption(String value) {
            this.value = value;
        }

        /**
         * Returns the string value of this option.
         *
         * @return the string value
         */
        public String value() {
            return value;
        }

        /**
         * Returns the {@code ToolChoiceOption} matching the given wire value.
         *
         * @param value the wire string value (for example {@code "auto"})
         * @return the matching {@code ToolChoiceOption}
         * @throws IllegalArgumentException if no option matches the given value
         */
        public static ToolChoiceOption fromValue(String value) {
            for (var option : values()) {
                if (option.value.equals(value))
                    return option;
            }
            throw new IllegalArgumentException("Unknown tool choice option: " + value);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
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
        result = prime * result + ((jsonSchema == null) ? 0 : jsonSchema.hashCode());
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
        BaseChatParameters other = (BaseChatParameters) obj;
        if (modelId == null) {
            if (other.modelId != null)
                return false;
        } else if (!modelId.equals(other.modelId))
            return false;
        if (transactionId == null) {
            if (other.transactionId != null)
                return false;
        } else if (!transactionId.equals(other.transactionId))
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
        if (jsonSchema == null) {
            if (other.jsonSchema != null)
                return false;
        } else if (!jsonSchema.equals(other.jsonSchema))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BaseChatParameters [modelId=" + modelId + ", transactionId=" + transactionId + ", toolChoiceOption=" + toolChoiceOption
            + ", toolChoice=" + toolChoice + ", frequencyPenalty=" + frequencyPenalty + ", logitBias=" + logitBias + ", logprobs=" + logprobs
            + ", topLogprobs=" + topLogprobs + ", maxCompletionTokens=" + maxCompletionTokens + ", n=" + n + ", presencePenalty=" + presencePenalty
            + ", seed=" + seed + ", stop=" + stop + ", temperature=" + temperature + ", topP=" + topP + ", timeLimit=" + timeLimit
            + ", responseFormat=" + responseFormat + ", jsonSchema=" + jsonSchema + "]";
    }
}
