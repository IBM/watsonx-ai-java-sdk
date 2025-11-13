/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents a JSON Schema of type {@code array}.
 * <p>
 * Use {@link JsonSchema#array(JsonSchema.Builder)} to create an instance.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * // Define a User schema
 * var userSchema = JsonSchema.object()
 *     .property("name", JsonSchema.string())
 *     .property("age", JsonSchema.integer())
 *     .required("name");
 *
 * // Define a schema for an array of Users
 * JsonSchema.array().items(userSchema).description("A list of user objects");
 * }</pre>
 *
 */
public final class ArraySchema extends JsonSchema {
    private final JsonSchema items;
    private final JsonSchema contains;
    private final Integer minItems;
    private final Integer maxItems;

    private ArraySchema(Builder builder) {
        super(builder.nullable ? List.of("array", "null") : "array", builder);
        if (isNull(builder.items) && isNull(builder.contains))
            throw new IllegalArgumentException("Either items or contains must be specified");
        items = builder.items;
        contains = builder.contains;
        minItems = builder.minItems;
        maxItems = builder.maxItems;
    }

    public JsonSchema getItems() {
        return items;
    }

    public JsonSchema getContains() {
        return contains;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#array(JsonSchema.Builder)} to create an instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * // Define a User schema
     * var userSchema = JsonSchema.object()
     *     .property("name", JsonSchema.string())
     *     .property("age", JsonSchema.integer())
     *     .required("name");
     *
     * // Define a schema for an array of Users
     * JsonSchema.array().items(userSchema).description("A list of user objects");
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends JsonSchema.Builder<Builder, ArraySchema> {
        private JsonSchema items;
        private JsonSchema contains;
        private Integer minItems;
        private Integer maxItems;

        private Builder() {}

        /**
         * Sets the schema that all elements in the array must conform to.
         *
         * @param items the schema that each element of the array must satisfy
         */
        public Builder items(JsonSchema.Builder<?, ?> items) {
            this.items = items.build();
            return this;
        }

        /**
         * Sets a schema that at least one element in the array must match.
         *
         * @param contains the schema that at least one element of the array must satisfy
         */
        public Builder contains(JsonSchema.Builder<?, ?> contains) {
            this.contains = contains.build();
            return this;
        }

        /**
         * Sets the minimum number of items that the array must contain.
         *
         * @param minItems the minimum number of elements required in the array
         */
        public Builder minItems(int minItems) {
            this.minItems = minItems;
            return this;
        }

        /**
         * Sets the maximum number of items that the array may contain.
         *
         * @param maxItems the maximum number of elements allowed in the array
         */
        public Builder maxItems(int maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public ArraySchema build() {
            return new ArraySchema(this);
        }
    }
}
