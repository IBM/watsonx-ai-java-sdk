/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a JSON Schema used to describe the structure of a response or tool.
 * <p>
 * The {@code JsonSchema} class provides static factory methods to create schema definitions for different data types such as objects, arrays,
 * strings, numbers, integers, booleans, and enums.
 * <p>
 * <b>Creation methods:</b>
 * <ul>
 * <li>{@link #object()} - creates an {@link ObjectSchema}</li>
 * <li>{@link #array()} - creates an {@link ArraySchema}</li>
 * <li>{@link #string()} - creates a {@link StringSchema}</li>
 * <li>{@link #number()} - creates a {@link NumberSchema}</li>
 * <li>{@link #integer()} - creates an {@link IntegerSchema}</li>
 * <li>{@link #bool()} - creates a {@link BooleanSchema}</li>
 * <li>{@link #enumeration(Object...)} - creates an {@link EnumSchema}</li>
 * <li>{@link #constant(Object)} - creates a {@link ConstantSchema}</li>
 * <li>{@link #required(String...)} - creates a {@link RequiredSchema}</li>
 *
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
 * @see ConstantSchema
 * @see RequiredSchema
 */
public abstract class JsonSchema {
    protected final String title;
    protected final String description;
    protected final Object defaultValue;
    protected final List<Object> examples;
    protected final Boolean readOnly;
    protected final Boolean writeOnly;
    protected final Boolean deprecated;
    protected final boolean nullable;
    protected final Object type;
    protected final List<? extends JsonSchema> oneOf;
    protected final List<? extends JsonSchema> anyOf;
    protected final List<? extends JsonSchema> allOf;
    protected final JsonSchema not;
    protected final JsonSchema ifSchema;
    protected final JsonSchema thenSchema;
    protected final JsonSchema elseSchema;

    protected JsonSchema(Object type, Builder<?, ?, ?> builder) {
        title = builder.title;
        description = builder.description;
        defaultValue = builder.defaultValue;
        examples = nonNull(builder.examples) ? Collections.unmodifiableList(new ArrayList<>(builder.examples)) : null;
        readOnly = builder.readOnly;
        writeOnly = builder.writeOnly;
        deprecated = builder.deprecated;
        nullable = builder.nullable;
        oneOf = nonNull(builder.oneOf)
            ? builder.oneOf.stream().map(JsonSchema.Builder::build).toList()
            : null;
        anyOf = nonNull(builder.anyOf)
            ? builder.anyOf.stream().map(JsonSchema.Builder::build).toList()
            : null;
        allOf = nonNull(builder.allOf)
            ? builder.allOf.stream().map(JsonSchema.Builder::build).toList()
            : null;
        not = nonNull(builder.not) ? builder.not.build() : null;
        ifSchema = nonNull(builder.ifSchema) ? builder.ifSchema.build() : null;
        thenSchema = nonNull(builder.thenSchema) ? builder.thenSchema.build() : null;
        elseSchema = nonNull(builder.elseSchema) ? builder.elseSchema.build() : null;
        // oneOf type, clean the type.
        this.type = builder.excludeType ? null : type;
    }

    /**
     * Returns the short title of this schema.
     *
     * @return the title
     */
    public String title() {
        return title;
    }

    /**
     * Returns the human-readable description of this schema.
     *
     * @return the description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the default value for this schema.
     *
     * @return the default value
     */
    public Object defaultValue() {
        return defaultValue;
    }

    /**
     * Returns example values for this schema.
     *
     * @return the list of example values
     */
    public List<Object> examples() {
        return examples;
    }

    /**
     * Returns whether this schema is read-only.
     *
     * @return {@code true} if the schema is read-only
     */
    public Boolean readOnly() {
        return readOnly;
    }

    /**
     * Returns whether this schema is write-only.
     *
     * @return {@code true} if the schema is write-only
     */
    public Boolean writeOnly() {
        return writeOnly;
    }

    /**
     * Returns whether this schema is deprecated.
     *
     * @return {@code true} if the schema is deprecated
     */
    public Boolean deprecated() {
        return deprecated;
    }

    /**
     * Returns the JSON Schema type of this schema.
     * <p>
     * The type can be a single string (e.g., {@code "string"}, {@code "object"}) or a list of types when the schema is nullable (e.g.,
     * {@code ["string", "null"]}).
     *
     * @return the type definition
     */
    public Object type() {
        return type;
    }

    /**
     * Returns the list of alternative schemas defined by {@code oneOf}.
     * <p>
     * The {@code oneOf} keyword validates the data against exactly one of the provided schemas. This is useful for defining mutually exclusive
     * alternatives.
     *
     * @return the list of alternative schemas
     */
    public List<? extends JsonSchema> oneOf() {
        return oneOf;
    }

    /**
     * Returns the list of schemas defined by {@code anyOf}.
     * <p>
     * The {@code anyOf} keyword validates the data against at least one of the provided schemas.
     *
     * @return the list of schemas
     */
    public List<? extends JsonSchema> anyOf() {
        return anyOf;
    }

    /**
     * Returns the list of schemas defined by {@code allOf}.
     * <p>
     * The {@code allOf} keyword validates the data against all of the provided schemas.
     *
     * @return the list of schemas
     */
    public List<? extends JsonSchema> allOf() {
        return allOf;
    }

    /**
     * Returns the schema defined by {@code not}.
     * <p>
     * The {@code not} keyword validates the data against anything that does not match the given schema.
     *
     * @return the negated schema
     */
    public JsonSchema not() {
        return not;
    }

    /**
     * Returns the conditional schema defined by {@code if}.
     *
     * @return the condition schema
     */
    public JsonSchema ifSchema() {
        return ifSchema;
    }

    /**
     * Returns the schema applied when {@code if} validates successfully, defined by {@code then}.
     *
     * @return the then schema
     */
    public JsonSchema thenSchema() {
        return thenSchema;
    }

    /**
     * Returns the schema applied when {@code if} fails to validate, defined by {@code else}.
     *
     * @return the else schema
     */
    public JsonSchema elseSchema() {
        return elseSchema;
    }

    /**
     * Creates a required constraint for use with {@code oneOf} or {@code anyOf} in object schemas.
     * <p>
     * This is typically used to specify alternative sets of required fields in an object schema.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.object()
     *     .property("age", JsonSchema.integer())
     *     .property("dateOfBirth", JsonSchema.string())
     *     .oneOf(
     *         JsonSchema.required("age"),
     *         JsonSchema.required("dateOfBirth")
     *     );
     * }</pre>
     *
     * @param required the names of the required fields
     * @return a {@link JsonSchema} representing required fields
     */
    public static RequiredSchema.Builder required(String... required) {
        return required(List.of(required));
    }

    /**
     * Creates a required constraint for use with {@code oneOf} or {@code anyOf} in object schemas.
     * <p>
     * This is typically used to specify alternative sets of required fields in an object schema.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * JsonSchema.object()
     *     .property("age", JsonSchema.integer())
     *     .property("dateOfBirth", JsonSchema.string())
     *     .oneOf(
     *         JsonSchema.required("age"),
     *         JsonSchema.required("dateOfBirth")
     *     );
     * }</pre>
     *
     * @param required the names of the required fields
     * @return a {@link JsonSchema} representing required fields
     */
    public static RequiredSchema.Builder required(List<String> required) {
        return new RequiredSchema.Builder(required);
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
     *
     * @param <B> the concrete builder type returned for fluent chaining
     * @param <JS> the {@link JsonSchema} type produced by this builder
     * @param <O> the builder type of the alternative schemas used with {@code oneOf}
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<B, JS extends JsonSchema, O extends JsonSchema.Builder<?, ?, ?>> {
        protected String title;
        protected String description;
        protected Object defaultValue;
        protected List<Object> examples;
        protected Boolean readOnly;
        protected Boolean writeOnly;
        protected Boolean deprecated;
        protected boolean nullable;
        protected List<O> oneOf;
        protected List<JsonSchema.Builder<?, ?, ?>> anyOf;
        protected List<JsonSchema.Builder<?, ?, ?>> allOf;
        protected JsonSchema.Builder<?, ?, ?> not;
        protected JsonSchema.Builder<?, ?, ?> ifSchema;
        protected JsonSchema.Builder<?, ?, ?> thenSchema;
        protected JsonSchema.Builder<?, ?, ?> elseSchema;
        protected boolean excludeType = false;

        protected Builder() {
            nullable = false;
        }

        /**
         * Sets a short title for the schema.
         *
         * @param title a short label for the schema
         */
        public B title(String title) {
            this.title = title;
            return (B) this;
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
         * Sets the default value for the schema.
         *
         * @param defaultValue the default value
         */
        public B defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return (B) this;
        }

        /**
         * Sets example values for the schema.
         * <p>
         * Examples are not used for validation but help document and illustrate the purpose of the schema.
         *
         * @param examples the list of example values
         */
        @SafeVarargs
        public final B examples(Object... examples) {
            this.examples = Arrays.asList(examples);
            return (B) this;
        }

        /**
         * Marks the schema as read-only.
         */
        public B readOnly() {
            this.readOnly = true;
            return (B) this;
        }

        /**
         * Marks the schema as write-only.
         */
        public B writeOnly() {
            this.writeOnly = true;
            return (B) this;
        }

        /**
         * Marks the schema as deprecated.
         * <p>
         * Indicates that applications should refrain from using the declared property.
         */
        public B deprecated() {
            this.deprecated = true;
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
         * Defines schemas using the {@code oneOf} keyword.
         *
         * @param oneOf the alternative schema builders
         */
        public B oneOf(O... oneOf) {
            return oneOf(List.of(oneOf));
        }

        /**
         * Defines schemas using the {@code oneOf} keyword.
         *
         * @param oneOf the alternative schema builders
         */
        public B oneOf(List<O> oneOf) {
            this.oneOf = oneOf.stream().map(e -> {
                // Clean metadata from oneOf alternatives to match JSON Schema spec
                // Note: This mutates the builders, but they're typically used inline and not reused, so this is safe in practice
                e.description = null;
                e.nullable = false;
                e.oneOf = null;
                e.excludeType = true;
                return e;
            }).toList();
            return (B) this;
        }

        /**
         * Defines schemas using the {@code anyOf} keyword.
         * <p>
         * The data must be valid against at least one of the provided schemas.
         *
         * @param anyOf the schema builders of which at least one must be satisfied
         */
        @SafeVarargs
        public final B anyOf(JsonSchema.Builder<?, ?, ?>... anyOf) {
            return anyOf(List.of(anyOf));
        }

        /**
         * Defines schemas using the {@code anyOf} keyword.
         * <p>
         * The data must be valid against at least one of the provided schemas.
         *
         * @param anyOf the schema builders of which at least one must be satisfied
         */
        public B anyOf(List<? extends JsonSchema.Builder<?, ?, ?>> anyOf) {
            @SuppressWarnings("rawtypes")
            List raw = anyOf;
            this.anyOf = raw;
            return (B) this;
        }

        /**
         * Defines schemas using the {@code allOf} keyword.
         * <p>
         * The data must be valid against all of the provided schemas.
         *
         * @param allOf the schema builders that all must be satisfied
         */
        @SafeVarargs
        public final B allOf(JsonSchema.Builder<?, ?, ?>... allOf) {
            return allOf(List.of(allOf));
        }

        /**
         * Defines schemas using the {@code allOf} keyword.
         * <p>
         * The data must be valid against all of the provided schemas.
         *
         * @param allOf the schema builders that all must be satisfied
         */
        public B allOf(List<? extends JsonSchema.Builder<?, ?, ?>> allOf) {
            @SuppressWarnings("rawtypes")
            List raw = allOf;
            this.allOf = raw;
            return (B) this;
        }

        /**
         * Defines a schema using the {@code not} keyword.
         * <p>
         * The data must not be valid against the provided schema.
         *
         * @param not the schema that the data must not match
         */
        public B not(JsonSchema.Builder<?, ?, ?> not) {
            this.not = not;
            return (B) this;
        }

        /**
         * Defines a conditional schema using the {@code if} keyword.
         * <p>
         * If the instance validates against this schema, the {@link #then(JsonSchema.Builder)} schema is applied; otherwise the
         * {@link #otherwise(JsonSchema.Builder)} schema is applied.
         *
         * @param ifSchema the condition schema
         */
        public B ifCondition(JsonSchema.Builder<?, ?, ?> ifSchema) {
            this.ifSchema = ifSchema;
            return (B) this;
        }

        /**
         * Defines the schema to apply when the {@code if} condition validates successfully.
         *
         * @param thenSchema the schema applied on success
         */
        public B then(JsonSchema.Builder<?, ?, ?> thenSchema) {
            this.thenSchema = thenSchema;
            return (B) this;
        }

        /**
         * Defines the schema to apply when the {@code if} condition fails to validate.
         *
         * @param elseSchema the schema applied on failure
         */
        public B otherwise(JsonSchema.Builder<?, ?, ?> elseSchema) {
            this.elseSchema = elseSchema;
            return (B) this;
        }

        /**
         * Builds and returns the concrete {@link JsonSchema} instance configured by this builder.
         *
         * @return the constructed schema instance
         */
        public abstract JS build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result + ((examples == null) ? 0 : examples.hashCode());
        result = prime * result + ((readOnly == null) ? 0 : readOnly.hashCode());
        result = prime * result + ((writeOnly == null) ? 0 : writeOnly.hashCode());
        result = prime * result + ((deprecated == null) ? 0 : deprecated.hashCode());
        result = prime * result + (nullable ? 1231 : 1237);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((oneOf == null) ? 0 : oneOf.hashCode());
        result = prime * result + ((anyOf == null) ? 0 : anyOf.hashCode());
        result = prime * result + ((allOf == null) ? 0 : allOf.hashCode());
        result = prime * result + ((not == null) ? 0 : not.hashCode());
        result = prime * result + ((ifSchema == null) ? 0 : ifSchema.hashCode());
        result = prime * result + ((thenSchema == null) ? 0 : thenSchema.hashCode());
        result = prime * result + ((elseSchema == null) ? 0 : elseSchema.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonSchema other = (JsonSchema) obj;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (defaultValue == null) {
            if (other.defaultValue != null)
                return false;
        } else if (!defaultValue.equals(other.defaultValue))
            return false;
        if (examples == null) {
            if (other.examples != null)
                return false;
        } else if (!examples.equals(other.examples))
            return false;
        if (readOnly == null) {
            if (other.readOnly != null)
                return false;
        } else if (!readOnly.equals(other.readOnly))
            return false;
        if (writeOnly == null) {
            if (other.writeOnly != null)
                return false;
        } else if (!writeOnly.equals(other.writeOnly))
            return false;
        if (deprecated == null) {
            if (other.deprecated != null)
                return false;
        } else if (!deprecated.equals(other.deprecated))
            return false;
        if (nullable != other.nullable)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (oneOf == null) {
            if (other.oneOf != null)
                return false;
        } else if (!oneOf.equals(other.oneOf))
            return false;
        if (anyOf == null) {
            if (other.anyOf != null)
                return false;
        } else if (!anyOf.equals(other.anyOf))
            return false;
        if (allOf == null) {
            if (other.allOf != null)
                return false;
        } else if (!allOf.equals(other.allOf))
            return false;
        if (not == null) {
            if (other.not != null)
                return false;
        } else if (!not.equals(other.not))
            return false;
        if (ifSchema == null) {
            if (other.ifSchema != null)
                return false;
        } else if (!ifSchema.equals(other.ifSchema))
            return false;
        if (thenSchema == null) {
            if (other.thenSchema != null)
                return false;
        } else if (!thenSchema.equals(other.thenSchema))
            return false;
        if (elseSchema == null) {
            if (other.elseSchema != null)
                return false;
        } else if (!elseSchema.equals(other.elseSchema))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "JsonSchema [title=" + title + ", description=" + description + ", defaultValue=" + defaultValue + ", examples=" + examples
            + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", deprecated=" + deprecated + ", type=" + type + ", nullable=" + nullable
            + ", oneOf=" + oneOf + ", anyOf=" + anyOf + ", allOf=" + allOf + ", not=" + not + ", if=" + ifSchema + ", then=" + thenSchema
            + ", else=" + elseSchema + "]";
    }
}
