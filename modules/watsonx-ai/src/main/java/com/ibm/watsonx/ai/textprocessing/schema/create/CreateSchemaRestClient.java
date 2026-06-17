/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.FileNotFoundException;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Create Schema APIs.
 */
public abstract class CreateSchemaRestClient extends WatsonxRestClient {
    protected final String cosUrl;
    protected final Authenticator cosAuthenticator;

    protected CreateSchemaRestClient(Builder builder) {
        super(builder);
        cosUrl = requireNonNull(builder.cosUrl, "cosUrl cannot be null");
        cosAuthenticator = requireNonNullElse(builder.cosAuthenticator, authenticator);
    }

    /**
     * Deletes a file from the specified COS bucket.
     *
     * @param request The {@link DeleteFileRequest} containing bucket and file information.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteFile(DeleteFileRequest request) throws FileNotFoundException;

    /**
     * Asynchronously deletes a file from the specified COS bucket.
     *
     * @param request the {@link DeleteFileRequest} containing bucket and file information.
     * @return a {@link CompletableFuture} that resolves to {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public abstract CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request);

    /**
     * Uploads a file to the specified COS bucket.
     *
     * @param request The {@link UploadRequest} containing bucket, file stream, and name information.
     * @return {@code true} if the upload request was successfully sent.
     */
    public abstract boolean uploadFile(UploadRequest request);

    /**
     * Deletes a submitted create request job.
     *
     * @param request The {@link DeleteRequest} containing request id and parameters.
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteRequest(DeleteRequest request);

    /**
     * Retrieves the details and results of submitted create request job.
     *
     * @param request The {@link FetchDetailsRequest} containing request id and fetch parameters.
     * @return A {@link CreateSchemaResponse} containing the job status and results.
     */
    public abstract CreateSchemaResponse fetchRequestDetails(FetchDetailsRequest request);

    /**
     * Starts a new create request job.
     *
     * @param request The {@link StartCreateSchemaRequest} containing input/output references and schema parameters.
     * @return A {@link CreateSchemaResponse} representing the created create schema job.
     */
    public abstract CreateSchemaResponse startRequest(StartCreateSchemaRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link CreateSchemaRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static CreateSchemaRestClient.Builder builder() {
        return ServiceLoader.load(CreateSchemaRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link CreateSchemaRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<CreateSchemaRestClient, Builder> {
        private String cosUrl;
        private Authenticator cosAuthenticator;

        /**
         * Specifies the Cloud Object Storage (COS) base URL to be used for reading and writing files.
         *
         * @param cosUrl The base COS URL as a string.
         */
        public Builder cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return this;
        }

        /**
         * Specifies a custom authenticator for Cloud Object Storage (COS) operations.
         * <p>
         * This allows using a different API key or authentication method for COS when it is deployed in a different environment or region than the
         * main service. If not specified, the main service authenticator will be used.
         *
         * @param cosAuthenticator The {@link Authenticator} to use for COS operations.
         */
        public Builder cosAuthenticator(Authenticator cosAuthenticator) {
            this.cosAuthenticator = cosAuthenticator;
            return this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface CreateSchemaRestClientBuilderFactory extends Supplier<CreateSchemaRestClient.Builder> {}
}
