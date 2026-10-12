/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final JsonSchema propertyNames;
    private final Map<String, List<String>> dependentRequired;
    private final Map<String, JsonSchema> dependentSchemas;
    private final Object unevaluatedProperties;

    private ObjectSchema(Builder builder) {
        super(builder.nullable ? List.of("object", "null") : "object", builder);
        properties = builder.properties.isEmpty() ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
        required = isNull(builder.required) ? null : List.copyOf(builder.required);
        minProperties = builder.minProperties;
        maxProperties = builder.maxProperties;
        patternProperties = isNull(builder.patternProperties) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.patternProperties));
        additionalProperties = builder.additionalProperties;
        propertyNames = isNull(builder.propertyNames) ? null : builder.propertyNames.build();
        unevaluatedProperties = builder.unevaluatedProperties;

        dependentRequired = isNull(builder.dependentRequired)
            ? null
            : Collections
                .unmodifiableMap(builder.dependentRequired.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()), (a, b) -> a, LinkedHashMap::new)));

        dependentSchemas = isNull(builder.dependentSchemas)
            ? null
            : Collections
                .unmodifiableMap(builder.dependentSchemas.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build(), (a, b) -> a, LinkedHashMap::new)));
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
     * Returns the schema used to validate property names of this object.
     *
     * @return the property names schema
     */
    public JsonSchema propertyNames() {
        return propertyNames;
    }

    /**
     * Returns a map of property dependencies, where each property name maps to a list of other property names that are required when the key property
     * is present.
     *
     * @return the dependent required map
     */
    public Map<String, List<String>> dependentRequired() {
        return dependentRequired;
    }

    /**
     * Returns a map of schema dependencies, where each property name maps to a schema that must validate when the key property is present.
     *
     * @return the dependent schemas map
     */
    public Map<String, JsonSchema> dependentSchemas() {
        return dependentSchemas;
    }

    /**
     * Returns the schema or boolean controlling handling of properties not evaluated by other keywords.
     *
     * @return the unevaluated properties value
     */
    public Object unevaluatedProperties() {
        return unevaluatedProperties;
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
    public static final class Builder extends JsonSchema.Builder<Builder, ObjectSchema, RequiredSchema.Builder> {
        private Map<String, JsonSchema> properties;
        private Map<String, JsonSchema> patternProperties;
        private Integer minProperties;
        private Integer maxProperties;
        private Object additionalProperties;
        private List<String> required;
        private JsonSchema.Builder<?, ?, ?> propertyNames;
        private Map<String, List<String>> dependentRequired;
        private Map<String, JsonSchema.Builder<?, ?, ?>> dependentSchemas;
        private Object unevaluatedProperties;

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
        public Builder property(String name, JsonSchema.Builder<?, ?, ?> schema) {
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
        public Builder patternProperty(String name, JsonSchema.Builder<?, ?, ?> schema) {
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
        public Builder additionalProperties(JsonSchema.Builder<?, ?, ?> schema) {
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
         * Defines alternative required field constraints using the {@code oneOf} keyword.
         * <p>
         * The {@code oneOf} keyword validates that <b>exactly one</b> of the provided required constraints is satisfied. This is useful for defining
         * mutually exclusive required field combinations.
         * <p>
         * <b>Example usage:</b>
         *
         * <pre>{@code
         * // Either age OR dateOfBirth must be present, but not both
         * JsonSchema.object()
         *     .property("age", JsonSchema.integer())
         *     .property("dateOfBirth", JsonSchema.string())
         *     .oneOf(
         *         JsonSchema.required("age"),
         *         JsonSchema.required("dateOfBirth")
         *     );
         * }</pre>
         *
         * @param oneOf the alternative required constraints
         */
        @Override
        public Builder oneOf(RequiredSchema.Builder... oneOf) {
            return super.oneOf(oneOf);
        }

        /**
         * Defines alternative required field constraints using the {@code oneOf} keyword.
         * <p>
         * The {@code oneOf} keyword validates that <b>exactly one</b> of the provided required constraints is satisfied. This is useful for defining
         * mutually exclusive required field combinations.
         * <p>
         * <b>Example usage:</b>
         *
         * <pre>{@code
         * // Either age OR dateOfBirth must be present, but not both
         * JsonSchema.object()
         *     .property("age", JsonSchema.integer())
         *     .property("dateOfBirth", JsonSchema.string())
         *     .oneOf(
         *         JsonSchema.required("age"),
         *         JsonSchema.required("dateOfBirth")
         *     );
         * }</pre>
         *
         * @param oneOf the alternative required constraints
         */
        @Override
        public Builder oneOf(List<RequiredSchema.Builder> oneOf) {
            return super.oneOf(oneOf);
        }

        /**
         * Defines alternative required field constraints using the {@code anyOf} keyword.
         * <p>
         * The {@code anyOf} keyword validates that <b>at least one</b> of the provided required constraints is satisfied. This is useful for defining
         * scenarios where different combinations of fields can be required.
         * <p>
         * <b>Example usage:</b>
         *
         * <pre>{@code
         * // At least one of salary OR hourlyRate must be present
         * JsonSchema.object()
         *     .property("salary", JsonSchema.number())
         *     .property("hourlyRate", JsonSchema.number())
         *     .anyOf(
         *         JsonSchema.required("salary"),
         *         JsonSchema.required("hourlyRate")
         *     );
         * }</pre>
         *
         * @param anyOf the alternative required constraints
         */
        public Builder anyOf(RequiredSchema.Builder... anyOf) {
            return anyOf(List.of(anyOf));
        }

        /**
         * Sets a schema to validate the names of all properties in the object.
         *
         * @param propertyNames the schema that each property name must satisfy
         */
        public Builder propertyNames(JsonSchema.Builder<?, ?, ?> propertyNames) {
            this.propertyNames = propertyNames;
            return this;
        }

        /**
         * Declares that if a property with the given key is present, all listed dependent properties must also be present.
         *
         * @param propertyName the property that triggers the dependency
         * @param dependents the property names that are required when {@code propertyName} is present
         */
        public Builder dependentRequired(String propertyName, String... dependents) {
            if (dependentRequired == null)
                dependentRequired = new LinkedHashMap<>();
            dependentRequired.put(propertyName, List.of(dependents));
            return this;
        }

        /**
         * Declares that if a property with the given key is present, the instance must also validate against the given schema.
         *
         * @param propertyName the property that triggers the dependency
         * @param schema the schema to validate against when {@code propertyName} is present
         */
        public Builder dependentSchema(String propertyName, JsonSchema.Builder<?, ?, ?> schema) {
            if (dependentSchemas == null)
                dependentSchemas = new LinkedHashMap<>();
            dependentSchemas.put(propertyName, schema);
            return this;
        }

        /**
         * Enables or disables validation of properties not covered by other object applicators.
         *
         * @param enable {@code false} to disallow unevaluated properties, {@code true} to allow them
         */
        public Builder unevaluatedProperties(boolean enable) {
            unevaluatedProperties = enable;
            return this;
        }

        /**
         * Sets a schema for properties not covered by other object applicators.
         *
         * @param schema the schema to apply to unevaluated properties
         */
        public Builder unevaluatedProperties(JsonSchema.Builder<?, ?, ?> schema) {
            unevaluatedProperties = schema.build();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((patternProperties == null) ? 0 : patternProperties.hashCode());
        result = prime * result + ((required == null) ? 0 : required.hashCode());
        result = prime * result + ((minProperties == null) ? 0 : minProperties.hashCode());
        result = prime * result + ((maxProperties == null) ? 0 : maxProperties.hashCode());
        result = prime * result + ((additionalProperties == null) ? 0 : additionalProperties.hashCode());
        result = prime * result + ((propertyNames == null) ? 0 : propertyNames.hashCode());
        result = prime * result + ((dependentRequired == null) ? 0 : dependentRequired.hashCode());
        result = prime * result + ((dependentSchemas == null) ? 0 : dependentSchemas.hashCode());
        result = prime * result + ((unevaluatedProperties == null) ? 0 : unevaluatedProperties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        ObjectSchema other = (ObjectSchema) obj;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (patternProperties == null) {
            if (other.patternProperties != null)
                return false;
        } else if (!patternProperties.equals(other.patternProperties))
            return false;
        if (required == null) {
            if (other.required != null)
                return false;
        } else if (!required.equals(other.required))
            return false;
        if (minProperties == null) {
            if (other.minProperties != null)
                return false;
        } else if (!minProperties.equals(other.minProperties))
            return false;
        if (maxProperties == null) {
            if (other.maxProperties != null)
                return false;
        } else if (!maxProperties.equals(other.maxProperties))
            return false;
        if (additionalProperties == null) {
            if (other.additionalProperties != null)
                return false;
        } else if (!additionalProperties.equals(other.additionalProperties))
            return false;
        if (propertyNames == null) {
            if (other.propertyNames != null)
                return false;
        } else if (!propertyNames.equals(other.propertyNames))
            return false;
        if (dependentRequired == null) {
            if (other.dependentRequired != null)
                return false;
        } else if (!dependentRequired.equals(other.dependentRequired))
            return false;
        if (dependentSchemas == null) {
            if (other.dependentSchemas != null)
                return false;
        } else if (!dependentSchemas.equals(other.dependentSchemas))
            return false;
        if (unevaluatedProperties == null) {
            if (other.unevaluatedProperties != null)
                return false;
        } else if (!unevaluatedProperties.equals(other.unevaluatedProperties))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ObjectSchema [title=" + title + ", description=" + description + ", defaultValue=" + defaultValue + ", examples=" + examples
            + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", deprecated=" + deprecated + ", type=" + type + ", nullable=" + nullable
            + ", oneOf=" + oneOf + ", anyOf=" + anyOf + ", allOf=" + allOf + ", not=" + not + ", if=" + ifSchema + ", then=" + thenSchema
            + ", else=" + elseSchema + ", properties=" + properties + ", patternProperties=" + patternProperties + ", required=" + required
            + ", minProperties=" + minProperties + ", maxProperties=" + maxProperties + ", additionalProperties=" + additionalProperties
            + ", propertyNames=" + propertyNames + ", dependentRequired=" + dependentRequired + ", dependentSchemas=" + dependentSchemas
            + ", unevaluatedProperties=" + unevaluatedProperties + "]";
    }
}
