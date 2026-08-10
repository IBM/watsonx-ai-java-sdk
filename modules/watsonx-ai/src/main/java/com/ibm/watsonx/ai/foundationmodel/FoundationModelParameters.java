/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

/**
 * Represents a set of parameters used to control the retrieval of foundation models.
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

    private FoundationModelParameters(Builder builder) {
        this.start = builder.start;
        this.limit = builder.limit;
        this.filter = builder.filter;
        this.transactionId = builder.transactionId;
        this.techPreview = builder.techPreview;
    }

    /**
     * Returns the starting index for pagination.
     *
     * @return the start index
     */
    public Integer start() {
        return start;
    }

    /**
     * Returns the maximum number of models to return.
     *
     * @return the limit
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Returns the filter expression as a string.
     *
     * @return the filter string, or {@code null} if no filter is set
     */
    public String filter() {
        return nonNull(filter) ? filter.toString() : null;
    }

    /**
     * Returns the transaction identifier.
     *
     * @return the transaction id
     */
    public String transactionId() {
        return transactionId;
    }

    /**
     * Returns whether to include tech preview models.
     *
     * @return {@code true} to include tech preview models, {@code false} otherwise
     */
    public Boolean techPreview() {
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

        private Builder() {}

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((limit == null) ? 0 : limit.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
        result = prime * result + ((techPreview == null) ? 0 : techPreview.hashCode());
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
        FoundationModelParameters other = (FoundationModelParameters) obj;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        if (limit == null) {
            if (other.limit != null)
                return false;
        } else if (!limit.equals(other.limit))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (transactionId == null) {
            if (other.transactionId != null)
                return false;
        } else if (!transactionId.equals(other.transactionId))
            return false;
        if (techPreview == null) {
            if (other.techPreview != null)
                return false;
        } else if (!techPreview.equals(other.techPreview))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FoundationModelParameters [start=" + start + ", limit=" + limit + ", filter=" + filter + ", transactionId=" + transactionId
            + ", techPreview=" + techPreview + "]";
    }
}
