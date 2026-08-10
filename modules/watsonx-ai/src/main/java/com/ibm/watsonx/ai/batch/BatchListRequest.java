/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to list batch jobs using the watsonx.ai Batches APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * BatchListRequest.builder()
 *     .limit(10)
 *     .build();
 * }</pre>
 */
public final class BatchListRequest extends WatsonxParameters {
    private final Integer limit;

    private BatchListRequest(Builder builder) {
        super(builder);
        limit = builder.limit;
    }

    /**
     * Returns the maximum number of batch jobs to return.
     *
     * @return the page size limit
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * BatchListRequest.builder()
     *     .limit(10)
     *     .build();
     * }</pre>
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link BatchListRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private Integer limit;

        private Builder() {}

        /**
         * Sets the maximum number of batch jobs to return. Must be between 1 and 100. Defaults to {@code 20}.
         *
         * @param limit the page size limit
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Builds a {@link BatchListRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link BatchListRequest}
         */
        public BatchListRequest build() {
            return new BatchListRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((limit == null) ? 0 : limit.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        BatchListRequest other = (BatchListRequest) obj;
        if (limit == null) {
            if (other.limit != null)
                return false;
        } else if (!limit.equals(other.limit))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BatchListRequest [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", limit=" + limit + "]";
    }
}
