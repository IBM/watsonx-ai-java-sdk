/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxModelParameters;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Represents a set of parameters used to control the behavior of a text generation request.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextGenerationParameters.builder()
 *     .decodingMethod("greedy")
 *     .maxNewTokens(512)
 *     .timeLimit(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 *
 */
public final class TextGenerationParameters extends WatsonxModelParameters {

    /**
     * Represents an exponential length penalty configuration to influence when text generation should terminate.
     * <p>
     * The penalty exponentially increases the likelihood of stopping generation once a specific number of tokens has been generated.
     * <p>
     * This configuration may be ignored in future service implementations.
     *
     * @param decayFactor the exponential decay factor; must be greater than 1.0. Larger values lead to more aggressive penalties.
     * @param startIndex the index (number of tokens generated) at which the penalty begins to apply; must be ≥ 0
     */
    public record LengthPenalty(double decayFactor, int startIndex) {}

    private final String decodingMethod;
    private final LengthPenalty lengthPenalty;
    private final Integer maxNewTokens;
    private final Integer minNewTokens;
    private final Integer randomSeed;
    private final List<String> stopSequences;
    private final Double temperature;
    private Long timeLimit;
    private final Integer topK;
    private final Double topP;
    private final Double repetitionPenalty;
    private final Integer truncateInputTokens;
    private final ReturnOptions returnOptions;
    private final Boolean includeStopSequence;
    private Map<String, String> promptVariables;

    private TextGenerationParameters(Builder builder) {
        super(builder);
        decodingMethod = builder.decodingMethod;
        lengthPenalty = builder.lengthPenalty;
        maxNewTokens = builder.maxNewTokens;
        minNewTokens = builder.minNewTokens;
        randomSeed = builder.randomSeed;
        stopSequences = builder.stopSequences;
        temperature = builder.temperature;
        timeLimit = builder.timeLimit;
        topK = builder.topK;
        topP = builder.topP;
        repetitionPenalty = builder.repetitionPenalty;
        truncateInputTokens = builder.truncateInputTokens;
        returnOptions = builder.returnOptions;
        includeStopSequence = builder.includeStopSequence;
        promptVariables = builder.promptVariables;
    }

    /**
     * Gets the decoding method used for text generation.
     *
     * @return the decoding method
     */
    public String decodingMethod() {
        return decodingMethod;
    }

    /**
     * Gets the length penalty configuration.
     *
     * @return the length penalty settings, or null if not configured
     */
    public LengthPenalty lengthPenalty() {
        return lengthPenalty;
    }

    /**
     * Gets the maximum number of new tokens to generate.
     *
     * @return the maximum number of new tokens
     */
    public Integer maxNewTokens() {
        return maxNewTokens;
    }

    /**
     * Gets the minimum number of new tokens to generate.
     *
     * @return the minimum number of new tokens
     */
    public Integer minNewTokens() {
        return minNewTokens;
    }

    /**
     * Gets the random seed for reproducible generation.
     *
     * @return the random seed value
     */
    public Integer randomSeed() {
        return randomSeed;
    }

    /**
     * Gets the list of stop sequences that will terminate generation.
     *
     * @return the list of stop sequences
     */
    public List<String> stopSequences() {
        return stopSequences;
    }

    /**
     * Gets the temperature value for controlling randomness in generation.
     *
     * @return the temperature value
     */
    public Double temperature() {
        return temperature;
    }

    /**
     * Gets the time limit for generation in milliseconds.
     *
     * @return the time limit in milliseconds
     */
    public Long timeLimit() {
        return timeLimit;
    }

    /**
     * Gets the top-k sampling parameter.
     *
     * @return the top-k value
     */
    public Integer topK() {
        return topK;
    }

    /**
     * Gets the top-p sampling parameter.
     *
     * @return the top-p value
     */
    public Double topP() {
        return topP;
    }

    /**
     * Gets the repetition penalty to discourage repeated tokens.
     *
     * @return the repetition penalty value
     */
    public Double repetitionPenalty() {
        return repetitionPenalty;
    }

    /**
     * Gets the maximum number of input tokens to use.
     *
     * @return the truncate input tokens value
     */
    public Integer truncateInputTokens() {
        return truncateInputTokens;
    }

    /**
     * Gets the return options configuration.
     *
     * @return the return options settings
     */
    public ReturnOptions returnOptions() {
        return returnOptions;
    }

    /**
     * Gets whether to include the stop sequence in the generated text.
     *
     * @return true if stop sequence should be included, false otherwise
     */
    public Boolean includeStopSequence() {
        return includeStopSequence;
    }

    /**
     * Gets the prompt variables for template substitution.
     *
     * @return the map of prompt variables
     */
    public Map<String, String> promptVariables() {
        return promptVariables;
    }

    void setTimeLimit(Long timeLimit) {
        this.timeLimit = timeLimit;
    }

    void setPromptVariables(Map<String, String> promptVariables) {
        this.promptVariables = promptVariables;
    }

    /**
     * Creates a new {@link Builder} instance initialized with all the current {@link TextGenerationParameters} values, except for {@code modelId},
     * {@code projectId}, {@code spaceId}, and {@code transactionId}.
     *
     * @return a {@link TextGenerationParameters} pre-populated with the current object's values, excluding any fields that must be omitted.
     */
    public TextGenerationParameters toSanitized() {
        var builder = new Builder()
            .decodingMethod(decodingMethod)
            .includeStopSequence(includeStopSequence)
            .maxNewTokens(maxNewTokens)
            .minNewTokens(minNewTokens)
            .promptVariables(promptVariables)
            .randomSeed(randomSeed)
            .repetitionPenalty(repetitionPenalty)
            .returnOptions(returnOptions)
            .stopSequences(stopSequences)
            .temperature(temperature)
            .topK(topK)
            .topP(topP)
            .truncateInputTokens(truncateInputTokens);

        if (nonNull(lengthPenalty))
            builder.lengthPenalty(lengthPenalty.decayFactor(), lengthPenalty.startIndex());

        if (nonNull(timeLimit))
            builder.timeLimit(Duration.ofMillis(timeLimit));

        return builder.build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextGenerationParameters.builder()
     *     .decodingMethod("greedy")
     *     .maxNewTokens(512)
     *     .timeLimit(Duration.ofSeconds(10))
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextGenerationParameters} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxModelParameters.Builder<Builder> {
        private String decodingMethod;
        private LengthPenalty lengthPenalty;
        private Integer maxNewTokens;
        private Integer minNewTokens;
        private Integer randomSeed;
        private List<String> stopSequences;
        private Double temperature;
        private Long timeLimit;
        private Integer topK;
        private Double topP;
        private Double repetitionPenalty;
        private Integer truncateInputTokens;
        private ReturnOptions returnOptions;
        private Boolean includeStopSequence;
        private Map<String, String> promptVariables;

        private Builder() {}

        /**
         * Sets the decoding strategy to use during text generation.
         * <p>
         * Allowed values are {@code sample} and {@code greedy}.
         * <ul>
         * <li>{@code greedy} selects the most probable token at each step.</li>
         * <li>{@code sample} selects the next token based on a probability distribution influenced by {@code top_k} and {@code top_p}.</li>
         * </ul>
         *
         * @param decodingMethod the decoding method to use
         */
        public Builder decodingMethod(String decodingMethod) {
            this.decodingMethod = decodingMethod;
            return this;
        }

        /**
         * Applies an exponential length penalty to encourage the generation to stop after a certain number of tokens.
         *
         * @param decayFactor the exponential decay factor (must be > 1)
         * @param startIndex the token index after which the penalty takes effect (must be ≥ 0)
         */
        public Builder lengthPenalty(double decayFactor, int startIndex) {
            this.lengthPenalty = new LengthPenalty(decayFactor, startIndex);
            return this;
        }

        /**
         * Sets the maximum number of new tokens to generate.
         *
         * @param maxNewTokens maximum number of new tokens (must be ≥ 0)
         */
        public Builder maxNewTokens(Integer maxNewTokens) {
            this.maxNewTokens = maxNewTokens;
            return this;
        }

        /**
         * Sets the minimum number of new tokens to generate.
         * <p>
         * If stop sequences are provided, they will be ignored until at least this number of tokens has been generated.
         *
         * @param minNewTokens minimum number of new tokens (must be ≥ 0)
         */
        public Builder minNewTokens(Integer minNewTokens) {
            this.minNewTokens = minNewTokens;
            return this;
        }

        /**
         * Sets the seed for random number generation to ensure repeatability in sampling mode.
         *
         * @param randomSeed the seed to use (must be ≥ 1)
         */
        public Builder randomSeed(Integer randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }

        /**
         * Defines stop sequences that end the generation when encountered.
         * <p>
         * A maximum of 6 unique stop sequences is allowed. Stop sequences are ignored until the minimum number of tokens is reached.
         *
         * @param stopSequences list of stop sequences (0–6 unique strings)
         */
        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        /**
         * Sets the sampling temperature to use when decoding.
         * <p>
         * Higher values (e.g., 1.5) produce more random output; lower values (e.g., 0.2) make output more focused and deterministic.
         * <p>
         * Valid range: {@code 0.05} to {@code 2.0}.
         *
         * @param temperature sampling temperature
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
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
         * Sets the {@code top_k} value for sampling.
         * <p>
         * Only the {@code top_k} most likely tokens are considered when sampling the next token. Applicable only when using {@code sample} decoding.
         *
         * @param topK number of top tokens to consider (1–100)
         */
        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        /**
         * Sets the {@code top_p} (nucleus sampling) threshold.
         * <p>
         * Only tokens whose cumulative probability adds up to {@code top_p} are considered for the next token. Applicable only when using
         * {@code sample} decoding.
         * <p>
         * Valid range: ({@code 0.0}, {@code 1.0}].
         *
         * @param topP cumulative probability threshold
         */
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        /**
         * Applies a penalty to discourage repetition of previously generated tokens.
         * <p>
         * A value of {@code 1.0} means no penalty. Higher values penalize repetition more.
         *
         * @param repetitionPenalty repetition penalty factor ({@code 1.0} – {@code 2.0})
         */
        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        /**
         * Sets the maximum number of input tokens to accept.
         * <p>
         * If the input exceeds this value, it will be truncated from the start (left-truncated).
         *
         * @param truncateInputTokens maximum number of input tokens (must be ≥ 1)
         */
        public Builder truncateInputTokens(Integer truncateInputTokens) {
            this.truncateInputTokens = truncateInputTokens;
            return this;
        }

        /**
         * Sets the return options that control the format and content of the output.
         *
         * @param returnOptions return options configuration
         */
        public Builder returnOptions(ReturnOptions returnOptions) {
            this.returnOptions = returnOptions;
            return this;
        }


        /**
         * Controls whether matched stop sequences should be included in the output.
         * <p>
         * Set to {@code false} to omit matched stop sequences from the end of the output text. Default is {@code true}.
         *
         * @param includeStopSequence whether to include matched stop sequences in the output
         */
        public Builder includeStopSequence(Boolean includeStopSequence) {
            this.includeStopSequence = includeStopSequence;
            return this;
        }

        /**
         * Sets the map of prompt variables to be injected into a parameterized prompt template.
         * <p>
         * This method is used to assign values to placeholder variables defined in a prompt template associated with a deployment. These variables
         * are resolved server-side before generating the response.
         * <p>
         * <strong>Note:</strong> This method is only applicable when using the {@link DeploymentService}, and has no effect in
         * {@link TextGenerationService}, where prompt templates and variables are not supported.
         *
         * @param promptVariables a map of variable names to their corresponding replacement values
         * @return this {@code Builder} instance for method chaining
         */
        public Builder promptVariables(Map<String, String> promptVariables) {
            this.promptVariables = promptVariables;
            return this;
        }

        /**
         * Builds a {@link TextGenerationParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextGenerationParameters}
         */
        public TextGenerationParameters build() {
            return new TextGenerationParameters(this);
        }
    }

    /**
     * Represents a set of parameters used to control the behavior of text generation request.
     * <p>
     * Instances of this class are created using the {@link Builder} pattern:
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ReturnOptions.builder()
     *     .inputText(true)
     *     .tokenLogprobs(true)
     *     .tokenRanks(true)
     *     .build();
     * }</pre>
     *
     */
    public static class ReturnOptions {
        private final Boolean inputText;
        private final Boolean generatedText;
        private final Boolean inputTokens;
        private final Boolean tokenLogprobs;
        private final Boolean tokenRanks;
        private final Integer topNTokens;

        private ReturnOptions(Builder builder) {
            this.inputText = builder.inputText;
            this.generatedText = builder.generatedText;
            this.inputTokens = builder.inputTokens;
            this.tokenLogprobs = builder.tokenLogprobs;
            this.tokenRanks = builder.tokenRanks;
            this.topNTokens = builder.topNTokens;
        }

        /**
         * Gets whether to include the input text in the response.
         *
         * @return true if input text should be included
         */
        public Boolean getInputText() {
            return inputText;
        }

        /**
         * Gets whether to include the generated text in the response.
         *
         * @return true if generated text should be included
         */
        public Boolean getGeneratedText() {
            return generatedText;
        }

        /**
         * Gets whether to include input tokens in the response.
         *
         * @return true if input tokens should be included
         */
        public Boolean getInputTokens() {
            return inputTokens;
        }

        /**
         * Gets whether to include token log probabilities in the response.
         *
         * @return true if token log probabilities should be included
         */
        public Boolean getTokenLogprobs() {
            return tokenLogprobs;
        }

        /**
         * Gets whether to include token ranks in the response.
         *
         * @return true if token ranks should be included
         */
        public Boolean getTokenRanks() {
            return tokenRanks;
        }

        /**
         * Gets the number of top tokens to return for each position.
         *
         * @return the number of top tokens
         */
        public Integer getTopN_Tokens() {
            return topNTokens;
        }

        /**
         * Returns a new {@link Builder} instance.
         * <p>
         * <b>Example usage:</b>
         *
         * <pre>{@code
         * ReturnOptions.builder()
         *     .inputText(true)
         *     .tokenLogprobs(true)
         *     .tokenRanks(true)
         *     .build();
         * }</pre>
         *
         * @return {@link Builder} instance.
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder class for constructing {@link ReturnOptions} instances with configurable parameters.
         */
        public static class Builder {
            private Boolean inputText;
            private Boolean generatedText;
            private Boolean inputTokens;
            private Boolean tokenLogprobs;
            private Boolean tokenRanks;
            private Integer topNTokens;

            private Builder() {}

            /**
             * Specifies whether to include the original input text in the output.
             *
             * @param inputText whether to include the input text
             */
            public Builder inputText(boolean inputText) {
                this.inputText = inputText;
                return this;
            }

            /**
             * Specifies whether to include the list of generated tokens in the response.
             *
             * @param generatedText whether to include generated tokens
             */
            public Builder generatedText(boolean generatedText) {
                this.generatedText = generatedText;
                return this;
            }

            /**
             * Specifies whether to include the list of input tokens in the response.
             *
             * @param inputTokens whether to include input tokens
             */
            public Builder inputTokens(boolean inputTokens) {
                this.inputTokens = inputTokens;
                return this;
            }

            /**
             * Specifies whether to include the log probability (natural log of token probability) for each returned token.
             *
             * @param tokenLogprobs whether to include log probabilities for tokens
             */
            public Builder tokenLogprobs(boolean tokenLogprobs) {
                this.tokenLogprobs = tokenLogprobs;
                return this;
            }

            /**
             * Specifies whether to include the rank of each returned token.
             *
             * @param tokenRanks whether to include token ranks
             */
            public Builder tokenRanks(boolean tokenRanks) {
                this.tokenRanks = tokenRanks;
                return this;
            }

            /**
             * Specifies how many of the top candidate tokens to return for each position.
             *
             * @param topNTokens number of top candidate tokens to return (must be ≥ 0)
             */
            public Builder topNTokens(int topNTokens) {
                this.topNTokens = topNTokens;
                return this;
            }

            /**
             * Builds a {@link ReturnOptions} instance using the configured parameters.
             *
             * @return a new instance of {@link ReturnOptions}
             */
            public ReturnOptions build() {
                return new ReturnOptions(this);
            }
        }
    }
}
