/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to cancel an in-progress batch job using the watsonx.ai Batches APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * BatchCancelRequest.builder()
 *     .batchId("batch_id")
 *     .build();
 * }</pre>
 */
public class BatchCancelRequest extends WatsonxParameters {
    private final String batchId;

    private BatchCancelRequest(Builder builder) {
        super(builder);
        batchId = builder.batchId;
    }

    /**
     * Returns the identifier of the batch job to cancel.
     *
     * @return the batch job identifier
     */
    public String batchId() {
        return batchId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * BatchCancelRequest.builder()
     *     .batchId("batch_id")
     *     .build();
     * }</pre>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link BatchCancelRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String batchId;

        private Builder() {}

        /**
         * Sets the identifier of the batch job to cancel.
         *
         * @param batchId the batch job identifier
         */
        public Builder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        /**
         * Builds a {@link BatchCancelRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link BatchCancelRequest}
         */
        public BatchCancelRequest build() {
            return new BatchCancelRequest(this);
        }
    }
}
