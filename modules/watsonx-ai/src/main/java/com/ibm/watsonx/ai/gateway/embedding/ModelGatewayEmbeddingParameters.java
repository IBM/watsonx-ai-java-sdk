/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static java.util.Objects.isNull;

/**
 * Parameters specific to the Model Gateway embeddings endpoint.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayEmbeddingParameters parameters = ModelGatewayEmbeddingParameters.builder()
 *     .dimensions(512)
 *     .encodingFormat(EncodingFormat.FLOAT)
 *     .build();
 * }</pre>
 *
 * @see ModelGatewayEmbeddingService
 */
public final class ModelGatewayEmbeddingParameters {

    /**
     * Supported wire formats for the embedding vector returned by the gateway.
     */
    public enum EncodingFormat {

        /**
         * Sends the vector as a JSON array of numbers. This is the default.
         */
        FLOAT("float"),

        /**
         * Sends the vector as a Base64-encoded string, which the SDK decodes for you.
         */
        BASE64("base64");

        private final String value;

        EncodingFormat(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    private final Integer dimensions;
    private final String encodingFormat;
    private final String user;

    private ModelGatewayEmbeddingParameters(Builder builder) {
        dimensions = builder.dimensions;
        encodingFormat = builder.encodingFormat;
        user = builder.user;
    }

    /**
     * Returns the number of dimensions for the output embeddings.
     *
     * @return the dimensions, or {@code null} if not set
     */
    public Integer dimensions() {
        return dimensions;
    }

    /**
     * Returns the format in which to return the embeddings.
     *
     * @return the encoding format, or {@code null} if not set
     */
    public String encodingFormat() {
        return encodingFormat;
    }

    /**
     * Returns a unique identifier representing the end-user.
     *
     * @return the user identifier, or {@code null} if not set
     */
    public String user() {
        return user;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayEmbeddingParameters} instances.
     */
    public static final class Builder {

        private Integer dimensions;
        private String encodingFormat;
        private String user;

        private Builder() {}

        /**
         * Sets the number of dimensions for the output embeddings.
         *
         * @param dimensions the desired number of dimensions
         */
        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * Sets the format in which to return the embeddings ({@code "float"} or {@code "base64"}).
         *
         * @param encodingFormat the encoding format string
         */
        public Builder encodingFormat(String encodingFormat) {
            this.encodingFormat = encodingFormat;
            return this;
        }

        /**
         * Sets the format in which to return the embeddings using the {@link EncodingFormat} enum.
         *
         * @param encodingFormat the encoding format
         */
        public Builder encodingFormat(EncodingFormat encodingFormat) {
            this.encodingFormat = isNull(encodingFormat) ? null : encodingFormat.value();
            return this;
        }

        /**
         * Sets a unique identifier representing the end-user.
         *
         * @param user the user identifier
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayEmbeddingParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayEmbeddingParameters}
         */
        public ModelGatewayEmbeddingParameters build() {
            return new ModelGatewayEmbeddingParameters(this);
        }
    }

    @Override
    public String toString() {
        return "ModelGatewayEmbeddingParameters [dimensions=" + dimensions + ", encodingFormat=" + encodingFormat + ", user=" + user + "]";
    }
}
