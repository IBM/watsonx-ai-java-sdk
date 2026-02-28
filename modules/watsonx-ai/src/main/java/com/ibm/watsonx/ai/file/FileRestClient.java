/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Files APIs.
 */
public abstract class FileRestClient extends WatsonxRestClient {

    protected FileRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Uploads a file.
     *
     * @param fileUploadRequest the {@link FileUploadRequest} containing the file and upload parameters.
     * @return a {@link FileData} object representing the uploaded file.
     */
    public abstract FileData upload(FileUploadRequest fileUploadRequest);

    /**
     * Returns a list of files that have been uploaded.
     *
     * @param fileListRequest the {@link FileListRequest}.
     * @return a {@link FileListResponse} containing the list of matching files and pagination metadata.
     */
    public abstract FileListResponse list(FileListRequest fileListRequest);

    /**
     * Retrieves the contents of an uploaded file.
     *
     * @param fileRetrieveRequest the {@link FileRetrieveRequest}.
     * @return content of the uploaded file.
     */
    public abstract String retrieve(FileRetrieveRequest fileRetrieveRequest);

    /**
     * Creates a new {@link Builder} using the first available {@link FileRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static FileRestClient.Builder builder() {
        return ServiceLoader.load(FileRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link FileRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<FileRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface FileRestClientBuilderFactory extends Supplier<FileRestClient.Builder> {}
}
