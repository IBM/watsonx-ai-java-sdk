/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

/**
 * Represents a JSON Schema that enforces a constant value.
 * <p>
 * Use {@link JsonSchema#constant(Object)} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * // Schema allowing only the integer 42
 * JsonSchema.constant(42);
 *
 * // Schema allowing only the string "fixedValue"
 * JsonSchema.constant("fixedValue");
 * }</pre>
 *
 */
public final class ConstantSchema extends JsonSchema {
    private final Object constant;

    ConstantSchema(Builder builder) {
        super(null, builder);
        this.constant = builder.value;
    }

    public Object getConst() {
        return constant;
    }

    /**
     * Builder class for constructing {@link ConstantSchema} instances with configurable parameters.
     */
    static final class Builder extends JsonSchema.Builder<Builder, ConstantSchema> {
        private final Object value;

        Builder(Object value) {
            this.value = value;
        }

        /**
         * Builds a {@link ConstantSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link ConstantSchema}
         */
        @Override
        public ConstantSchema build() {
            return new ConstantSchema(this);
        }
    }
}
