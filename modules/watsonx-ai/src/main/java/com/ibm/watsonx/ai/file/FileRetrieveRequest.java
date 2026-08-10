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
public final class FileRetrieveRequest extends WatsonxParameters {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        FileRetrieveRequest other = (FileRetrieveRequest) obj;
        if (fileId == null) {
            if (other.fileId != null)
                return false;
        } else if (!fileId.equals(other.fileId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FileRetrieveRequest [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", fileId=" + fileId
            + "]";
    }
}
