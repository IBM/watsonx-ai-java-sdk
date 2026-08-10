/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

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

    public Object constant() {
        return constant;
    }

    /**
     * Builder class for constructing {@link ConstantSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, ConstantSchema, ConstantSchema.Builder> {
        private final Object value;

        Builder(Object value) {
            this.value = value;
        }

        @Override
        public Builder description(String description) {
            throw new UnsupportedOperationException("ConstantSchema does not support description");
        }

        @Override
        public Builder nullable() {
            throw new UnsupportedOperationException("ConstantSchema does not support nullable");
        }

        @Override
        public Builder oneOf(Builder... oneOf) {
            throw new UnsupportedOperationException("ConstantSchema does not support oneOf");
        }

        @Override
        public Builder oneOf(List<Builder> oneOf) {
            throw new UnsupportedOperationException("ConstantSchema does not support oneOf");
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        ConstantSchema other = (ConstantSchema) obj;
        if (constant == null) {
            if (other.constant != null)
                return false;
        } else if (!constant.equals(other.constant))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConstantSchema [description=" + description + ", type=" + type + ", nullable=" + nullable + ", oneOf=" + oneOf + ", constant="
            + constant + "]";
    }
}
