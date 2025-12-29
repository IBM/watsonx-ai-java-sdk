/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Classification APIs.
 */
public abstract class TextClassificationRestClient extends WatsonxRestClient {
    protected final String cosUrl;
    protected final Authenticator cosAuthenticator;

    protected TextClassificationRestClient(Builder builder) {
        super(builder);
        cosUrl = requireNonNull(builder.cosUrl, "cosUrl cannot be null");
        cosAuthenticator = requireNonNullElse(builder.cosAuthenticator, authenticator);
    }

    /**
     * Starts a new text classification job.
     *
     * @param request The {@link StartClassificationRequest} containing input/output references and classification parameters.
     * @return A {@link TextClassificationResponse} representing the created classification job.
     */
    public abstract TextClassificationResponse startClassification(StartClassificationRequest request);

    /**
     * Fetches the details and results of a previously submitted text classification job.
     *
     * @param request The {@link FetchClassificationDetailsRequest} containing classification id and fetch parameters.
     * @return A {@link TextClassificationResponse} containing the job status and results.
     */
    public abstract TextClassificationResponse fetchClassificationDetails(FetchClassificationDetailsRequest request);

    /**
     * Deletes a previously submitted text classification job.
     *
     * @param request The {@link DeleteClassificationRequest} containing classification id and parameters.
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteClassification(DeleteClassificationRequest request);

    /**
     * Deletes a file from the specified COS bucket.
     *
     * @param request The {@link DeleteFileRequest} containing bucket and file information.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteFile(DeleteFileRequest request);

    /**
     * Asynchronously deletes a file from the specified COS bucket.
     *
     * @param request The {@link DeleteFileRequest} containing bucket and file information.
     * @return A {@link CompletableFuture} that completes with {@code true} if the file was successfully deleted.
     */
    public abstract CompletableFuture<Boolean> asyncDeleteFile(DeleteFileRequest request);

    /**
     * Uploads a file stream to the specified COS bucket.
     *
     * @param request The {@link UploadRequest} containing bucket, file stream, and name information.
     * @return {@code true} if the upload request was successfully sent.
     */
    public abstract boolean upload(UploadRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link TextClassificationRestClientBuilderFactory} discovered via
     * {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TextClassificationRestClient.Builder builder() {
        return ServiceLoader.load(TextClassificationRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link TextClassificationRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TextClassificationRestClient, Builder> {
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
    public interface TextClassificationRestClientBuilderFactory extends Supplier<TextClassificationRestClient.Builder> {}

    /**
     * Represents a request to start a new text classification job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param transactionId optional transaction identifier for correlating multiple related operations.
     * @param textClassificationRequest the request body containing input/output references and classification parameters.
     */
    public record StartClassificationRequest(String requestTrackingId, String transactionId, TextClassificationRequest textClassificationRequest) {

        /**
         * Creates a new {@link StartClassificationRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param transactionId optional transaction identifier for correlating multiple related operations.
         * @param textClassificationRequest the request body containing input/output references and classification parameters.
         * @return a new {@link StartClassificationRequest} instance.
         */
        public static StartClassificationRequest of(String requestTrackingId, String transactionId,
            TextClassificationRequest textClassificationRequest) {
            return new StartClassificationRequest(requestTrackingId, transactionId, textClassificationRequest);
        }
    }

    /**
     * Represents a request to fetch the details and results of a text classification job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param classificationId the unique identifier of the classification job to fetch.
     * @param parameters additional parameters specifying the fetch operation.
     */
    public record FetchClassificationDetailsRequest(String requestTrackingId, String classificationId, TextClassificationFetchParameters parameters) {

        /**
         * Creates a new {@link FetchClassificationDetailsRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param classificationId the unique identifier of the classification job to fetch.
         * @param parameters additional parameters specifying the fetch operation.
         * @return a new {@link FetchClassificationDetailsRequest} instance.
         */
        public static FetchClassificationDetailsRequest of(String requestTrackingId, String classificationId,
            TextClassificationFetchParameters parameters) {
            return new FetchClassificationDetailsRequest(requestTrackingId, classificationId, parameters);
        }
    }

    /**
     * Represents a request to delete a previously submitted text classification job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param classificationId the unique identifier of the classification job to delete.
     * @param parameters additional parameters controlling the delete operation.
     */
    public record DeleteClassificationRequest(String requestTrackingId, String classificationId, TextClassificationDeleteParameters parameters) {

        /**
         * Creates a new {@link DeleteClassificationRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param classificationId the unique identifier of the classification job to delete.
         * @param parameters additional parameters controlling the delete operation.
         * @return a new {@link DeleteClassificationRequest} instance.
         */
        public static DeleteClassificationRequest of(String requestTrackingId, String classificationId,
            TextClassificationDeleteParameters parameters) {
            return new DeleteClassificationRequest(requestTrackingId, classificationId, parameters);
        }
    }

}
