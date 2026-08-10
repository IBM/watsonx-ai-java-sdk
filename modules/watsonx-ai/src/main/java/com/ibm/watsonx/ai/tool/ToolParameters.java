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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
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
        ToolParameters other = (ToolParameters) obj;
        if (transactionId == null) {
            if (other.transactionId != null)
                return false;
        } else if (!transactionId.equals(other.transactionId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ToolParameters [transactionId=" + transactionId + "]";
    }
}
