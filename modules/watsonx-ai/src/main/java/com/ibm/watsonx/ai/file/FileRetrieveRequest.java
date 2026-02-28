/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to retrieve a file from the watsonx.ai Files APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FileRetrieveRequest.builder()
 *     .fileId("file-abc123")
 *     .build();
 * }</pre>
 */
public class FileRetrieveRequest extends WatsonxParameters {
    private final String fileId;

    private FileRetrieveRequest(Builder builder) {
        super(builder);
        fileId = builder.fileId;
    }

    /**
     * Returns the identifier of the file to retrieve.
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
     * FileRetrieveRequest.builder()
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
     * Builder class for constructing {@link FileRetrieveRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String fileId;

        private Builder() {}

        /**
         * Sets the identifier of the file to retrieve.
         *
         * @param fileId the file identifier
         */
        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        /**
         * Builds a {@link FileRetrieveRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link FileRetrieveRequest}
         */
        public FileRetrieveRequest build() {
            return new FileRetrieveRequest(this);
        }
    }
}
