/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.isNull;
import java.util.Arrays;

/**
 * Represents a JSON Schema used to describe the structure of a response or tool.
 * <p>
 * The {@code JsonSchema} class provides static factory methods to create schema definitions for different data types such as objects, arrays,
 * strings, numbers, integers, booleans, and enums.
 * <p>
 * <b>Creation methods:</b>
 * <ul>
 * <li>{@link #object()} — creates an {@link ObjectSchema}</li>
 * <li>{@link #array(JsonSchema.Builder)} — creates an {@link ArraySchema}</li>
 * <li>{@link #string()} — creates a {@link StringSchema}</li>
 * <li>{@link #number()} — creates a {@link NumberSchema}</li>
 * <li>{@link #integer()} — creates an {@link IntegerSchema}</li>
 * <li>{@link #bool()} — creates a {@link BooleanSchema}</li>
 * <li>{@link #enumeration(Object...)} — creates an {@link EnumSchema}</li>
 * </ul>
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var userSchema = JsonSchema.object()
 *     .property("id", JsonSchema.string())
 *     .property("email", JsonSchema.string().pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
 *     .required("id", "email");
 *
 * var usersArray = JsonSchema.array(userSchema)
 *     .description("A list of registered users");
 * }</pre>
 *
 * @see ObjectSchema
 * @see StringSchema
 * @see NumberSchema
 * @see IntegerSchema
 * @see BooleanSchema
 * @see ArraySchema
 * @see EnumSchema
 */
public abstract class JsonSchema {
    protected final String description;
    protected final boolean nullable;
    protected final Object type;
    protected final Object constant;

    protected JsonSchema(Object type, Builder<?, ?> builder) {
        description = builder.description;
        nullable = builder.nullable;
        if (isNull(builder.constant)) {
            this.type = type;
            constant = null;
        } else {
            constant = builder.constant;
            this.type = null;
        }
    }

    public String getDescription() {
        return description;
    }

    public Object getType() {
        return type;
    }

    public Object getConstant() {
        return constant;
    }

    /**
     * Represents a JSON Schema of type {@code object}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.object()
     *     .property("id", JsonSchema.string())
     *     .property("price", JsonSchema.number())
     *     .property("tags", JsonSchema.array(JsonSchema.string()))
     *     .required("id", "price");
     * }</pre>
     *
     * @return a builder for {@link ObjectSchema}
     */
    public static ObjectSchema.Builder object() {
        return ObjectSchema.builder();
    }

    /**
     * Represents a JSON Schema of type {@code string}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * // Define a simple string schema
     * JsonSchema.string()
     *     .description("The full name of the user")
     *     .nullable();
     *
     * // Define a string schema with a regex pattern
     * JsonSchema.string()
     *     .description("The user's email address")
     *     .pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
     *     .nullable();
     * }</pre>
     *
     * @return a builder for {@link StringSchema}
     */
    public static StringSchema.Builder string() {
        return StringSchema.builder();
    }

    /**
     * Represents a JSON Schema of type {@code number}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.number()
     *     .description("The price of the item")
     *     .nullable();
     * }</pre>
     *
     * @return a builder for {@link NumberSchema}
     */
    public static NumberSchema.Builder number() {
        return NumberSchema.builder();
    }

    /**
     * Represents a JSON Schema of type {@code integer}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.integer()
     *     .description("The age of the user")
     *     .nullable();
     * }</pre>
     *
     * @return a builder for {@link IntegerSchema}
     */
    public static IntegerSchema.Builder integer() {
        return IntegerSchema.builder();
    }

    /**
     * Represents a JSON Schema of type {@code boolean}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.bool()
     *     .description("Indicates whether the user account is active")
     *     .nullable();
     * }</pre>
     *
     * @return a builder for {@link BooleanSchema}
     */
    public static BooleanSchema.Builder bool() {
        return BooleanSchema.builder();
    }

    /**
     * Creates a JSON Schema of type {@code array}.
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
     * @param itemsSchema the schema that each element in the array must satisfy
     * @return a builder for {@link ArraySchema}
     */
    public static ArraySchema.Builder array() {
        return ArraySchema.builder();
    }

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
     * @param <T> the type of the enum values
     * @param values the allowed values for the enum
     * @return a builder for {@link EnumSchema}
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> EnumSchema.Builder enumeration(T... values) {
        return EnumSchema.builder().values(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B, O extends JsonSchema> {
        protected String description;
        protected boolean nullable;
        protected Object constant;

        protected Builder() {
            nullable = false;
        }

        /**
         * Sets a human-readable description for the schema.
         * <p>
         * The description is typically used to provide additional context about the meaning or intended use of a property or field.
         *
         * @param description a short text describing the schema
         */
        public B description(String description) {
            this.description = description;
            return (B) this;
        }

        /**
         * Marks the schema as nullable.
         * <p>
         * When set, the schema type will include {@code "null"} as an allowed value.
         */
        public B nullable() {
            this.nullable = true;
            return (B) this;
        }

        /**
         * Sets a constant value that the instance must exactly match.
         *
         * @param <T> the type of the constant value
         * @param constant the exact value that instances must match
         */
        public <T> B constant(T constant) {
            this.constant = constant;
            return (B) this;
        }

        public abstract O build();
    }
}
