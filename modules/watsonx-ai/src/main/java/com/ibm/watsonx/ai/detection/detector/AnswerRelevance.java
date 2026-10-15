/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

/**
 * A detector that evaluates the relevance of a generated answer with respect to the original prompt.
 * <p>
 * Answer relevance measures whether the generated text actually addresses the question asked.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * AnswerRelevance detector = AnswerRelevance.builder()
 *     .threshold(0.5)
 *     .build();
 * }</pre>
 */
public final class AnswerRelevance extends BaseDetector {

    /**
     * The method used to compute the answer relevance metric.
     */
    public enum Method {
        GRANITE_GUARDIAN("granite_guardian"),
        ANSWER_RELEVANCE_MODEL("answer_relevance_model");

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

    private AnswerRelevance(Builder builder) {
        super(builder);
    }

    /**
     * Returns an empty {@link AnswerRelevance} detector instance with no properties configured.
     *
     * @return an empty {@link AnswerRelevance} instance.
     */
    public static AnswerRelevance ofDefaults() {
        return builder().build();
    }

    /**
     * Returns an {@link AnswerRelevance} detector instance configured with the specified threshold.
     *
     * @param threshold the detection threshold value to configure
     * @return an {@link AnswerRelevance} instance with the specified threshold
     */
    public static AnswerRelevance ofThreshold(double threshold) {
        return builder().threshold(threshold).build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * AnswerRelevance detector = AnswerRelevance.builder()
     *     .threshold(0.5)
     *     .method(AnswerRelevance.Method.GRANITE_GUARDIAN)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link AnswerRelevance} instances with configurable parameters.
     */
    public final static class Builder extends BaseDetector.Builder<Builder> {

        private Builder() {
            super("answer_relevance");
        }

        /**
         * Sets the method used to compute answer relevance.
         *
         * @param method the computation method
         * @return this builder
         */
        public Builder method(String method) {
            return addProperty("method", method);
        }

        /**
         * Sets the method used to compute answer relevance.
         *
         * @param method the computation method
         * @return this builder
         * @see #method(String)
         */
        public Builder method(Method method) {
            return method(method == null ? null : method.value());
        }

        /**
         * Builds an {@link AnswerRelevance} instance using the configured parameters.
         *
         * @return a new instance of {@link AnswerRelevance}
         */
        public AnswerRelevance build() {
            return new AnswerRelevance(this);
        }
    }

    @Override
    public String toString() {
        return "AnswerRelevance [name=" + name() + ", properties=" + properties() + "]";
    }
}
