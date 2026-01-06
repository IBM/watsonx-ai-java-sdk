/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

/**
 * Represents a JSON Schema constraint that specifies required properties.
 * <p>
 * Use {@link JsonSchema#required(String...)} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * // Either age OR dateOfBirth must be present
 * JsonSchema.object()
 *     .property("age", JsonSchema.integer())
 *     .property("dateOfBirth", JsonSchema.string())
 *     .oneOf(
 *         JsonSchema.required("age"),
 *         JsonSchema.required("dateOfBirth")
 *     );
 * }</pre>
 *
 */
public final class RequiredSchema extends JsonSchema {
    private final List<String> required;

    RequiredSchema(Builder builder) {
        super(null, builder);
        this.required = builder.required;
    }

    public List<String> required() {
        return required;
    }

    /**
     * Builder class for constructing {@link RequiredSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, RequiredSchema, RequiredSchema.Builder> {
        private final List<String> required;

        Builder(List<String> required) {
            this.required = required;
        }

        @Override
        public Builder description(String description) {
            throw new UnsupportedOperationException("RequiredSchema does not support description");
        }

        @Override
        public Builder nullable() {
            throw new UnsupportedOperationException("RequiredSchema does not support nullable");
        }

        @Override
        public Builder oneOf(Builder... oneOf) {
            throw new UnsupportedOperationException("RequiredSchema does not support oneOf");
        }

        @Override
        public Builder oneOf(List<Builder> oneOf) {
            throw new UnsupportedOperationException("RequiredSchema does not support oneOf");
        }

        /**
         * Builds a {@link RequiredSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link RequiredSchema}
         */
        @Override
        public RequiredSchema build() {
            return new RequiredSchema(this);
        }
    }
}
