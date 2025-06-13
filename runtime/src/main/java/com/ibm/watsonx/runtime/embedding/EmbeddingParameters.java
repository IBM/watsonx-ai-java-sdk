package com.ibm.watsonx.runtime.embedding;

import com.ibm.watsonx.runtime.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a embedding generation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 * 
 * <pre>{@code
 * EmbeddingParameters.builder()
 *     .truncateInputTokens(512)
 *     .build();
 * }</pre>
 *
 */
public final class EmbeddingParameters extends WatsonxParameters {
    private final Integer truncateInputTokens;

    public EmbeddingParameters(Builder builder) {
        super(builder);
        truncateInputTokens = builder.truncateInputTokens;
    }

    public Integer getTruncateInputTokens() {
        return truncateInputTokens;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EmbeddingParameters} instances.
     */
    public static final class Builder extends WatsonxParameters.Builder<Builder> {
        private Integer truncateInputTokens;

        /**
         * Represents the maximum number of tokens accepted per input.
         * 
         * This can be used to avoid requests failing due to input being longer than configured limits. If the text is truncated, then it truncates the end of
         * the input (on the right), so the start of the input will remain the same.
         * 
         * If this value exceeds the maximum sequence length (refer to the documentation to find this value for the model) then the call will fail if the
         * total number of tokens exceeds the maximum sequence length.
         * 
         * @param truncateInputTokens Integer value.
         */
        public Builder truncateInputTokens(Integer truncateInputTokens) {
            this.truncateInputTokens = truncateInputTokens;
            return this;
        }

        /**
         * Builds a {@link EmbeddingParameters} instance.
         *
         * @return a new instance of {@link EmbeddingParameters}
         */
        public EmbeddingParameters build() {
            return new EmbeddingParameters(this);
        }
    }
}
