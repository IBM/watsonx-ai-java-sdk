/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

/**
 * Represents a set of parameters used to control the behavior of a foundation models specs.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FoundationModelParameters.builder()
 *     .filter(Filter.of(modelId("ibm/granite-13b-instruct-v2")))
 *     .techPreview(true)
 *     .build();
 * }</pre>
 *
 */
public final class FoundationModelParameters {
    private final Integer start;
    private final Integer limit;
    private final Filter filter;
    private final String transactionId;
    private final Boolean techPreview;

    protected FoundationModelParameters(Builder builder) {
        this.start = builder.start;
        this.limit = builder.limit;
        this.filter = builder.filter;
        this.transactionId = builder.transactionId;
        this.techPreview = builder.techPreview;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getFilter() {
        return nonNull(filter) ? filter.toString() : null;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Boolean getTechPreview() {
        return techPreview;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FoundationModelParameters.builder()
     *     .filter(Filter.of(modelId("ibm/granite-13b-instruct-v2")))
     *     .techPreview(true)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FoundationModelParameters} instances.
     */
    public final static class Builder {
        private Integer start;
        private Integer limit;
        private Filter filter;
        private String transactionId;
        private Boolean techPreview;

        /**
         * Sets the pagination start token.
         *
         * @param start the pagination start token.
         */
        public Builder start(Integer start) {
            this.start = start;
            return this;
        }

        /**
         * Sets the maximum number of resources to return.
         *
         * @param limit the maximum number of resources to return.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the filters to apply.
         *
         * @param filter the filter object.
         */
        public Builder filter(Filter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets the transaction id for request tracking.
         *
         * @param transactionId the transaction id.
         */
        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        /**
         * Sets whether Tech Preview models should be included in the response.
         *
         * @param techPreview {@code true} to include Tech Preview models, {@code false} otherwise.
         */
        public Builder techPreview(Boolean techPreview) {
            this.techPreview = techPreview;
            return this;
        }

        /**
         * Builds a {@link FoundationModelParameters} instance.
         *
         * @return a new instance of {@link FoundationModelParameters}
         */
        public FoundationModelParameters build() {
            return new FoundationModelParameters(this);
        }
    }
}
