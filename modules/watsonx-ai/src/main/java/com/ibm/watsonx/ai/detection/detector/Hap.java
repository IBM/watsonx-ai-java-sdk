/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector specialized in identifying hate and profanity (HAP) content in text.
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
     * Returns an empty {@link Hap} detector instance with no properties configured.
     * <p>
     * This is equivalent to:
     *
     * <pre>{@code
     * Hap detector = Hap.builder().build();
     * }</pre>
     *
     * @return an empty {@link Hap} instance.
     */
    public static Hap ofDefaults() {
        return builder().build();
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
