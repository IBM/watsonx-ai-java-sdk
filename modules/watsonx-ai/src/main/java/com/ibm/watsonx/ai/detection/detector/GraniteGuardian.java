/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * Granite Guardian detector.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * GraniteGuardian detector = GraniteGuardian.builder()
 *     .threshold(0.5)
 *     .build();
 * }</pre>
 */
public final class GraniteGuardian extends BaseDetector {

    private GraniteGuardian(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * GraniteGuardian detector = GraniteGuardian.builder()
     *     .threshold(0.5)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link GraniteGuardian} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("granite_guardian");
        }

        /**
         * Builds a {@link GraniteGuardian} instance using the configured parameters.
         *
         * @return a new instance of {@link GraniteGuardian}
         */
        public GraniteGuardian build() {
            return new GraniteGuardian(this);
        }
    }
}
