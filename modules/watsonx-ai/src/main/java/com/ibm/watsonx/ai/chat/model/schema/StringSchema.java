/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.requireNonNull;
import java.util.List;

/**
 * Represents a JSON Schema of type {@code string}.
 * <p>
 * Use {@link JsonSchema#string()} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * JsonSchema.string()
 *     .description("The full name of the user")
 *     .nullable();
 * }</pre>
 *
 */
public final class StringSchema extends JsonSchema {
    private final String pattern;
    private final Integer maxLength;
    private final Integer minLength;
    private final String format;

    private StringSchema(Builder builder) {
        super(builder.nullable ? List.of("string", "null") : "string", builder);
        pattern = builder.pattern;
        maxLength = builder.maxLength;
        minLength = builder.minLength;
        format = builder.format;
    }

    public String getPattern() {
        return pattern;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public String getFormat() {
        return format;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#string()} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.string()
     *     .description("The full name of the user")
     *     .nullable();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link StringSchema} instances with configurable parameters.
     */
    public final static class Builder extends JsonSchema.Builder<Builder, StringSchema, StringSchema.Builder> {
        private String pattern;
        private Integer minLength;
        private Integer maxLength;
        private String format;

        private Builder() {}

        /**
         * Sets a regular expression pattern that the string value must match.
         *
         * @param pattern the regular expression that the string must match
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Sets the minimum allowed length for the string value.
         *
         * @param minLength the minimum number of characters allowed
         */
        public Builder minLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        /**
         * Sets the maximum allowed length for the string value.
         *
         * @param maxLength the maximum number of characters allowed
         */
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Sets a predefined semantic format for the string value.
         *
         * @param format the predefined string format.
         */
        public Builder format(Format format) {
            requireNonNull(format, "The format cannot be null");
            this.format = format.getValue();
            return this;
        }

        /**
         * Builds a {@link StringSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link StringSchema}
         */
        public StringSchema build() {
            return new StringSchema(this);
        }
    }
}
