/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

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
public final class ToolParameters {
    private final String transactionId;

    private ToolParameters(Builder builder) {
        this.transactionId = builder.transactionId;
    }

    /**
     * Gets the transaction id.
     *
     * @return the transaction id
     */
    public String transactionId() {
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
     * Builder class for constructing {@link ToolParameters} instances.
     */
    public final static class Builder {
        private String transactionId;

        private Builder() {}

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
