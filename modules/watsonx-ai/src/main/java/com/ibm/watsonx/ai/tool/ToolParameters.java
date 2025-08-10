/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import com.ibm.watsonx.ai.foundationmodel.FoundationModelParameters;

/**
 * Represents a set of parameters used to control the behavior of a Tool APIs.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ToolParameters.builder()
 *     .transactionId("transaction-id")
 *     .build();
 * }</pre>
 *
 */
public class ToolParameters {
    private final String transactionId;

    protected ToolParameters(Builder builder) {
        this.transactionId = builder.transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolParameters.builder()
     *     .transactionId("transaction-id")
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
    public static class Builder {
        private String transactionId;

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
         * Builds a {@link ToolParameters} instance.
         *
         * @return a new instance of {@link ToolParameters}
         */
        public ToolParameters build() {
            return new ToolParameters(this);
        }
    }
}
