/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

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
 * <li>{@link #array()} — creates an {@link ArraySchema}</li>
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

    protected JsonSchema(Object type, Builder<?, ?> builder) {
        description = builder.description;
        nullable = builder.nullable;
        this.type = type;
    }

    public String description() {
        return description;
    }

    public Object type() {
        return type;
    }

    /**
     * Represents a JSON Schema that allows only a single constant value.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.constant(42);
     * JsonSchema.constant("fixedString");
     * }</pre>
     *
     * @param <T> the type of the constant value
     * @param value the exact value that instances must match
     * @return a {@link JsonSchema} representing the constant value
     */
    public static <T> ConstantSchema.Builder constant(T value) {
        return new ConstantSchema.Builder(value);
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
     * Creates a JSON Schema of type {@code object} with a description.
     * <p>
     * The description is a human-readable text providing context about the object schema.
     *
     * <pre>{@code
     * JsonSchema.object("A user object")
     *     .property("id", JsonSchema.string())
     *     .property("email", JsonSchema.string())
     *     .required("id", "email");
     * }</pre>
     *
     * @param description a short text describing the schema
     * @return a builder for {@link ObjectSchema} with the given description
     */
    public static ObjectSchema.Builder object(String description) {
        return object().description(description);
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
     * Creates a JSON Schema of type {@code string} with a description.
     * <p>
     * The description is a human-readable text providing context about the string field.
     *
     * <pre>{@code
     * JsonSchema.string("The user's email")
     *     .pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
     *     .nullable();
     * }</pre>
     *
     * @param description a short text describing the string schema
     * @return a builder for {@link StringSchema} with the given description
     */
    public static StringSchema.Builder string(String description) {
        return string().description(description);
    }

    /**
     * Represents a JSON Schema of type {@code number}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.number()
     *     .description("The price of the item")
     *     .maximum(9999)
     *     .minimum(0);
     * }</pre>
     *
     * @return a builder for {@link NumberSchema}
     */
    public static NumberSchema.Builder number() {
        return NumberSchema.builder();
    }

    /**
     * Creates a JSON Schema of type {@code number} with a description.
     * <p>
     * The description is a human-readable text providing context about the number field.
     *
     * <pre>{@code
     * JsonSchema.number("The price of the item")
     *     .minimum(0)
     *     .maximum(9999);
     * }</pre>
     *
     * @param description a short text describing the number schema
     * @return a builder for {@link NumberSchema} with the given description
     */
    public static NumberSchema.Builder number(String description) {
        return number().description(description);
    }

    /**
     * Represents a JSON Schema of type {@code integer}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.integer()
     *     .description("The age of the user")
     *     .minimum(18);
     * }</pre>
     *
     * @return a builder for {@link IntegerSchema}
     */
    public static IntegerSchema.Builder integer() {
        return IntegerSchema.builder();
    }

    /**
     * Creates a JSON Schema of type {@code integer} with a description.
     * <p>
     * The description is a human-readable text providing context about the integer field.
     *
     * <pre>{@code
     * JsonSchema.integer("The user's age")
     *     .minimum(18);
     * }</pre>
     *
     * @param description a short text describing the integer schema
     * @return a builder for {@link IntegerSchema} with the given description
     */
    public static IntegerSchema.Builder integer(String description) {
        return integer().description(description);
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
     * Creates a JSON Schema of type {@code boolean} with a description.
     * <p>
     * The description is a human-readable text providing context about the boolean field.
     *
     * <pre>{@code
     * JsonSchema.bool("Indicates whether the user account is active")
     *     .nullable();
     * }</pre>
     *
     * @param description a short text describing the boolean schema
     * @return a builder for {@link BooleanSchema} with the given description
     */
    public static BooleanSchema.Builder bool(String description) {
        return bool().description(description);
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
     * @return a builder for {@link ArraySchema}
     */
    public static ArraySchema.Builder array() {
        return ArraySchema.builder();
    }

    /**
     * Creates a JSON Schema of type {@code array} with a description.
     * <p>
     * The description is a human-readable text providing context about the array field.
     *
     * <pre>{@code
     * JsonSchema.array("A list of user objects")
     *     .items(JsonSchema.object().property("name", JsonSchema.string()));
     * }</pre>
     *
     * @param description a short text describing the array schema
     * @return a builder for {@link ArraySchema} with the given description
     */
    public static ArraySchema.Builder array(String description) {
        return array().description(description);
    }

    /**
     * Represents a JSON Schema of type {@code enum}.
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

    /**
     * Builder class for constructing {@link JsonSchema} instances with configurable parameters.
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<B, O extends JsonSchema> {
        protected String description;
        protected boolean nullable;

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
         * Builds and returns the concrete {@link JsonSchema} instance configured by this builder.
         * <p>
         * Each subclass of {@code JsonSchema} provides its own implementation of this method, assembling a fully-initialized and immutable schema
         * object based on the parameters supplied to the builder.
         *
         * @return the constructed schema instance of type {@code O}
         */
        public abstract O build();
    }
}
