/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.JsonSchema.ArraySchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.BooleanSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.EnumSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.IntegerSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.NumberSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.ObjectSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.StringSchema;

/**
 * Represents a JSON Schema used to describe the structure of JSON/Tool.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * JsonSchema userSchema = JsonSchema.builder()
 *     .addStringProperty("name", "The user's full name")
 *     .addIntegerProperty("age", "The user's age in years")
 *     .addRequired("name")
 *     .build();
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
public sealed interface JsonSchema
    permits ObjectSchema, StringSchema, NumberSchema, IntegerSchema, BooleanSchema, ArraySchema, EnumSchema {

    /**
     * Create a new instance of {@link ObjectSchema.Builder}. Represents a JSON Schema used to describe the structure of JSON/Tool.
     * <p>
     * Example usage:
     *
     * <pre>{@code
     * JsonSchema.builder()
     *     .addStringProperty("name", "The user's full name")
     *     .addIntegerProperty("age", "The user's age in years")
     *     .addRequired("name")
     *     .build();
     * }</pre>
     *
     */
    public static ObjectSchema.Builder builder() {
        return ObjectSchema.of();
    }

    /**
     * Represents a JSON object schema.
     *
     * @param type The type of the schema (always {@code object}).
     * @param properties The map of property names to their JSON schemas.
     * @param required The list of required property names.
     */
    public record ObjectSchema(Object type, Map<String, JsonSchema> properties, List<String> required) implements JsonSchema {
        public Object type() {
            return "object";
        }

        public static ObjectSchema.Builder of() {
            return new ObjectSchema.Builder();
        }

        public static class Builder {
            private Map<String, JsonSchema> properties = new LinkedHashMap<>();
            private List<String> required;

            public Builder addStringProperty(String name) {
                return addStringProperty(name, null);
            }

            public Builder addNullableStringProperty(String name) {
                return addNullableStringProperty(name, null);
            }

            public Builder addStringProperty(String name, String description) {
                return addStringProperty(name, description, false);
            }

            public Builder addNullableStringProperty(String name, String description) {
                return addStringProperty(name, description, true);
            }

            public Builder addStringProperty(String name, String description, boolean nullable) {

                if (nonNull(description)) {
                    var schema = nullable ? StringSchema.ofNullable(description) : StringSchema.of(description);
                    return addProperty(name, schema);
                }

                return addProperty(name, nullable ? StringSchema.ofNullable() : StringSchema.of());
            }

            public Builder addIntegerProperty(String name) {
                return addIntegerProperty(name, null);
            }

            public Builder addNullableIntegerProperty(String name) {
                return addNullableIntegerProperty(name, null);
            }

            public Builder addIntegerProperty(String name, String description) {
                return addIntegerProperty(name, description, false);
            }

            public Builder addNullableIntegerProperty(String name, String description) {
                return addIntegerProperty(name, description, true);
            }

            public Builder addIntegerProperty(String name, String description, boolean nullable) {

                if (nonNull(description)) {
                    var schema = nullable ? IntegerSchema.ofNullable(description) : IntegerSchema.of(description);
                    return addProperty(name, schema);
                }

                return addProperty(name, nullable ? IntegerSchema.ofNullable() : IntegerSchema.of());
            }


            public Builder addNumberProperty(String name) {
                return addNumberProperty(name, null);
            }

            public Builder addNullableNumberProperty(String name) {
                return addNullableNumberProperty(name, null);
            }

            public Builder addNumberProperty(String name, String description) {
                return addNumberProperty(name, description, false);
            }

            public Builder addNullableNumberProperty(String name, String description) {
                return addNumberProperty(name, description, true);
            }

            public Builder addNumberProperty(String name, String description, boolean nullable) {

                if (nonNull(description)) {
                    var schema = nullable ? NumberSchema.ofNullable(description) : NumberSchema.of(description);
                    return addProperty(name, schema);
                }

                return addProperty(name, nullable ? NumberSchema.ofNullable() : NumberSchema.of());
            }

            public Builder addBooleanProperty(String name) {
                return addBooleanProperty(name, null);
            }

            public Builder addNullableBooleanProperty(String name) {
                return addNullableBooleanProperty(name, null);
            }

            public Builder addBooleanProperty(String name, String description) {
                return addBooleanProperty(name, description, false);
            }

            public Builder addNullableBooleanProperty(String name, String description) {
                return addBooleanProperty(name, description, true);
            }

            public Builder addBooleanProperty(String name, String description, boolean nullable) {

                if (nonNull(description)) {
                    var schema = nullable ? BooleanSchema.ofNullable(description) : BooleanSchema.of(description);
                    return addProperty(name, schema);
                }

                return addProperty(name, nullable ? BooleanSchema.ofNullable() : BooleanSchema.of());
            }

            public Builder addArrayProperty(String name, ObjectSchema.Builder schema) {
                requireNonNull(schema);
                return addProperty(name, ArraySchema.of(schema.build()));
            }

            public Builder addArrayProperty(String name, JsonSchema schema) {
                return addProperty(name, ArraySchema.of(schema));
            }

            public Builder addEnumProperty(String name, Object... values) {
                return addProperty(name, EnumSchema.of(values));
            }

            public Builder addObjectProperty(String name, JsonSchema schema) {
                return addProperty(name, schema);
            }

            public Builder addProperty(String name, JsonSchema schema) {
                requireNonNull(name);
                requireNonNull(schema);
                properties.put(name, schema);
                return this;
            }

            public Builder required(List<String> required) {
                this.required = required;
                return this;
            }

            public Builder required(String... required) {
                return required(Arrays.asList(required));
            }

            public ObjectSchema build() {
                return new ObjectSchema("object", properties, required);
            }
        }
    }

    /**
     * Represents a JSON string schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create a non-nullable string schema with a description
     * StringSchema.of("The name of the person");
     *
     * // Create a nullable string schema
     * StringSchema.ofNullable("Email address (optional)");
     * }</pre>
     *
     * @param type The type of the schema.
     * @param description The description of the field.
     */
    public record StringSchema(Object type, String description) implements JsonSchema {

        public static StringSchema ofNullable() {
            return ofNullable(null);
        }

        public static StringSchema ofNullable(String description) {
            return new StringSchema(List.of("string", "null"), description);
        }

        public static StringSchema of() {
            return of(null);
        }

        public static StringSchema of(String description) {
            return new StringSchema("string", description);
        }
    }

    /**
     * Represents a JSON number schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create a non-nullable number schema with a description
     * NumberSchema.of("The score");
     *
     * // Create a nullable number schema
     * NumberSchema.ofNullable("The score (optional)");
     * }</pre>
     *
     * @param type The type of the schema.
     * @param description The description of the field.
     */
    public record NumberSchema(Object type, String description) implements JsonSchema {

        public static NumberSchema ofNullable() {
            return ofNullable(null);
        }

        public static NumberSchema ofNullable(String description) {
            return new NumberSchema(List.of("number", "null"), description);
        }

        public static NumberSchema of() {
            return of(null);
        }

        public static NumberSchema of(String description) {
            return new NumberSchema("number", description);
        }
    }

    /**
     * Represents a JSON number schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create a non-nullable integer schema with a description
     * IntegerSchema.of("The age");
     *
     * // Create a nullable integer schema
     * IntegerSchema.ofNullable("The age (optional)");
     * }</pre>
     *
     * @param type The type of the schema.
     * @param description The description of the field.
     */
    public record IntegerSchema(Object type, String description) implements JsonSchema {

        public static IntegerSchema ofNullable() {
            return ofNullable(null);
        }

        public static IntegerSchema ofNullable(String description) {
            return new IntegerSchema(List.of("integer", "null"), description);
        }

        public static IntegerSchema of() {
            return of(null);
        }

        public static IntegerSchema of(String description) {
            return new IntegerSchema("integer", description);
        }
    }

    /**
     * Represents a JSON boolean schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create a non-nullable boolean schema with a description
     * BooleanSchema.of("Indicates whether the user is active");
     *
     * // Create a nullable boolean schema
     * BooleanSchema.ofNullable("Optional feature toggle");
     * }</pre>
     *
     * @param type The type of the schema.
     * @param description The description of the field.
     */
    public record BooleanSchema(Object type, String description) implements JsonSchema {

        public static BooleanSchema ofNullable() {
            return ofNullable(null);
        }

        public static BooleanSchema ofNullable(String description) {
            return new BooleanSchema(List.of("boolean", "null"), description);
        }

        public static BooleanSchema of() {
            return of(null);
        }

        public static BooleanSchema of(String description) {
            return new BooleanSchema("boolean", description);
        }
    }

    /**
     * Represents a JSON array schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create an array schema of non-nullable strings
     * ArraySchema.of(StringSchema.of("A tag label"));
     *
     * // Create an array schema of nullable booleans
     * ArraySchema.of(BooleanSchema.ofNullable("Optional flags"));
     * }</pre>
     *
     * @param type The type of the schema.
     * @param items The schema of the array items.
     */
    public record ArraySchema(String type, JsonSchema items) implements JsonSchema {
        public ArraySchema {
            type = "array";
            items = requireNonNull(items);
        }

        public static ArraySchema of(JsonSchema schema) {
            return new ArraySchema("array", schema);
        }
    }

    /**
     * Represents a JSON enum schema.
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * // Create an enum schema for status values
     * EnumSchema.of("PENDING", "APPROVED", "REJECTED");
     *
     * // Create an enum schema with mixed types (e.g., integers and null)
     * EnumSchema.of(1, 2, 3, null);
     * }</pre>
     *
     * @param enumValues The list of allowed values.
     */
    public record EnumSchema<T>(List<T> enumValues) implements JsonSchema {
        public EnumSchema {
            enumValues = requireNonNull(enumValues);
        }

        @SafeVarargs
        public static <T> EnumSchema<T> of(T... values) {
            return new EnumSchema<T>(Arrays.asList(values));
        }
    }
}
