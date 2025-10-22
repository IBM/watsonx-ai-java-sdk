/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;

/**
 * Represents a collection of semantic key-value pair fields used in schema-based text extraction or classification.
 *
 * @see TextExtractionService
 * @see TextClassificationService
 */
public final class KvpFields {
    private final Map<String, KvpField> fieldMap;

    /**
     * Represents a semantic key-value pair field definition within a schema.
     * <p>
     * Each field describes what value should be extracted or identified, provides an example of the expected format, and optionally specifies a list
     * of available options (for inferred or enumerated fields).
     *
     * @param description a description of the field to identify
     * @param example an example value illustrating the expected structure
     * @param availableOptions a list of predefined string values for this field
     */
    public record KvpField(String description, String example, List<String> availableOptions) {

        public KvpField {
            requireNonNull(description, "description can not be null");
            requireNonNull(example, "example can not be null");
        }

        /**
         * Creates a new {@link KvpField} instance with the given description and example.
         *
         * @param description a description of the field to identify
         * @param example an example value to help inform the model of structure and format
         * @return a new {@link KvpField} instance
         */
        public static KvpField of(String description, String example) {
            return of(description, example, null);
        }

        /**
         * Creates a new {@link KvpField} instance with the given description, example, and available options.
         *
         * @param description a description of the field to identify
         * @param example an example value illustrating expected format
         * @param availableOptions a list of possible values for this field
         * @return a new {@link KvpField} instance
         */
        public static KvpField of(String description, String example, List<String> availableOptions) {
            return new KvpField(description, example, availableOptions);
        }
    }

    private KvpFields(Builder builder) {
        fieldMap = builder.fields;
    }

    public Map<String, KvpField> getFields() {
        return fieldMap;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link KvpFields} instance.
     */
    public final static class Builder {
        private Map<String, KvpField> fields;

        private Builder() {}

        /**
         * Adds a new field definition.
         *
         * @param name the short-form name of the field (key)
         * @param field the {@link KvpField} definition
         */
        public Builder add(String name, KvpField field) {
            requireNonNull(name, "name can not be null");
            requireNonNull(field, "field can not be null");
            fields = requireNonNullElse(fields, new HashMap<>());
            fields.put(name, field);
            return this;
        }

        /**
         * Builds a {@link KvpFields} instance.
         *
         * @return a new instance of {@link SeKvpFieldsmanticConfig}
         */
        public KvpFields build() {
            return new KvpFields(this);
        }
    }
}
