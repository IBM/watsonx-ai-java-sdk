/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import java.util.List;

/**
 * Represents a JSON Schema of type {@code boolean}.
 * <p>
 * Use {@link JsonSchema#bool()} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * JsonSchema.bool()
 *     .description("Indicates whether the user account is active")
 *     .nullable();
 * }</pre>
 *
 */
public final class BooleanSchema extends JsonSchema {

    private BooleanSchema(Builder builder) {
        super(builder.nullable ? List.of("boolean", "null") : "boolean", builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#bool()} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.bool()
     *     .description("Indicates whether the user account is active")
     *     .nullable();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link BooleanSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, BooleanSchema> {

        private Builder() {}

        /**
         * Builds a {@link BooleanSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link BooleanSchema}
         */
        @Override
        public BooleanSchema build() {
            return new BooleanSchema(this);
        }
    }
}
