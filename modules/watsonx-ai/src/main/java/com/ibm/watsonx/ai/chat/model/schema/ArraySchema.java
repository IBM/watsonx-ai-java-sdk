/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.isNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a JSON Schema of type {@code array}.
 * <p>
 * Use {@link JsonSchema#array()} to create an instance.
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
    private final List<JsonSchema> prefixItems;
    private final JsonSchema contains;
    private final Integer minItems;
    private final Integer maxItems;
    private final Integer minContains;
    private final Integer maxContains;
    private final Boolean uniqueItems;
    private final Object unevaluatedItems;

    private ArraySchema(Builder builder) {
        super(builder.nullable ? List.of("array", "null") : "array", builder);

        if (isNull(builder.items) && isNull(builder.prefixItems) && isNull(builder.contains) && isNull(oneOf) && isNull(anyOf) && isNull(allOf))
            throw new IllegalArgumentException("One of items, prefixItems, or contains must be specified");

        items = builder.items;
        prefixItems = isNull(builder.prefixItems) ? null : Collections.unmodifiableList(builder.prefixItems);
        contains = builder.contains;
        minItems = builder.minItems;
        maxItems = builder.maxItems;
        minContains = builder.minContains;
        maxContains = builder.maxContains;
        uniqueItems = builder.uniqueItems;
        unevaluatedItems = builder.unevaluatedItems;
    }

    /**
     * Returns the schema that all elements in the array must conform to.
     *
     * @return the items schema
     */
    public JsonSchema items() {
        return items;
    }

    /**
     * Returns the list of schemas used for tuple validation, where each schema validates the element at the corresponding index.
     *
     * @return the prefix items schemas
     */
    public List<JsonSchema> prefixItems() {
        return prefixItems;
    }

    /**
     * Returns the schema that at least one element in the array must match.
     *
     * @return the contains schema
     */
    public JsonSchema contains() {
        return contains;
    }

    /**
     * Returns the minimum number of items that the array must contain.
     *
     * @return the minimum number of items
     */
    public Integer minItems() {
        return minItems;
    }

    /**
     * Returns the maximum number of items that the array may contain.
     *
     * @return the maximum number of items
     */
    public Integer maxItems() {
        return maxItems;
    }

    /**
     * Returns the minimum number of times the {@code contains} schema must match.
     *
     * @return the minimum contains count
     */
    public Integer minContains() {
        return minContains;
    }

    /**
     * Returns the maximum number of times the {@code contains} schema may match.
     *
     * @return the maximum contains count
     */
    public Integer maxContains() {
        return maxContains;
    }

    /**
     * Returns whether the array elements must all be unique.
     *
     * @return {@code true} if uniqueness is required
     */
    public Boolean uniqueItems() {
        return uniqueItems;
    }

    /**
     * Returns the schema or boolean controlling validation of array elements not evaluated by other keywords.
     *
     * @return the unevaluated items value
     */
    public Object unevaluatedItems() {
        return unevaluatedItems;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#array()} to create an instance.
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

    /**
     * Builder class for constructing {@link ArraySchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, ArraySchema, ArraySchema.Builder> {
        private JsonSchema items;
        private List<JsonSchema> prefixItems;
        private JsonSchema contains;
        private Integer minItems;
        private Integer maxItems;
        private Integer minContains;
        private Integer maxContains;
        private Boolean uniqueItems;
        private Object unevaluatedItems;

        private Builder() {}

        /**
         * Sets the schema that all elements in the array must conform to.
         *
         * @param items the schema that each element of the array must satisfy
         */
        public Builder items(JsonSchema.Builder<?, ?, ?> items) {
            this.items = items.build();
            return this;
        }

        /**
         * Sets the schemas for tuple validation, where each schema corresponds to the element at the same index.
         * <p>
         * <b>Example usage:</b>
         *
         * <pre>{@code
         * // A tuple of [number, street name string, street type enum]
         * JsonSchema.array()
         *     .prefixItems(
         *         JsonSchema.number(),
         *         JsonSchema.string(),
         *         JsonSchema.enumeration("Street", "Avenue", "Boulevard")
         *     );
         * }</pre>
         *
         * @param prefixItems the schemas for each positional element
         */
        public Builder prefixItems(JsonSchema.Builder<?, ?, ?>... prefixItems) {
            this.prefixItems = Arrays.stream(prefixItems).map(b -> (JsonSchema) b.build()).toList();
            return this;
        }

        /**
         * Sets the schemas for tuple validation, where each schema corresponds to the element at the same index.
         *
         * @param prefixItems the schemas for each positional element
         */
        public Builder prefixItems(List<? extends JsonSchema.Builder<?, ?, ?>> prefixItems) {
            this.prefixItems = prefixItems.stream().map(b -> (JsonSchema) b.build()).toList();
            return this;
        }

        /**
         * Sets a schema that at least one element in the array must match.
         *
         * @param contains the schema that at least one element of the array must satisfy
         */
        public Builder contains(JsonSchema.Builder<?, ?, ?> contains) {
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

        /**
         * Sets the minimum number of times the {@code contains} schema must match within the array.
         *
         * @param minContains the minimum number of matching elements required
         */
        public Builder minContains(int minContains) {
            this.minContains = minContains;
            return this;
        }

        /**
         * Sets the maximum number of times the {@code contains} schema may match within the array.
         *
         * @param maxContains the maximum number of matching elements allowed
         */
        public Builder maxContains(int maxContains) {
            this.maxContains = maxContains;
            return this;
        }

        /**
         * Requires that all elements in the array are unique.
         */
        public Builder uniqueItems() {
            this.uniqueItems = true;
            return this;
        }

        /**
         * Enables or disables validation of array elements not covered by other keywords.
         *
         * @param allow {@code false} to disallow unevaluated items, {@code true} to allow them
         */
        public Builder unevaluatedItems(boolean allow) {
            unevaluatedItems = allow;
            return this;
        }

        /**
         * Sets a schema that array elements not evaluated by other keywords must satisfy.
         *
         * @param schema the schema for unevaluated items
         */
        public Builder unevaluatedItems(JsonSchema.Builder<?, ?, ?> schema) {
            unevaluatedItems = schema.build();
            return this;
        }

        /**
         * Builds a {@link ArraySchema} instance using the configured parameters.
         *
         * @return a new instance of {@link ArraySchema}
         */
        @Override
        public ArraySchema build() {
            return new ArraySchema(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + ((prefixItems == null) ? 0 : prefixItems.hashCode());
        result = prime * result + ((contains == null) ? 0 : contains.hashCode());
        result = prime * result + ((minItems == null) ? 0 : minItems.hashCode());
        result = prime * result + ((maxItems == null) ? 0 : maxItems.hashCode());
        result = prime * result + ((minContains == null) ? 0 : minContains.hashCode());
        result = prime * result + ((maxContains == null) ? 0 : maxContains.hashCode());
        result = prime * result + ((uniqueItems == null) ? 0 : uniqueItems.hashCode());
        result = prime * result + ((unevaluatedItems == null) ? 0 : unevaluatedItems.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        ArraySchema other = (ArraySchema) obj;
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        if (prefixItems == null) {
            if (other.prefixItems != null)
                return false;
        } else if (!prefixItems.equals(other.prefixItems))
            return false;
        if (contains == null) {
            if (other.contains != null)
                return false;
        } else if (!contains.equals(other.contains))
            return false;
        if (minItems == null) {
            if (other.minItems != null)
                return false;
        } else if (!minItems.equals(other.minItems))
            return false;
        if (maxItems == null) {
            if (other.maxItems != null)
                return false;
        } else if (!maxItems.equals(other.maxItems))
            return false;
        if (minContains == null) {
            if (other.minContains != null)
                return false;
        } else if (!minContains.equals(other.minContains))
            return false;
        if (maxContains == null) {
            if (other.maxContains != null)
                return false;
        } else if (!maxContains.equals(other.maxContains))
            return false;
        if (uniqueItems == null) {
            if (other.uniqueItems != null)
                return false;
        } else if (!uniqueItems.equals(other.uniqueItems))
            return false;
        if (unevaluatedItems == null) {
            if (other.unevaluatedItems != null)
                return false;
        } else if (!unevaluatedItems.equals(other.unevaluatedItems))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ArraySchema [title=" + title + ", description=" + description + ", defaultValue=" + defaultValue + ", examples=" + examples
            + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", deprecated=" + deprecated + ", type=" + type + ", nullable=" + nullable
            + ", oneOf=" + oneOf + ", anyOf=" + anyOf + ", allOf=" + allOf + ", not=" + not + ", if=" + ifSchema + ", then=" + thenSchema
            + ", else=" + elseSchema + ", items=" + items + ", prefixItems=" + prefixItems + ", contains=" + contains + ", minItems=" + minItems
            + ", maxItems=" + maxItems + ", minContains=" + minContains + ", maxContains=" + maxContains + ", uniqueItems=" + uniqueItems
            + ", unevaluatedItems=" + unevaluatedItems + "]";
    }
}
