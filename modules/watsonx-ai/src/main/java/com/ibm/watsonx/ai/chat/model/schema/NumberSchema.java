/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

/**
 * Represents a JSON Schema of type {@code number}.
 * <p>
 * Use {@link JsonSchema#number()} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * JsonSchema.number()
 *     .description("The price of the item")
 *     .nullable();
 * }</pre>
 *
 */
public final class NumberSchema extends JsonSchema {
    private final Integer minimum;
    private final Integer maximum;
    private final Integer exclusiveMinimum;
    private final Integer exclusiveMaximum;

    protected NumberSchema(Builder builder) {
        super(builder.nullable ? List.of("number", "null") : "number", builder);
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
        this.exclusiveMinimum = builder.exclusiveMinimum;
        this.exclusiveMaximum = builder.exclusiveMaximum;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public Integer getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public Integer getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#number()} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.number()
     *     .description("The price of the item")
     *     .nullable();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link NumberSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, NumberSchema> {
        private Integer minimum;
        private Integer maximum;
        private Integer exclusiveMinimum;
        private Integer exclusiveMaximum;

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
         * Builds a {@link NumberSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link NumberSchema}
         */
        @Override
        public NumberSchema build() {
            return new NumberSchema(this);
        }
    }
}
