/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxCryptoParameters;
import com.ibm.watsonx.ai.rerank.RerankPayload.Parameters;
import com.ibm.watsonx.ai.rerank.RerankPayload.ReturnOptions;

/**
 * Represents a set of parameters used to control the behavior of a rerank operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * RerankParameters.builder()
 *     .truncateInputTokens(512)
 *     .build();
 * }</pre>
 *
 */
public final class RerankParameters extends WatsonxCryptoParameters {
    private final Integer truncateInputTokens;
    private final Integer topN;
    private final Boolean returnInputs;
    private final Boolean returnQuery;

    private RerankParameters(Builder builder) {
        super(builder);
        truncateInputTokens = builder.truncateInputTokens;
        topN = builder.topN;
        returnInputs = builder.returnInputs;
        returnQuery = builder.returnQuery;
    }

    /**
     * Returns the maximum number of tokens allowed per input.
     *
     * @return the maximum number of tokens, or {@code null} if not set
     */
    public Integer truncateInputTokens() {
        return truncateInputTokens;
    }

    /**
     * Returns the number of top-ranked results to return.
     *
     * @return the top N value, or {@code null} if not set
     */
    public Integer topN() {
        return topN;
    }

    /**
     * Returns whether to include input strings in the response.
     *
     * @return {@code true} to include inputs, {@code false} otherwise, or {@code null} if not set
     */
    public Boolean returnInputs() {
        return returnInputs;
    }

    /**
     * Returns whether to include the query in the response.
     *
     * @return {@code true} to include query, {@code false} otherwise, or {@code null} if not set
     */
    public Boolean returnQuery() {
        return returnQuery;
    }

    Parameters toRerankRequestParameters() {

        Parameters parameters = null;
        ReturnOptions returnOptions = null;

        if (nonNull(topN) || nonNull(returnInputs) || nonNull(returnQuery))
            returnOptions = new ReturnOptions(topN, returnInputs, returnQuery);

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
     * RerankParameters.builder()
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
     * Builder class for constructing {@link RerankParameters} instances.
     */
    public static final class Builder extends WatsonxCryptoParameters.Builder<Builder> {
        private Integer truncateInputTokens;
        private Integer topN;
        private Boolean returnInputs;
        private Boolean returnQuery;

        private Builder() {}

        /**
         * The maximum number of tokens allowed per input.
         * <p>
         * If the input exceeds this limit, it will be truncated from the end (right side).
         * <p>
         * Must be > 1.
         *
         * @param truncateInputTokens Integer value.
         */
        public Builder truncateInputTokens(Integer truncateInputTokens) {
            this.truncateInputTokens = truncateInputTokens;
            return this;
        }

        /**
         * If set, only the top {@code N} ranked results are returned.
         * <p>
         * Must be > 1.
         *
         * @param topN The number of top-ranked results to return
         */
        public Builder topN(Integer topN) {
            this.topN = topN;
            return this;
        }

        /**
         * Whether to return the input strings in the response.
         *
         * @param returnInputs Boolean value.
         */
        public Builder returnInputs(Boolean returnInputs) {
            this.returnInputs = returnInputs;
            return this;
        }

        /**
         * Whether to return the query in the response.
         *
         * @param returnQuery Boolean value.
         */
        public Builder returnQuery(Boolean returnQuery) {
            this.returnQuery = returnQuery;
            return this;
        }

        /**
         * Builds a {@link RerankParameters} instance.
         *
         * @return a new instance of {@link RerankParameters}
         */
        public RerankParameters build() {
            return new RerankParameters(this);
        }
    }

    @Override
    public String toString() {
        return "RerankParameters [" + super.toString() + ", truncateInputTokens=" + truncateInputTokens + ", topN=" + topN + ", returnInputs="
            + returnInputs + ", returnQuery=" + returnQuery + "]";
    }
}
