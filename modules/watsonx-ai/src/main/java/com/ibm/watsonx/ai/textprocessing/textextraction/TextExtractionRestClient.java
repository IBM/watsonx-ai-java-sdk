/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.FileNotFoundException;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Code;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Extraction APIs.
 */
public abstract class TextExtractionRestClient extends WatsonxRestClient {
    protected final String cosUrl;
    protected final Authenticator cosAuthenticator;

    protected TextExtractionRestClient(Builder builder) {
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
    public abstract CompletableFuture<Boolean> asyncDeleteFile(DeleteFileRequest request);

    /**
     * Reads the content of a file from the specified COS bucket.
     *
     * @param request The {@link ReadFileRequest} containing bucket and file information.
     * @return The file content as a string.
     */
    public abstract String readFile(ReadFileRequest request) throws FileNotFoundException;

    /**
     * Uploads a file stream to the specified COS bucket.
     *
     * @param request The {@link UploadRequest} containing bucket, file stream, and name information.
     * @return {@code true} if the upload request was successfully sent.
     */
    public abstract boolean upload(UploadRequest request);

    /**
     * Deletes a previously submitted text extraction job.
     *
     * @param request The {@link DeleteExtractionRequest} containing extraction id and parameters.
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteExtraction(DeleteExtractionRequest request);

    /**
     * Retrieves the details and results of a previously submitted text extraction job.
     *
     * @param request The {@link FetchExtractionDetailsRequest} containing extraction id and fetch parameters.
     * @return A {@link TextExtractionResponse} containing the job status and results.
     */
    public abstract TextExtractionResponse fetchExtractionDetails(FetchExtractionDetailsRequest request);

    /**
     * Starts a new text extraction job.
     *
     * @param request The {@link StartExtractionRequest} containing input/output references and extraction parameters.
     * @return A {@link TextExtractionResponse} representing the created extraction job.
     */
    public abstract TextExtractionResponse startExtraction(StartExtractionRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link TextExtractionRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TextExtractionRestClient.Builder builder() {
        return ServiceLoader.load(TextExtractionRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Inspects the given {@link WatsonxException} and maps it to a {@link FileNotFoundException} if it corresponds to a {@code COS_FILE_NOT_FOUND}
     * error.
     * <p>
     * If the exception does not represent a file-not-found condition, the original {@link WatsonxException} is returned.
     *
     * @param e the {@link WatsonxException} to inspect
     * @return a {@link FileNotFoundException} if the exception indicates a missing COS file, otherwise the original {@link WatsonxException}
     */
    protected Exception mapIfCosFileNotFound(WatsonxException e) {
        if (e.statusCode() == 404 && e.details().isPresent()) {
            var details = e.details().get();
            var fileNotFound = details.errors().stream()
                .filter(error -> error.is(Code.COS_FILE_NOT_FOUND))
                .findFirst()
                .map(Error::message)
                .orElse(e.getMessage());
            return new FileNotFoundException(fileNotFound);
        }

        return e;
    }

    /**
     * Builder abstract class for constructing {@link TextExtractionRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TextExtractionRestClient, Builder> {
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
    public interface TextExtractionRestClientBuilderFactory extends Supplier<TextExtractionRestClient.Builder> {}

    /**
     * Represents a request to delete a previously submitted text extraction job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param extractionId the unique identifier of the extraction job to delete.
     * @param parameters additional parameters controlling the delete operation.
     */
    public record DeleteExtractionRequest(String requestTrackingId, String extractionId, TextExtractionDeleteParameters parameters) {

        /**
         * Creates a new {@link DeleteExtractionRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param extractionId the unique identifier of the extraction job to delete.
         * @param parameters additional parameters controlling the delete operation.
         * @return a new {@link DeleteExtractionRequest} instance.
         */
        public static DeleteExtractionRequest of(String requestTrackingId, String extractionId, TextExtractionDeleteParameters parameters) {
            return new DeleteExtractionRequest(requestTrackingId, extractionId, parameters);
        }
    }

    /**
     * Represents a request to fetch the details and results of a text extraction job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param extractionId the unique identifier of the extraction job to fetch.
     * @param parameters additional parameters specifying the fetch operation.
     */
    public record FetchExtractionDetailsRequest(String requestTrackingId, String extractionId, TextExtractionFetchParameters parameters) {

        /**
         * Creates a new {@link FetchExtractionDetailsRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param extractionId the unique identifier of the extraction job to fetch.
         * @param parameters additional parameters specifying the fetch operation.
         * @return a new {@link FetchExtractionDetailsRequest} instance.
         */
        public static FetchExtractionDetailsRequest of(String requestTrackingId, String extractionId, TextExtractionFetchParameters parameters) {
            return new FetchExtractionDetailsRequest(requestTrackingId, extractionId, parameters);
        }
    }

    /**
     * Represents a request to start a new text extraction job.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
     * @param transactionId optional transaction identifier for correlating multiple related operations.
     * @param textExtractionRequest the request body containing input/output references and extraction parameters.
     */
    public record StartExtractionRequest(String requestTrackingId, String transactionId, TextExtractionRequest textExtractionRequest) {

        /**
         * Creates a new {@link StartExtractionRequest} instance.
         *
         * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
         * @param transactionId optional transaction identifier for correlating multiple related operations.
         * @param textExtractionRequest the request body containing input/output references and extraction parameters.
         * @return a new {@link StartExtractionRequest} instance.
         */
        public static StartExtractionRequest of(String requestTrackingId, String transactionId, TextExtractionRequest textExtractionRequest) {
            return new StartExtractionRequest(requestTrackingId, transactionId, textExtractionRequest);
        }
    }
}
