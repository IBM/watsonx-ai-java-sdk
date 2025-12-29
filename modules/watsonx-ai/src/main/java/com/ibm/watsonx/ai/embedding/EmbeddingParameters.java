/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxCryptoParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingRequest.Parameters;
import com.ibm.watsonx.ai.embedding.EmbeddingRequest.ReturnOptions;

/**
 * Represents a set of parameters used to control the behavior of embedding generation.
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
public final class EmbeddingParameters extends WatsonxCryptoParameters {
    private final Integer truncateInputTokens;
    private final Boolean inputText;

    private EmbeddingParameters(Builder builder) {
        super(builder);
        truncateInputTokens = builder.truncateInputTokens;
        inputText = builder.inputText;
    }

    /**
     * Returns the maximum number of tokens accepted per input.
     *
     * @return the maximum number of input tokens, or {@code null} if not set
     */
    public Integer truncateInputTokens() {
        return truncateInputTokens;
    }

    /**
     * Returns whether to include the input text in each result document.
     *
     * @return {@code true} to include input text, {@code false} otherwise, or {@code null} if not set
     */
    public Boolean inputText() {
        return inputText;
    }

    Parameters toEmbeddingRequestParameters() {
        Parameters parameters = null;
        ReturnOptions returnOptions = null;

        if (nonNull(inputText))
            returnOptions = new ReturnOptions(inputText);

        if (nonNull(truncateInputTokens) || nonNull(returnOptions))
            parameters = new Parameters(truncateInputTokens, returnOptions);

        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * EmbeddingParameters.builder()
     *     .truncateInputTokens(512)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EmbeddingParameters} instances.
     */
    public static final class Builder extends WatsonxCryptoParameters.Builder<Builder> {
        private Integer truncateInputTokens;
        private Boolean inputText;

        private Builder() {}

        /**
         * Sets the maximum number of tokens accepted per input.
         * <p>
         * This can be used to avoid requests failing due to input being longer than configured limits. If the text is truncated, it truncates the end
         * of the input (on the right), so the start of the input will remain the same.
         *
         * @param truncateInputTokens the maximum number of input tokens
         */
        public Builder truncateInputTokens(Integer truncateInputTokens) {
            this.truncateInputTokens = truncateInputTokens;
            return this;
        }

        /**
         * Sets whether to include the input text in each of the result documents.
         *
         * @param inputText {@code true} to include input text, {@code false} otherwise
         */
        public Builder inputText(Boolean inputText) {
            this.inputText = inputText;
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
