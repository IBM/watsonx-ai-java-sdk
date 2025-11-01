/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector specialized in identifying Personally Identifiable Information (PII) in text content.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * Pii detector = Pii.ofDefaults();
 * }</pre>
 */
public final class Pii extends BaseDetector {

    private Pii(Builder builder) {
        super(builder);
    }

    /**
     * Returns an empty {@link Pii} detector instance with no properties configured.
     * <p>
     * This is equivalent to:
     *
     * <pre>{@code
     * Pii detector = Pii.builder().build();
     * }</pre>
     *
     * @return an empty {@link Pii} instance.
     */
    public static Pii ofDefaults() {
        return builder().build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * Pii detector = Pii.ofDefaults();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link Pii} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("pii");
        }

        /**
         * Builds a {@link Pii} instance using the configured parameters.
         *
         * @return a new instance of {@link Pii}
         */
        public Pii build() {
            return new Pii(this);
        }
    }
}
