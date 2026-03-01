/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to delete a file from the watsonx.ai Files APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FileDeleteRequest.builder()
 *     .fileId("file-abc123")
 *     .build();
 * }</pre>
 */
public class FileDeleteRequest extends WatsonxParameters {
    private final String fileId;

    private FileDeleteRequest(Builder builder) {
        super(builder);
        fileId = builder.fileId;
    }

    /**
     * Returns the identifier of the file to delete.
     *
     * @return the file identifier
     */
    public String fileId() {
        return fileId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FileDeleteRequest.builder()
     *     .fileId("file-abc123")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FileDeleteRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String fileId;

        private Builder() {}

        /**
         * Sets the identifier of the file to delete.
         *
         * @param fileId the file identifier
         * @return this builder
         */
        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        /**
         * Builds a {@link FileDeleteRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link FileDeleteRequest}
         */
        public FileDeleteRequest build() {
            return new FileDeleteRequest(this);
        }
    }
}
