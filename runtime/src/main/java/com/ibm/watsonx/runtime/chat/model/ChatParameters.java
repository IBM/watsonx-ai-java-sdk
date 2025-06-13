package com.ibm.watsonx.runtime.chat.model;


import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.runtime.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a chat model during text generation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * ChatParameters params = ChatParameters.builder()
 *     .temperature(0.7)
 *     .maxTokens(100)
 *     .toolChoiceOption(ToolChoice.AUTO)
 *     .responseFormat(ResponseFormat.JSON)
 *     .build();
 * }</pre>
 *
 * The parameters encapsulated in this class control aspects such as output randomness, token limits, tool invocation, stopping criteria, and
 * reproducibility.
 */
public final class ChatParameters extends WatsonxParameters {

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
    private final String responseFormat;

    public ChatParameters(Builder builder) {
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
        this.responseFormat = nonNull(builder.responseFormat) ? builder.responseFormat.type() : null;
        this.toolChoice =
            nonNull(builder.toolChoice) ? Map.of("type", "function", "function", Map.of("name", builder.toolChoice))
                : null;
    }

    public static Builder builder() {
        return new Builder();
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

    /**
     * Builder class for constructing {@link ChatParameters} instances with configurable parameters.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private ToolChoice toolChoiceOption;
        private String toolChoice;
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

        /**
         * Specifies the tool selection strategy.
         * <p>
         * When set to {@code ToolChoice.AUTO}, the model decides whether to invoke tools.
         * <p>
         * If set to {@code ToolChoice.REQUIRED}, the model is forced to invoke a specific tool.
         *
         * @param toolChoiceOption a {@link ToolChoice} enum indicating the tool selection strategy
         */
        public Builder toolChoiceOption(ToolChoice toolChoiceOption) {
            this.toolChoiceOption = requireNonNull(toolChoiceOption);
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
         * Increasing or decreasing probability of tokens being selected during generation; a positive bias makes a token more likely to appear, while a
         * negative bias makes it less likely.
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
         * An integer specifying the number of most likely tokens to return at each token position, each with an associated log probability. The option
         * logprobs must be set to true if this parameter is used.
         *
         * @param topLogprobs the number of top tokens with logprobs to return
         */
        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        /**
         * The maximum number of tokens that can be generated in the chat completion. The total length of input tokens and generated tokens is limited by the
         * model's context length. Set to 0 for the model's configured max generated tokens.
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
         * What sampling temperature to use,. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused
         * and deterministic. We generally recommend altering this or {@code top_p} but not both. Valid range: (0, 2).
         *
         * @param temperature the sampling temperature
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability
         * mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. We generally recommend altering this or
         * {@code temperature} but not both. Valid range: (0, 1).
         *
         * @param topP the nucleus sampling threshold
         */
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        /**
         * Sets a maximum time limit for the completion generation. If this time is exceeded, the generation halts and returns what's generated so far.
         *
         * @param timeLimit {@link Duration} time limit.
         */
        public Builder timeLimit(Duration timeLimit) {
            this.timeLimit = requireNonNullElse(timeLimit, Duration.ofSeconds(10)).toMillis();
            return this;
        }

        /**
         * Sets the response format. Currently supports JSON format.
         *
         * @param responseFormat the desired response format
         */
        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = requireNonNull(responseFormat);
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
         * @return the builder instance
         */
        public Builder stop(List<String> stop) {
            this.stop = stop;
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
        JSON("json_object");

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
    public static enum ToolChoice {
        /**
         * The model will automatically decide whether to generate a message or invoke a tool.
         */
        AUTO("auto"),

        /**
         * The model is required to invoke a specific tool and cannot choose freely.
         */
        REQUIRED("required");

        private final String type;

        ToolChoice(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }
    }
}