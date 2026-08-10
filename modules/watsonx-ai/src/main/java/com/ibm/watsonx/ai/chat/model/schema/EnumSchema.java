/*
 * Copyright 2025 IBM Corporation
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
    public static final class Builder extends JsonSchema.Builder<Builder, EnumSchema, EnumSchema.Builder> {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((enumValues == null) ? 0 : enumValues.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        EnumSchema other = (EnumSchema) obj;
        if (enumValues == null) {
            if (other.enumValues != null)
                return false;
        } else if (!enumValues.equals(other.enumValues))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EnumSchema [description=" + description + ", type=" + type + ", nullable=" + nullable + ", oneOf=" + oneOf + ", enumValues="
            + enumValues + "]";
    }
}
