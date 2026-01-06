/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

/**
 * Represents a JSON Schema of type {@code integer}.
 * <p>
 * Use {@link JsonSchema#integer()} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * JsonSchema.integer()
 *     .description("The age of the user")
 *     .nullable();
 * }</pre>
 *
 */
public final class IntegerSchema extends JsonSchema {
    private final Integer minimum;
    private final Integer maximum;
    private final Integer exclusiveMinimum;
    private final Integer exclusiveMaximum;
    private final Integer multipleOf;

    private IntegerSchema(Builder builder) {
        super(builder.nullable ? List.of("integer", "null") : "integer", builder);
        minimum = builder.minimum;
        maximum = builder.maximum;
        exclusiveMinimum = builder.exclusiveMinimum;
        exclusiveMaximum = builder.exclusiveMaximum;
        multipleOf = builder.multipleOf;
    }

    public Integer minimum() {
        return minimum;
    }

    public Integer maximum() {
        return maximum;
    }

    public Integer exclusiveMinimum() {
        return exclusiveMinimum;
    }

    public Integer exclusiveMaximum() {
        return exclusiveMaximum;
    }

    public Integer multipleOf() {
        return multipleOf;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#integer()} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.integer()
     *     .description("The age of the user")
     *     .nullable();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link IntegerSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, IntegerSchema, IntegerSchema.Builder> {
        private Integer minimum;
        private Integer maximum;
        private Integer exclusiveMinimum;
        private Integer exclusiveMaximum;
        private Integer multipleOf;

        private Builder() {}

        /**
         * Sets the minimum allowed value for the number.
         *
         * @param minimum the minimum allowed numeric value
         */
        public Builder minimum(int minimum) {
            this.minimum = minimum;
            return this;
        }

        /**
         * Sets the maximum allowed value for the number.
         *
         * @param maximum the maximum allowed numeric value
         */
        public Builder maximum(int maximum) {
            this.maximum = maximum;
            return this;
        }

        /**
         * Sets an exclusive lower bound for the number value.
         *
         * @param exclusiveMinimum the exclusive lower bound for the number
         */
        public Builder exclusiveMinimum(int exclusiveMinimum) {
            this.exclusiveMinimum = exclusiveMinimum;
            return this;
        }

        /**
         * Sets an exclusive upper bound for the number value.
         *
         * @param exclusiveMaximum the exclusive upper bound for the number
         */
        public Builder exclusiveMaximum(int exclusiveMaximum) {
            this.exclusiveMaximum = exclusiveMaximum;
            return this;
        }

        /**
         * Sets the value that the integer must be a multiple of.
         *
         * @param multipleOf the value to which the integer must be a multiple.
         */
        public Builder multipleOf(Integer multipleOf) {
            this.multipleOf = multipleOf;
            return this;
        }

        /**
         * Builds a {@link IntegerSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link IntegerSchema}
         */
        @Override
        public IntegerSchema build() {
            return new IntegerSchema(this);
        }
    }
}
