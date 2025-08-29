/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import java.util.List;
import java.util.Map;

/**
 * Represents a request for the text extraction api.
 */
public record TextExtractionRequest(String projectId, String spaceId, DataReference documentReference, DataReference resultsReference,
    Parameters parameters, Map<String, Object> custom) {

    /**
     * Represents a reference to external or internal data used during text extraction.
     *
     * @param type The type of data source. Allowable value: {@code connection_asset}.
     * @param connection The connection details containing credentials and context needed to access the data.
     * @param location The specific location details of the data within the connection.
     */
    public record DataReference(String type, CosDataConnection connection, CosDataLocation location) {

        public static final String TYPE_CONNECTION_ASSET = "connection_asset";

        public DataReference {
            type = TYPE_CONNECTION_ASSET;
        }
    };

    /**
     * Represents the connection asset that holds credentials and configuration to access a COS (Cloud Object Storage) location.
     *
     * @param id The unique identifier of the connection asset.
     */
    public record CosDataConnection(String id) {
        public CosDataConnection {
            requireNonNull(id, "id cannot be null");
        }
    };

    /**
     * Represents the location of the data within a Cloud Object Storage (COS) connection.
     *
     * @param fileName The name of the file or directory in the bucket.
     * @param bucket Optional override for the bucket name defined in the connection asset.
     */
    public record CosDataLocation(String fileName, String bucket) {
        public CosDataLocation {
            requireNonNull(fileName, "fileName cannot be null");
        }
    }

    public record Parameters(
        List<String> requestedOutputs,
        String mode,
        String ocrMode,
        List<String> languages,
        Boolean autoRotationCorrection,
        String createEmbeddedImages,
        Integer outputDpi,
        Boolean outputTokensAndBbox,
        String kvpMode,
        SemanticConfig semanticConfig) {

        public static Parameters of(List<String> requestedOutputs) {
            return new Parameters(requestedOutputs, null, null, null, null, null, null, null, null, null);
        }
    }

    /**
     * Represents a semantic key-value pair field used in schema extraction.
     *
     * @param description a description of the field to identify
     * @param example an example value to help inform the language model of the structure and format
     */
    public record KvpField(String description, String example) {}

    /**
     * Represents the semantic configuration for text extraction.
     * <p>
     * This configuration includes options to control image downscaling, text hints usage, generic key-value pair extraction, and custom schemas for
     * semantic KVP extraction.
     */
    public static class SemanticConfig {
        private final Integer targetImageWidth;
        private final Boolean enableTextHints;
        private final Boolean enableGenericKvp;
        private final List<Schema> schemas;

        public SemanticConfig(Builder builder) {
            this.targetImageWidth = builder.targetImageWidth;
            this.enableTextHints = builder.enableTextHints;
            this.enableGenericKvp = builder.enableGenericKvp;
            this.schemas = builder.schemas;
        }

        public Integer getTargetImageWidth() {
            return targetImageWidth;
        }

        public Boolean getEnableTextHints() {
            return enableTextHints;
        }

        public Boolean getEnableGenericKvp() {
            return enableGenericKvp;
        }

        public List<Schema> getSchemas() {
            return schemas;
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
         * Builder class for constructing {@link SemanticConfig} instance.
         */
        public static class Builder {
            private Integer targetImageWidth;
            private Boolean enableTextHints;
            private Boolean enableGenericKvp;
            private List<Schema> schemas;

            private Builder() {}

            /**
             * Sets the target image width for downscaling.
             *
             * @param targetImageWidth the target width in pixels
             * @return this builder instance
             */
            public Builder targetImageWidth(Integer targetImageWidth) {
                this.targetImageWidth = targetImageWidth;
                return this;
            }

            /**
             * Sets whether to enable text hints during extraction.
             *
             * @param enableTextHints true to enable text hints, false otherwise
             * @return this builder instance
             */
            public Builder enableTextHints(Boolean enableTextHints) {
                this.enableTextHints = enableTextHints;
                return this;
            }

            /**
             * Sets whether to enable generic key-value pair extraction.
             *
             * @param enableGenericKvp true to enable generic KVP extraction, false otherwise
             * @return this builder instance
             */
            public Builder enableGenericKvp(Boolean enableGenericKvp) {
                this.enableGenericKvp = enableGenericKvp;
                return this;
            }

            /**
             * Sets the list of custom semantic schemas.
             *
             * @param schemas the list of schemas to use
             * @return this builder instance
             */
            public Builder schemas(List<Schema> schemas) {
                this.schemas = schemas;
                return this;
            }

            /**
             * Builds a {@link SemanticConfig} instance.
             *
             * @return a new instance of {@link SemanticConfig}
             */
            public SemanticConfig build() {
                return new SemanticConfig(this);
            }
        }
    }

    /**
     * Builder class for constructing {@link Schema} instances.
     */
    public static class Schema {
        private final String documentType;
        private final String documentDescription;
        private final Integer targetImageWidth;
        private final Boolean enableTextHints;
        private final Boolean enableGenericKvp;
        private final KvpField fields;

        public Schema(Builder builder) {
            this.documentType = builder.documentType;
            this.documentDescription = builder.documentDescription;
            this.targetImageWidth = builder.targetImageWidth;
            this.enableTextHints = builder.enableTextHints;
            this.enableGenericKvp = builder.enableGenericKvp;
            this.fields = builder.fields;
        }

        public String getDocumentType() {
            return documentType;
        }

        public String getDocumentDescription() {
            return documentDescription;
        }

        public Integer getTargetImageWidth() {
            return targetImageWidth;
        }

        public Boolean getEnableTextHints() {
            return enableTextHints;
        }

        public Boolean getEnableGenericKvp() {
            return enableGenericKvp;
        }

        public KvpField getFields() {
            return fields;
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
        public static class Builder {
            private String documentType;
            private String documentDescription;
            private Integer targetImageWidth;
            private Boolean enableTextHints;
            private Boolean enableGenericKvp;
            private KvpField fields;

            private Builder() {}

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
             * Sets the target image width to downscale the input image.
             *
             * @param targetImageWidth the target image width in pixels
             */
            public Builder targetImageWidth(Integer targetImageWidth) {
                this.targetImageWidth = targetImageWidth;
                return this;
            }

            /**
             * Sets whether to enable text hints for this schema.
             *
             * @param enableTextHints true to enable, false to disable
             */
            public Builder enableTextHints(Boolean enableTextHints) {
                this.enableTextHints = enableTextHints;
                return this;
            }

            /**
             * Sets whether to enable generic KVP extraction for this schema.
             *
             * @param enableGenericKvp true to enable, false to disable
             */
            public Builder enableGenericKvp(Boolean enableGenericKvp) {
                this.enableGenericKvp = enableGenericKvp;
                return this;
            }

            /**
             * Sets mapping of fields to identify within the schema, where each key is the short-form name of the field, and the corresponding value
             * is an object is a schema as defined below.
             *
             * @param fields the list of {@link KvpField} objects representing fields
             */
            public Builder fields(KvpField fields) {
                this.fields = fields;
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
    }
}
