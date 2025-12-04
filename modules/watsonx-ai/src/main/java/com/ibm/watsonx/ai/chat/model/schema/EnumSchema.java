/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

/**
 * Represents a JSON Schema of type {@code enum}.
 * <p>
 * Use {@link JsonSchema#enumeration(Object...)} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * JsonSchema.enumeration("ACTIVE", "INACTIVE", "PENDING");
 * }</pre>
 *
 */
public final class EnumSchema extends JsonSchema {
    private final List<?> enumValues;

    private EnumSchema(Builder builder) {
        super(null, builder);
        enumValues = builder.values;
    }

    public List<?> enumValues() {
        return enumValues;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#enumeration(Object...)} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.enumeration("ACTIVE", "INACTIVE", "PENDING");
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EnumSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, EnumSchema> {
        private List<?> values;

        private Builder() {}

        <T> Builder values(List<T> values) {
            this.values = values;
            return this;
        }

        /**
         * Builds a {@link EnumSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link EnumSchema}
         */
        @Override
        public EnumSchema build() {
            return new EnumSchema(this);
        }
    }
}
