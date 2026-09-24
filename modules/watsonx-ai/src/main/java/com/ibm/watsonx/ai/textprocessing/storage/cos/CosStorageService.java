/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Code;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLogger;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.storage.StorageOperations;

/**
 * Service class to interact with IBM Cloud Object Storage.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CosStorageService cosStorageService = CosStorageService.builder()
 *     .cosUrl(COS_URL)
 *     .bucket("my-bucket")
 *     .apiKey(API_KEY)
 *     .build();
 *
 * cosStorageService.upload(inputStream, "my-document.pdf");
 * var content = cosStorageService.readFile("my-document.pdf");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public final class CosStorageService implements StorageOperations {

    private static final Logger logger = LoggerFactory.getLogger(CosStorageService.class);
    private final CosStorageRestClient client;

    private CosStorageService(Builder builder) {
        this.client = CosStorageRestClient.builder()
            .cosUrl(builder.cosUrl)
            .bucket(builder.bucket)
            .authenticator(builder.authenticator)
            .cosAuthenticator(builder.cosAuthenticator)
            .httpClient(builder.httpClient)
            .timeout(builder.timeout != null ? builder.timeout : Duration.ofSeconds(60))
            .logRequests(builder.logRequests)
            .logResponses(builder.logResponses)
            .requestLogger(builder.requestLogger, builder.requestLogLevel)
            .responseLogger(builder.responseLogger, builder.responseLogLevel)
            .verifySsl(builder.verifySsl)
            .build();
    }

    @Override
    public boolean upload(String requestId, InputStream is, String fileName) {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(is, "is cannot be null");
        requireNonNull(fileName, "fileName cannot be null");
        return client.upload(requestId, is, fileName);
    }

    @Override
    public boolean upload(String requestId, File file) {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(file, "file cannot be null");
        return client.upload(requestId, file);
    }

    @Override
    public String uploadAndGetId(String requestId, InputStream is, String fileName) {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(is, "is cannot be null");
        requireNonNull(fileName, "fileName cannot be null");
        return client.uploadAndGetId(requestId, is, fileName);
    }

    @Override
    public String readFile(String requestId, String fileName) throws FileNotFoundException {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(fileName, "fileName cannot be null");
        try {
            return client.readFile(requestId, fileName);
        } catch (WatsonxException e) {
            Exception mapped = mapIfCosFileNotFound(e);
            if (mapped instanceof FileNotFoundException fnf)
                throw fnf;
            throw e;
        }
    }

    @Override
    public boolean deleteFile(String requestId, String fileName) throws FileNotFoundException {
        requireNonNull(requestId, "requestId cannot be null");
        try {
            return client.deleteFileAsync(DeleteFileRequest.of(requestId, null, fileName)).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WatsonxException ex) {
                Exception mapped = mapIfCosFileNotFound(ex);
                if (mapped instanceof FileNotFoundException fnf)
                    throw fnf;
                throw ex;
            }
            if (e.getCause() instanceof RuntimeException re)
                throw re;
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void deleteFileAsync(String requestId, String fileName) {
        client.deleteFileAsync(DeleteFileRequest.of(requestId, null, fileName))
            .exceptionally(ex -> {
                logger.warn("Async COS delete failed for {}: {}", fileName, ex.getMessage());
                return false;
            });
    }

    /**
     * Asynchronously deletes a file using a {@link DeleteFileRequest} that carries an optional request tracking id. Errors are logged as warnings and
     * not propagated to the caller.
     * <p>
     * Used internally by the text processing services to clean up files after processing.
     *
     * @param request The {@link DeleteFileRequest} containing the file name and optional tracking id.
     */
    public void deleteFileAsync(DeleteFileRequest request) {
        client.deleteFileAsync(request)
            .exceptionally(ex -> {
                logger.warn("Async COS delete failed for {}: {}", request.fileName(), ex.getMessage());
                return false;
            });
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * CosStorageService cosStorageService = CosStorageService.builder()
     *     .cosUrl(COS_URL)
     *     .bucket("my-bucket")
     *     .apiKey(API_KEY)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link CosStorageService} instances with configurable parameters.
     */
    public static final class Builder {
        private String cosUrl;
        private String bucket;
        private Authenticator authenticator;
        private Authenticator cosAuthenticator;
        private HttpClient httpClient;
        private Duration timeout;
        private boolean logRequests;
        private boolean logResponses;
        private HttpRequestLogger requestLogger;
        private Level requestLogLevel = Level.INFO;
        private HttpResponseLogger responseLogger;
        private Level responseLogLevel = Level.INFO;
        private boolean verifySsl = true;

        private Builder() {}

        /**
         * Specifies the COS base URL to be used for reading and writing files.
         *
         * @param cosUrl The base COS URL as a string.
         */
        public Builder cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return this;
        }

        /**
         * Specifies the COS bucket name.
         *
         * @param bucket The COS bucket name.
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Sets an {@link IBMCloudAuthenticator}-based {@link Authenticator}, initialized from the provided IBM Cloud API key.
         * <p>
         * For alternative authentication mechanisms, use {@link #authenticator(Authenticator)}.
         *
         * @param apiKey IBM Cloud API key.
         */
        public Builder apiKey(String apiKey) {
            requireNonNull(apiKey, "The apiKey must be provided");
            authenticator = IBMCloudAuthenticator.builder()
                .httpClient(httpClient)
                .apiKey(apiKey)
                .build();
            return this;
        }

        /**
         * Sets the {@link Authenticator} used to authenticate requests.
         * <p>
         * For IBM Cloud IAM authentication, {@link #apiKey(String)} provides a simpler alternative.
         *
         * @param authenticator The {@link Authenticator} instance.
         */
        public Builder authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        /**
         * Sets an {@link IBMCloudAuthenticator}-based dedicated COS {@link Authenticator}, initialized from the provided IBM Cloud API key.
         * <p>
         * Use this when COS is deployed in a different environment or region than the main service. If not set, the main authenticator is used.
         *
         * @param cosApiKey IBM Cloud API key for COS.
         */
        public Builder cosApiKey(String cosApiKey) {
            requireNonNull(cosApiKey, "The cosApiKey must be provided");
            this.cosAuthenticator = IBMCloudAuthenticator.builder().httpClient(httpClient).apiKey(cosApiKey).build();
            return this;
        }

        /**
         * Specifies a custom authenticator for COS operations.
         * <p>
         * This allows using a different API key or authentication method for COS when it is deployed in a different environment or region. If not
         * specified, the main authenticator is used.
         *
         * @param cosAuthenticator The {@link Authenticator} to use for COS operations.
         */
        public Builder cosAuthenticator(Authenticator cosAuthenticator) {
            this.cosAuthenticator = cosAuthenticator;
            return this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for HTTP communication.
         * <p>
         * If not specified, a default {@link HttpClient} will be created automatically.
         *
         * @param httpClient The custom {@link HttpClient} to use.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the request timeout. Defaults to 60 seconds if not specified.
         *
         * @param timeout The {@link Duration} timeout.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Enables or disables logging of the request payload.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise.
         */
        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        /**
         * Enables or disables logging of the response payload.
         *
         * @param logResponses {@code true} to log the response, {@code false} otherwise.
         */
        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        /**
         * Enables logging of the request payload through a custom logger, fired at the given level.
         *
         * @param requestLogger The custom request logger.
         * @param requestLogLevel The level at which the logger is fired.
         */
        public Builder requestLogger(HttpRequestLogger requestLogger, Level requestLogLevel) {
            this.requestLogger = requestLogger;
            this.requestLogLevel = requestLogLevel;
            return this;
        }

        /**
         * Enables logging of the response payload through a custom logger, fired at the given level.
         *
         * @param responseLogger The custom response logger.
         * @param responseLogLevel The level at which the logger is fired.
         */
        public Builder responseLogger(HttpResponseLogger responseLogger, Level responseLogLevel) {
            this.responseLogger = responseLogger;
            this.responseLogLevel = responseLogLevel;
            return this;
        }

        /**
         * Sets whether SSL/TLS certificate verification should be performed.
         * <p>
         * When set to {@code true} (default), the client validates server certificates. When set to {@code false}, all certificates are accepted
         * without validation, including self-signed certificates.
         *
         * @param verifySsl {@code true} to enable certificate verification, {@code false} to accept all certificates.
         */
        public Builder verifySsl(boolean verifySsl) {
            this.verifySsl = verifySsl;
            return this;
        }

        /**
         * Builds a {@link CosStorageService} instance using the configured parameters.
         *
         * @return A new instance of {@link CosStorageService}.
         */
        public CosStorageService build() {
            return new CosStorageService(this);
        }
    }

    private Exception mapIfCosFileNotFound(WatsonxException e) {
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
}
