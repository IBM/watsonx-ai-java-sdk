/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.requireNonNullElse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON Schema of type {@code object}.
 * <p>
 * Use {@link JsonSchema#object()} to create an instance.
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
 */
public final class ObjectSchema extends JsonSchema {
    private final Map<String, JsonSchema> properties;
    private final Map<String, JsonSchema> patternProperties;
    private final List<String> required;
    private final Integer minProperties;
    private final Integer maxProperties;
    private final Object additionalProperties;

    private ObjectSchema(Builder builder) {
        super(builder.nullable ? List.of("object", "null") : "object", builder);
        properties = builder.properties.isEmpty() ? null : builder.properties;
        required = builder.required;
        minProperties = builder.minProperties;
        maxProperties = builder.maxProperties;
        patternProperties = builder.patternProperties;
        additionalProperties = builder.additionalProperties;
    }

    public Map<String, JsonSchema> properties() {
        return properties;
    }

    public List<String> required() {
        return required;
    }

    public Integer minProperties() {
        return minProperties;
    }

    public Integer maxProperties() {
        return maxProperties;
    }

    public Object additionalProperties() {
        return additionalProperties;
    }

    public Map<String, JsonSchema> patternProperties() {
        return patternProperties;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * Use {@link JsonSchema#object()} to create an instance.
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
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ObjectSchema} instances with configurable parameters.
     */
    public static final class Builder extends JsonSchema.Builder<Builder, ObjectSchema> {
        private Map<String, JsonSchema> properties;
        private Map<String, JsonSchema> patternProperties;
        private Integer minProperties;
        private Integer maxProperties;
        private Object additionalProperties;
        private List<String> required;

        private Builder() {
            properties = new LinkedHashMap<>();
        }

        /**
         * Adds a property definition to the object schema.
         * <p>
         * Each property is identified by a name and defined by a {@link JsonSchema}.
         *
         * @param name the property name
         * @param schema the schema describing the property's structure
         */
        public Builder property(String name, JsonSchema.Builder<?, ?> schema) {
            properties.put(name, schema.build());
            return this;
        }

        /**
         * Sets the minimum number of properties that the object must contain.
         *
         * @param minProperties the minimum number of properties
         */
        public Builder minProperties(int minProperties) {
            this.minProperties = minProperties;
            return this;
        }

        /**
         * Sets the maximum number of properties that the object can contain.
         *
         * @param maxProperties the maximum number of properties
         */
        public Builder maxProperties(int maxProperties) {
            this.maxProperties = maxProperties;
            return this;
        }

        /**
         * Adds a pattern-based property definition to the object schema.
         *
         * @param name the regex pattern for the property name
         * @param schema the schema describing the property's structure
         */
        public Builder patternProperty(String name, JsonSchema.Builder<?, ?> schema) {
            patternProperties = requireNonNullElse(patternProperties, new LinkedHashMap<>());
            patternProperties.put(name, schema.build());
            return this;
        }

        /**
         * Enables or disables additional properties for the object schema.
         *
         * @param enable whether to enable additional properties (true) or disable them (false).
         */
        public Builder additionalProperties(Boolean enable) {
            additionalProperties = enable;
            return this;
        }

        /**
         * Sets a specific schema for additional properties.
         *
         * @param schema the schema to apply to additional properties.
         */
        public Builder additionalProperties(JsonSchema.Builder<?, ?> schema) {
            return additionalProperties(schema.build());
        }

        /**
         * Sets a specific schema for additional properties.
         *
         * @param schema the schema to apply to additional properties.
         */
        public Builder additionalProperties(JsonSchema schema) {
            additionalProperties = schema;
            return this;
        }

        /**
         * Marks one or more properties as required.
         *
         * @param required the names of the required properties
         */
        public Builder required(String... required) {
            return required(List.of(required));
        }

        /**
         * Marks one or more properties as required.
         *
         * @param required the names of the required properties
         */
        public Builder required(List<String> required) {
            this.required = required;
            return this;
        }

        /**
         * Builds a {@link ObjectSchema} instance using the configured parameters.
         *
         * @return a new instance of {@link ObjectSchema}
         */
        @Override
        public ObjectSchema build() {
            return new ObjectSchema(this);
        }
    }
}
