/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.tokenization.TokenizationRequest.Parameters;

/**
 * Represents a set of parameters used to control the behavior of a tokenization request.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TokenizationParameters.builder()
 *     .returnTokens(true)
 *     .build();
 * }</pre>
 *
 */
public final class TokenizationParameters extends WatsonxParameters {
    private final Boolean returnTokens;

    public TokenizationParameters(Builder builder) {
        super(builder);
        returnTokens = builder.returnTokens;
    }

    public Boolean getReturnTokens() {
        return returnTokens;
    }

    Parameters toTokenizationRequestParameters() {
        Parameters parameters = null;

        if (nonNull(returnTokens))
            parameters = new Parameters(returnTokens);

        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TokenizationParameters.builder()
     *     .returnTokens(true)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TokenizationParameters} instances.
     */
    public static final class Builder extends WatsonxParameters.Builder<Builder> {
        private Boolean returnTokens;

        /**
         * Indicating if the response will include the actual tokens produced.
         *
         * @param returnTokens A boolean value indicating whether the tokens should be included in the response
         */
        public Builder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens;
            return this;
        }

        /**
         * Builds a {@link TokenizationParameters} instance.
         *
         * @return a new instance of {@link TokenizationParameters}
         */
        public TokenizationParameters build() {
            return new TokenizationParameters(this);
        }
    }
}
