/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.nonNull;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;

/**
 * Represents a custom schema used for semantic key-value pair.
 *
 * <p>
 * A {@code Schema} defines the structure and semantic meaning of the information to be extracted from a document, allowing precise control over how
 * the extraction model identifies and returns key-value pairs. Custom schemas can override or extend the predefined extraction behavior.
 *
 * <p>
 * Each schema can be defined either through:
 * <ul>
 * <li>{@link #fields} — a mapping of field names to {@link KvpFields} definitions, for variable-layout documents, or</li>
 * <li>{@link #pages} — a page-based mapping of {@link KvpPage} instances, for fixed-layout documents.</li>
 * </ul>
 * These two options are mutually exclusive.
 *
 * @see KvpFields
 * @see KvpPage
 */
public final class Schema {
    private final String documentType;
    private final String documentDescription;
    private final Map<String, KvpField> fields;
    private final KvpPage pages;
    private final String additionalPromptInstructions;

    private Schema(Builder builder) {
        documentType = builder.documentType;
        documentDescription = builder.documentDescription;
        fields = nonNull(builder.fields) ? builder.fields.fields() : null;
        pages = builder.pages;
        additionalPromptInstructions = builder.additionalPromptInstructions;
    }

    /**
     * Gets the document type.
     *
     * @return the document type
     */
    public String documentType() {
        return documentType;
    }

    /**
     * Gets the document description.
     *
     * @return the document description
     */
    public String documentDescription() {
        return documentDescription;
    }

    /**
     * Gets the field-based schema definition.
     *
     * @return the map of field definitions, or null if page-based schema is used
     */
    public Map<String, KvpField> fields() {
        return fields;
    }

    /**
     * Gets the page-based schema definition.
     *
     * @return the page definition, or null if field-based schema is used
     */
    public KvpPage pages() {
        return pages;
    }

    /**
     * Gets the additional prompt instructions.
     *
     * @return the additional prompt instructions, or null if not set
     */
    public String additionalPromptInstructions() {
        return additionalPromptInstructions;
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
     * Builder class for constructing {@link Schema} instance.
     */
    public final static class Builder {
        private String documentType;
        private String documentDescription;
        private KvpFields fields;
        private KvpPage pages;
        private String additionalPromptInstructions;

        /**
         * Sets the document type for the schema.
         *
         * @param documentType a short title like "Passport" or "Invoice"
         */
        public Builder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        /**
         * Sets the document description for the schema.
         *
         * @param documentDescription a one or two sentence description to help the LLM understand the document
         */
        public Builder documentDescription(String documentDescription) {
            this.documentDescription = documentDescription;
            return this;
        }

        /**
         * Sets the field-based schema definition.
         *
         * <p>
         * Defines a mapping of field names to semantic field definitions. This option is mutually exclusive with {@link #pages(KvpPage)}.
         *
         * @param fields the {@link KvpFields} definition
         */
        public Builder fields(KvpFields fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Sets the page-based schema definition.
         *
         * <p>
         * Defines a mapping of page descriptions and their corresponding {@link KvpPage} definitions, used primarily for fixed-layout documents. This
         * option is mutually exclusive with {@link #fields(KvpFields)}.
         *
         * @param pages the {@link KvpPage} definition
         */
        public Builder pages(KvpPage pages) {
            this.pages = pages;
            return this;
        }

        /**
         * Sets optional additional prompt instructions for the model.
         *
         * <p>
         * If provided, these instructions are appended to the model prompt to refine or guide extraction behavior for this schema.
         *
         * @param additionalPromptInstructions custom prompt instructions to include
         */
        public Builder additionalPromptInstructions(String additionalPromptInstructions) {
            this.additionalPromptInstructions = additionalPromptInstructions;
            return this;
        }

        /**
         * Builds a {@link Schema} instance.
         *
         * @return a new instance of {@link Schema}
         */
        public Schema build() {
            return new Schema(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((documentType == null) ? 0 : documentType.hashCode());
        result = prime * result + ((documentDescription == null) ? 0 : documentDescription.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((pages == null) ? 0 : pages.hashCode());
        result = prime * result + ((additionalPromptInstructions == null) ? 0 : additionalPromptInstructions.hashCode());
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
        Schema other = (Schema) obj;
        if (documentType == null) {
            if (other.documentType != null)
                return false;
        } else if (!documentType.equals(other.documentType))
            return false;
        if (documentDescription == null) {
            if (other.documentDescription != null)
                return false;
        } else if (!documentDescription.equals(other.documentDescription))
            return false;
        if (fields == null) {
            if (other.fields != null)
                return false;
        } else if (!fields.equals(other.fields))
            return false;
        if (pages == null) {
            if (other.pages != null)
                return false;
        } else if (!pages.equals(other.pages))
            return false;
        if (additionalPromptInstructions == null) {
            if (other.additionalPromptInstructions != null)
                return false;
        } else if (!additionalPromptInstructions.equals(other.additionalPromptInstructions))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Schema [documentType=" + documentType + ", documentDescription=" + documentDescription + ", fields=" + fields + ", pages=" + pages
            + ", additionalPromptInstructions=" + additionalPromptInstructions + "]";
    }
}
