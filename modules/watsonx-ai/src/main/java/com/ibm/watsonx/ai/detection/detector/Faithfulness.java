/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector that evaluates faithfulness (also known as groundedness) of a response with respect to the provided context.
 * <p>
 * Faithfulness measures whether the generated answer is supported by the retrieved context documents.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * Faithfulness detector = Faithfulness.builder()
 *     .threshold(0.5)
 *     .build();
 * }</pre>
 */
public final class Faithfulness extends BaseDetector {

    /**
     * The method used to compute the faithfulness (groundedness) metric.
     */
    public enum Method {
        GRANITE_GUARDIAN("granite_guardian"),
        FAITHFULNESS_MODEL("faithfulness_model");

        private final String value;

        Method(String value) {
            this.value = value;
        }

        /**
         * Returns the wire string value.
         *
         * @return the wire value
         */
        public String value() {
            return value;
        }
    }

    private Faithfulness(Builder builder) {
        super(builder);
    }

    /**
     * Returns an empty {@link Faithfulness} detector instance with no properties configured.
     *
     * @return an empty {@link Faithfulness} instance.
     */
    public static Faithfulness ofDefaults() {
        return builder().build();
    }

    /**
     * Returns a {@link Faithfulness} detector instance configured with the specified threshold.
     *
     * @param threshold the detection threshold value to configure
     * @return a {@link Faithfulness} instance with the specified threshold
     */
    public static Faithfulness ofThreshold(double threshold) {
        return builder().threshold(threshold).build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * Faithfulness detector = Faithfulness.builder()
     *     .threshold(0.5)
     *     .method(Faithfulness.Method.GRANITE_GUARDIAN)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link Faithfulness} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("faithfulness");
        }

        /**
         * Sets the method used to compute faithfulness (groundedness).
         *
         * @param method the computation method
         * @return this builder
         */
        public Builder method(String method) {
            return addProperty("method", method);
        }

        /**
         * Sets the method used to compute faithfulness (groundedness).
         *
         * @param method the computation method
         * @return this builder
         * @see #method(String)
         */
        public Builder method(Method method) {
            return method(method == null ? null : method.value());
        }

        /**
         * Builds a {@link Faithfulness} instance using the configured parameters.
         *
         * @return a new instance of {@link Faithfulness}
         */
        public Faithfulness build() {
            return new Faithfulness(this);
        }
    }

    @Override
    public String toString() {
        return "Faithfulness [name=" + name() + ", properties=" + properties() + "]";
    }
}
