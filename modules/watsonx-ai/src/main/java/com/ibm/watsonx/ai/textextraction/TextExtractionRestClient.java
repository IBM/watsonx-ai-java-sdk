/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Extraction APIs.
 */
public abstract class TextExtractionRestClient extends WatsonxRestClient {

    protected final String cosUrl;

    protected TextExtractionRestClient(Builder builder) {
        super(builder);
        this.cosUrl = requireNonNull(builder.cosUrl, "cosUrl cannot be null");
    }

    /**
     * Deletes a file from the specified COS bucket.
     *
     * @param request The {@link DeleteFileRequest} containing bucket and file information.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteFile(DeleteFileRequest request);

    /**
     * Deletes a file from the specified COS bucket.
     *
     * @param request The {@link DeleteFileRequest} containing bucket and file information.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public abstract CompletableFuture<Boolean> asyncDeleteFile(DeleteFileRequest request);

    /**
     * Reads the content of a file from the specified COS bucket.
     *
     * @param request The {@link ReadFileRequest} containing bucket and file information.
     * @return The file content as a string.
     */
    public abstract String readFile(ReadFileRequest request);

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
     * Fetches the details and results of a previously submitted text extraction job.
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
     * Builder abstract class for constructing {@link TextExtractionRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TextExtractionRestClient, Builder> {
        private String cosUrl;

        /**
         * Specifies the Cloud Object Storage (COS) base URL to be used for reading and writing files.
         *
         * @param cosUrl The base COS URL as a string.
         */
        public Builder cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks (e.g., Quarkus, Spring) to provide their own client implementations.
     */
    public interface TextExtractionRestClientBuilderFactory extends Supplier<TextExtractionRestClient.Builder> {}

    /**
     * Request wrapper for deleting a file from Cloud Object Storage.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests
     * @param bucketName The name of the COS bucket containing the file.
     * @param fileName The name of the file to delete.
     */
    public record DeleteFileRequest(String requestTrackingId, String bucketName, String fileName) {
        public static DeleteFileRequest of(String bucketName, String fileName) {
            return of(null, bucketName, fileName);
        }

        public static DeleteFileRequest of(String requestTrackingId, String bucketName, String fileName) {
            return new DeleteFileRequest(requestTrackingId, bucketName, fileName);
        }
    }

    /**
     * Request wrapper for reading a file from Cloud Object Storage.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
     * @param bucketName The name of the COS bucket containing the file.
     * @param fileName The name of the file to read.
     */
    public record ReadFileRequest(String requestTrackingId, String bucketName, String fileName) {
        public static ReadFileRequest of(String bucketName, String fileName) {
            return of(null, bucketName, fileName);
        }

        public static ReadFileRequest of(String requestTrackingId, String bucketName, String fileName) {
            return new ReadFileRequest(requestTrackingId, bucketName, fileName);
        }
    }

    /**
     * Request wrapper for uploading a file to Cloud Object Storage.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
     * @param bucketName The name of the COS bucket where the file will be uploaded.
     * @param is The {@link InputStream} containing the file content.
     * @param fileName The name of the file to store in the bucket.
     */
    public record UploadRequest(String requestTrackingId, String bucketName, InputStream is, String fileName) {
        public static UploadRequest of(String bucketName, InputStream is, String fileName) {
            return of(null, bucketName, is, fileName);
        }

        public static UploadRequest of(String requestTrackingId, String bucketName, InputStream is, String fileName) {
            return new UploadRequest(requestTrackingId, bucketName, is, fileName);
        }
    }

    /**
     * Request wrapper for deleting a previously submitted text extraction job.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
     * @param extractionId The unique identifier of the extraction job to delete.
     * @param parameters Additional parameters controlling the delete operation.
     */
    public record DeleteExtractionRequest(String requestTrackingId, String extractionId, TextExtractionDeleteParameters parameters) {
        public static DeleteExtractionRequest of(String extractionId, TextExtractionDeleteParameters parameters) {
            return of(null, extractionId, parameters);
        }

        public static DeleteExtractionRequest of(String requestTrackingId, String extractionId, TextExtractionDeleteParameters parameters) {
            return new DeleteExtractionRequest(requestTrackingId, extractionId, parameters);
        }
    }

    /**
     * Request wrapper for fetching details and results of a text extraction job.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
     * @param extractionId The unique identifier of the extraction job to fetch.
     * @param parameters Additional parameters specifying the fetch operation.
     */
    public record FetchExtractionDetailsRequest(String requestTrackingId, String extractionId, TextExtractionFetchParameters parameters) {
        public static FetchExtractionDetailsRequest of(String extractionId, TextExtractionFetchParameters parameters) {
            return of(null, extractionId, parameters);
        }

        public static FetchExtractionDetailsRequest of(String requestTrackingId, String extractionId, TextExtractionFetchParameters parameters) {
            return new FetchExtractionDetailsRequest(requestTrackingId, extractionId, parameters);
        }
    }

    /**
     * Request wrapper for starting a new text extraction job.
     *
     * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
     * @param transactionId Optional transaction identifier for correlating multiple related operations.
     * @param textExtractionRequest The request body containing input/output references and extraction parameters.
     */
    public record StartExtractionRequest(String requestTrackingId, String transactionId, TextExtractionRequest textExtractionRequest) {
        public static StartExtractionRequest of(TextExtractionRequest textExtractionRequest) {
            return of(null, null, textExtractionRequest);
        }

        public static StartExtractionRequest withTransactionId(String transactionId, TextExtractionRequest textExtractionRequest) {
            return of(null, transactionId, textExtractionRequest);
        }

        public static StartExtractionRequest withRequestTrackingId(String requestTrackingId, TextExtractionRequest textExtractionRequest) {
            return of(requestTrackingId, null, textExtractionRequest);
        }

        public static StartExtractionRequest of(String requestTrackingId, String transactionId, TextExtractionRequest textExtractionRequest) {
            return new StartExtractionRequest(requestTrackingId, transactionId, textExtractionRequest);
        }
    }
}
