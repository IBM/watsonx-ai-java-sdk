/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxParameters;

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
public class TextGenerationParameters extends WatsonxParameters {

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

    public TextGenerationParameters(Builder builder) {
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
    }

    public String getDecodingMethod() {
        return decodingMethod;
    }

    public LengthPenalty getLengthPenalty() {
        return lengthPenalty;
    }

    public Integer getMaxNewTokens() {
        return maxNewTokens;
    }

    public Integer getMinNewTokens() {
        return minNewTokens;
    }

    public Integer getRandomSeed() {
        return randomSeed;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public Integer getTopK() {
        return topK;
    }

    public Double getTopP() {
        return topP;
    }

    public Double getRepetitionPenalty() {
        return repetitionPenalty;
    }

    public Integer getTruncateInputTokens() {
        return truncateInputTokens;
    }

    public ReturnOptions getReturnOptions() {
        return returnOptions;
    }

    public Boolean getIncludeStopSequence() {
        return includeStopSequence;
    }

    void setTimeLimit(Long timeLimit) {
        this.timeLimit = timeLimit;
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
    public static class Builder extends WatsonxParameters.Builder<Builder> {
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
         * Sets a maximum time limit for the completion generation. If this time is exceeded, the generation halts and returns what's generated so
         * far.
         *
         * @param timeLimit {@link Duration} time limit.
         */
        public Builder timeLimit(Duration timeLimit) {
            this.timeLimit = requireNonNullElse(timeLimit, Duration.ofSeconds(10)).toMillis();
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

        public ReturnOptions(Builder builder) {
            this.inputText = builder.inputText;
            this.generatedText = builder.generatedText;
            this.inputTokens = builder.inputTokens;
            this.tokenLogprobs = builder.tokenLogprobs;
            this.tokenRanks = builder.tokenRanks;
            this.topNTokens = builder.topNTokens;
        }

        public Boolean getInputText() {
            return inputText;
        }

        public Boolean getGeneratedText() {
            return generatedText;
        }

        public Boolean getInputTokens() {
            return inputTokens;
        }

        public Boolean getTokenLogprobs() {
            return tokenLogprobs;
        }

        public Boolean getTokenRanks() {
            return tokenRanks;
        }

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
