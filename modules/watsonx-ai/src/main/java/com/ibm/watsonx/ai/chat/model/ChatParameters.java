/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxModelParameters;
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
 *     .toolChoiceOption(ToolChoice.AUTO)
 *     .responseAsJson()
 *     .build();
 * }</pre>
 */
public final class ChatParameters extends WatsonxModelParameters {

    public record JsonSchemaObject(String name, Object schema, boolean strict) {};

    private final String toolChoiceOption;
    private final Map<String, Object> toolChoice;
    private final Set<String> guidedChoice;
    private final String guidedRegex;
    private final String guidedGrammar;
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
    private final String responseFormat;
    private final JsonSchemaObject jsonSchema;
    private final Double repetitionPenalty;
    private final Double lengthPenalty;
    private final String context;

    private ChatParameters(Builder builder) {
        super(builder);
        this.toolChoiceOption = nonNull(builder.toolChoiceOption) ? builder.toolChoiceOption.type() : null;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.logitBias = builder.logitBias;
        this.logprobs = builder.logprobs;
        this.topLogprobs = builder.topLogprobs;
        this.maxCompletionTokens = builder.maxCompletionTokens;
        this.n = builder.n;
        this.presencePenalty = builder.presencePenalty;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.timeLimit = builder.timeLimit;
        this.seed = builder.seed;
        this.stop = builder.stop;
        this.context = builder.context;

        if (nonNull(builder.responseFormat)) {
            this.responseFormat = builder.responseFormat.type();
            this.jsonSchema = builder.jsonSchema;
        } else {
            this.responseFormat = null;
            this.jsonSchema = null;
        }

        this.toolChoice = nonNull(builder.toolChoice)
            ? Map.of("type", "function", "function", Map.of("name", builder.toolChoice))
            : null;

        this.guidedChoice = builder.guidedChoice;
        this.guidedRegex = builder.guidedRegex;
        this.guidedGrammar = builder.guidedGrammar;
        this.repetitionPenalty = builder.repetitionPenalty;
        this.lengthPenalty = builder.lengthPenalty;
    }

    public String getToolChoiceOption() {
        return toolChoiceOption;
    }

    public Map<String, Object> getToolChoice() {
        return toolChoice;
    }

    public Map<String, Integer> getLogitBias() {
        return logitBias;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
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

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public Integer getSeed() {
        return seed;
    }

    public List<String> getStop() {
        return stop;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public JsonSchemaObject getJsonSchema() {
        return jsonSchema;
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

    public Double getRepetitionPenalty() {
        return repetitionPenalty;
    }

    public Double getLengthPenalty() {
        return lengthPenalty;
    }

    public String getContext() {
        return context;
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
     *     .toolChoiceOption(ToolChoice.AUTO)
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
    public final static class Builder extends WatsonxModelParameters.Builder<Builder> {
        private ToolChoiceOption toolChoiceOption;
        private String toolChoice;
        private Set<String> guidedChoice;
        private String guidedRegex;
        private String guidedGrammar;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private Boolean logprobs;
        private Integer topLogprobs;
        private Integer maxCompletionTokens;
        private Integer n;
        private Double presencePenalty;
        private ResponseFormat responseFormat;
        private Integer seed;
        private List<String> stop;
        private Double temperature;
        private Double topP;
        private Long timeLimit;
        private JsonSchemaObject jsonSchema;
        private Double repetitionPenalty;
        private Double lengthPenalty;
        private String context;

        private Builder() {}

        /**
         * Specifies the tool selection strategy for the model.
         * <p>
         * When set to {@code ToolChoiceOption.AUTO}, the model automatically decides whether to invoke any tool.
         * <p>
         * When set to {@code ToolChoiceOption.REQUIRED}, the model is forced to invoke a specific tool.
         * <p>
         * When set to {@code ToolChoiceOption.NONE}, the model is not allowed to invoke any tools.
         *
         * @param toolChoiceOption the {@link ToolChoiceOption} that determines how the model selects tools
         */
        public Builder toolChoiceOption(ToolChoiceOption toolChoiceOption) {
            this.toolChoiceOption = toolChoiceOption;
            return this;
        }

        /**
         * Specifies a set of allowed output choices.
         * <p>
         * When this parameter is set, the model is constrained to return exactly one of the provided choices.
         *
         * @param guidedChoice a variable number of allowed output strings
         */
        public Builder guidedChoice(String... guidedChoice) {
            return guidedChoice(Set.of(guidedChoice));
        }

        /**
         * Specifies a set of allowed output choices.
         * <p>
         * When this parameter is set, the model is constrained to return exactly one of the provided choices.
         *
         * @param guidedChoices a variable number of allowed output strings
         */
        public Builder guidedChoice(Set<String> guidedChoices) {
            this.guidedChoice = guidedChoices;
            return this;
        }

        /**
         * Constrains the model output to match a regular expression pattern.
         * <p>
         * If specified, the generated output must conform to the provided regex.
         *
         * @param guidedRegex the regex pattern that the output must match
         */
        public Builder guidedRegex(String guidedRegex) {
            this.guidedRegex = guidedRegex;
            return this;
        }

        /**
         * Constrains the model output to follow a context-free grammar.
         * <p>
         * If specified, the generated output will conform to the defined grammar.
         *
         * @param guidedGrammar the context-free grammar string that the output must follow
         */
        public Builder guidedGrammar(String guidedGrammar) {
            this.guidedGrammar = guidedGrammar;
            return this;
        }

        /**
         * Forces the model to call a specific tool by its identifier.
         * <p>
         * Only one of {@code toolChoice} or {@code toolChoiceOption} should be set.
         *
         * @param toolChoice the tool identifier (name) to invoke
         */
        public Builder toolChoice(String toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        /**
         * Sets the frequency penalty to reduce repetition of tokens.
         * <p>
         * Values > 0 discourage repetition; valid range is (-2, 2).
         *
         * @param frequencyPenalty the frequency penalty
         */
        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        /**
         * Increasing or decreasing probability of tokens being selected during generation; a positive bias makes a token more likely to appear, while
         * a negative bias makes it less likely.
         *
         * @param logitBias a map from token ids to bias values
         */
        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        /**
         * Enables or disables the return of log probabilities for the generated tokens.
         *
         * @param logprobs whether to return log probabilities
         */
        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        /**
         * An integer specifying the number of most likely tokens to return at each token position, each with an associated log probability. The
         * option logprobs must be set to true if this parameter is used.
         *
         * @param topLogprobs the number of top tokens with logprobs to return
         */
        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        /**
         * The maximum number of tokens that can be generated in the chat completion. The total length of input tokens and generated tokens is limited
         * by the model's context length. Set to 0 for the model's configured max generated tokens.
         *
         * @param maxCompletionTokens the maximum number of tokens
         */
        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        /**
         * Sets the number of completions to generate for each input. A higher value may increase cost due to multiple outputs.
         *
         * @param n the number of completions to generate
         */
        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        /**
         * Sets the presence penalty to encourage new topic generation. Positive values penalize previously mentioned tokens. Valid range: (-2, 2).
         *
         * @param presencePenalty the presence penalty
         */
        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        /**
         * What sampling temperature to use,. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more
         * focused and deterministic. We generally recommend altering this or {@code top_p} but not both. Valid range: (0, 2).
         *
         * @param temperature the sampling temperature
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p
         * probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. We generally recommend altering this
         * or {@code temperature} but not both. Valid range: (0, 1).
         *
         * @param topP the nucleus sampling threshold
         */
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        /**
         * Sets a maximum time limit for the completion generation.
         *
         * @param timeLimit {@link Duration} time limit.
         */
        public Builder timeLimit(Duration timeLimit) {
            this.timeLimit = isNull(timeLimit) ? null : timeLimit.toMillis();
            return this;
        }

        /**
         * Sets the response format to {@code TEXT}, indicating that the model output will be free-form text.
         * <p>
         * No JSON structure will be enforced, and the response will be treated as plain text.
         */
        public Builder responseAsText() {
            this.responseFormat = ResponseFormat.TEXT;
            return this;
        }

        /**
         * Sets the response format to {@code JSON}, indicating that the model output should be a JSON object.
         * <p>
         * The output will be in JSON format, but no schema will be enforced or validated.
         */
        public Builder responseAsJson() {
            this.responseFormat = ResponseFormat.JSON;
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
         * Sets the response format to {@code JSON_SCHEMA} and defines the JSON Schema used to validate the model's output.
         * <p>
         * Allows specifying a custom schema name and whether strict schema validation should be applied.
         * <ul>
         * <li>If {@code strict} is {@code true}, the model's output must exactly match the schema.</li>
         * <li>If {@code strict} is {@code false}, additional fields not defined in the schema are allowed.</li>
         * </ul>
         *
         * @param name the identifier name for the schema
         * @param schema the JSON Schema describing the expected output structure
         * @param strict whether to enforce strict schema validation
         */
        public Builder responseAsJsonSchema(String name, JsonSchema schema, boolean strict) {
            this.responseFormat = ResponseFormat.JSON_SCHEMA;
            this.jsonSchema = new JsonSchemaObject(name, schema, strict);
            return this;
        }

        /**
         * Sets the response format to {@code JSON_SCHEMA} and defines the JSON Schema used to validate the model's output.
         * <p>
         * Allows specifying a custom schema name and whether strict schema validation should be applied.
         * <ul>
         * <li>If {@code strict} is {@code true}, the model's output must exactly match the schema.</li>
         * <li>If {@code strict} is {@code false}, additional fields not defined in the schema are allowed.</li>
         * </ul>
         *
         * @param name the identifier name for the schema
         * @param schema the JSON Schema describing the expected output structure
         * @param strict whether to enforce strict schema validation
         */
        public Builder responseAsJsonSchema(String name, Map<String, Object> schema, boolean strict) {
            this.responseFormat = ResponseFormat.JSON_SCHEMA;
            this.jsonSchema = new JsonSchemaObject(name, schema, strict);
            return this;
        }

        /**
         * Random number generator seed to use in sampling mode for experimental repeatability.
         *
         * @param seed the seed value
         */
        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Defines stop sequences that end the generation when encountered. A maximum of 4 unique stop sequences is allowed.
         *
         * @param stop list of stop sequences
         */
        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        /**
         * Sets the repetition penalty to be applied during text generation. This penalty helps to discourage the model from repeating the same words
         * or phrases too often.
         * <p>
         * The penalty value should be greater than 1.0 for repetition discouragement. A value of 1.0 means no penalty, and values above 1.0 increase
         * the strength of the penalty.
         *
         * @param repetitionPenalty the repetition penalty value.
         */
        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        /**
         * Sets the length penalty to be applied during text generation. This penalty influences the length of the generated text. A length penalty
         * discourages the model from generating overly long responses, or conversely, it can encourage more extended outputs.
         * <p>
         * When the penalty value is greater than 1.0, it discourages generating longer responses. Conversely, a value less than 1.0 incentivizes the
         * model to generate longer text. A value of 1.0 means no penalty, and the length of the output will be determined by other factors, such as
         * the input prompt and model's natural completion behavior.
         *
         * @param lengthPenalty the length penalty value.
         */
        public Builder lengthPenalty(Double lengthPenalty) {
            this.lengthPenalty = lengthPenalty;
            return this;
        }

        /**
         * Sets the context string to be inserted into the messages during chat generation.
         * <p>
         * Depending on the underlying model, the provided context may be injected into:
         * <ul>
         * <li>the content of the <b>system</b> role message.</li>
         * <li>the beginning of the <b>last user</b> message.</li>
         * </ul>
         * For example, if context is {@code "Today is Wednesday"} and the user input is {@code "Who are you and which day is tomorrow?"}, the
         * resulting message may be {@code "Today is Wednesday. Who are you and which day is tomorrow?"}.
         * <p>
         * <b>Note:</b> This parameter is only supported when using {@link DeploymentService}.
         *
         * @param context The context string to insert into the messages
         * @return The current Builder instance for method chaining
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

    /**
     * Specifies the format in which the model should return the response.
     */
    public static enum ResponseFormat {
        TEXT("text"),
        JSON("json_object"),
        JSON_SCHEMA("json_schema");

        private final String type;

        ResponseFormat(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }

    /**
     * Specifies how the model should decide whether to use a tool during generation.
     */
    public static enum ToolChoiceOption {
        /**
         * The model will automatically decide whether to generate a message or invoke a tool.
         */
        AUTO("auto"),

        /**
         * The model is required to invoke a specific tool and cannot choose freely.
         */
        REQUIRED("required"),

        /**
         * The model can not invoke tools.
         */
        NONE("none");

        private final String type;

        ToolChoiceOption(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }
}