/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector specialized of a Hate and profanity (HAP) text detector.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * Hap detector = Hap.builder()
 *     .threshold(0.5)
 *     .build();
 * }</pre>
 */
public final class Hap extends BaseDetector {

    private Hap(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * Hap detector = Hap.builder()
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
     * Builder class for constructing {@link Hap} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("hap");
        }

        /**
         * Builds a {@link Hap} instance using the configured parameters.
         *
         * @return a new instance of {@link Hap}
         */
        public Hap build() {
            return new Hap(this);
        }
    }
}
