/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import java.io.InputStream;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to upload a file using the watsonx.ai Files APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FileUploadRequest.builder()
 *     .inputStream(is)
 *     .fileName("audio.mp3")
 *     .purpose(Purpose.BATCH)
 *     .build();
 * }</pre>
 */
public class FileUploadRequest extends WatsonxParameters {
    private final InputStream inputStream;
    private final String fileName;
    private final Purpose purpose;

    private FileUploadRequest(Builder builder) {
        super(builder);
        inputStream = builder.inputStream;
        fileName = builder.fileName;
        purpose = builder.purpose;
    }

    /**
     * Returns the input stream of the file content to upload.
     *
     * @return the {@link InputStream} of the file
     */
    public InputStream inputStream() {
        return inputStream;
    }

    /**
     * Returns the name of the file to upload.
     *
     * @return the file name
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Returns the purpose of the file upload.
     *
     * @return the {@link Purpose} of the file
     */
    public Purpose purpose() {
        return purpose;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FileUploadRequest.builder()
     *     .inputStream(is)
     *     .fileName("audio.mp3")
     *     .purpose(Purpose.BATCH)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FileUploadRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private InputStream inputStream;
        private String fileName;
        private Purpose purpose;

        private Builder() {}

        /**
         * Sets the input stream of the file content to upload.
         *
         * @param inputStream the {@link InputStream} of the file
         */
        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        /**
         * Sets the name of the file to upload.
         *
         * @param fileName the file name
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the purpose of the file upload.
         *
         * @param purpose the {@link Purpose} of the file
         */
        public Builder purpose(Purpose purpose) {
            this.purpose = purpose;
            return this;
        }

        /**
         * Builds a {@link FileUploadRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link FileUploadRequest}
         */
        public FileUploadRequest build() {
            return new FileUploadRequest(this);
        }
    }
}
