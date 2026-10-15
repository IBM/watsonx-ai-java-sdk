/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector that evaluates context relevance for RAG-based pipelines.
 * <p>
 * Context relevance measures whether the retrieved context is relevant to the input query.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ContextRelevance detector = ContextRelevance.builder()
 *     .threshold(0.5)
 *     .build();
 * }</pre>
 */
public final class ContextRelevance extends BaseDetector {

    /**
     * The method used to compute the context relevance metric.
     */
    public enum Method {
        GRANITE_GUARDIAN("granite_guardian"),
        CONTEXT_RELEVANCE_MODEL("context_relevance_model");

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

    private ContextRelevance(Builder builder) {
        super(builder);
    }

    /**
     * Returns an empty {@link ContextRelevance} detector instance with no properties configured.
     *
     * @return an empty {@link ContextRelevance} instance.
     */
    public static ContextRelevance ofDefaults() {
        return builder().build();
    }

    /**
     * Returns a {@link ContextRelevance} detector instance configured with the specified threshold.
     *
     * @param threshold the detection threshold value to configure
     * @return a {@link ContextRelevance} instance with the specified threshold
     */
    public static ContextRelevance ofThreshold(double threshold) {
        return builder().threshold(threshold).build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ContextRelevance detector = ContextRelevance.builder()
     *     .threshold(0.5)
     *     .method(ContextRelevance.Method.GRANITE_GUARDIAN)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ContextRelevance} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("context_relevance");
        }

        /**
         * Sets the method used to compute context relevance.
         *
         * @param method the computation method
         * @return this builder
         */
        public Builder method(String method) {
            return addProperty("method", method);
        }

        /**
         * Sets the method used to compute context relevance.
         *
         * @param method the computation method
         * @return this builder
         * @see #method(String)
         */
        public Builder method(Method method) {
            return method(method == null ? null : method.value());
        }

        /**
         * Builds a {@link ContextRelevance} instance using the configured parameters.
         *
         * @return a new instance of {@link ContextRelevance}
         */
        public ContextRelevance build() {
            return new ContextRelevance(this);
        }
    }

    @Override
    public String toString() {
        return "ContextRelevance [name=" + name() + ", properties=" + properties() + "]";
    }
}
