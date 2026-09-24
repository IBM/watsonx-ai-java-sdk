/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLogger;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;

/**
 * Abstraction of a REST client for IBM Cloud Object Storage operations used by text processing services.
 */
public abstract class CosStorageRestClient {

    protected final String cosUrl;
    protected final String bucket;
    protected final Duration timeout;

    protected CosStorageRestClient(Builder<?> builder) {
        this.cosUrl = builder.cosUrl;
        this.bucket = builder.bucket;
        this.timeout = builder.timeout;
    }

    /**
     * Uploads a file from an {@link InputStream}.
     *
     * @param requestTrackingId optional identifier used to correlate log entries for this request
     * @param is The input stream of the file content.
     * @param fileName The name of the file in the storage backend.
     * @return {@code true} if the upload succeeded.
     */
    public abstract boolean upload(String requestTrackingId, InputStream is, String fileName);

    /**
     * Uploads a local {@link File}.
     *
     * @param requestTrackingId optional identifier used to correlate log entries for this request
     * @param file The local file to upload.
     * @return {@code true} if the upload succeeded.
     */
    public abstract boolean upload(String requestTrackingId, File file);

    /**
     * Uploads a file from an {@link InputStream} and returns an identifier for the stored object.
     *
     * @param requestTrackingId optional identifier used to correlate log entries for this request
     * @param is The input stream of the file content.
     * @param fileName The name of the file in the storage backend.
     * @return A file identifier.
     */
    public abstract String uploadAndGetId(String requestTrackingId, InputStream is, String fileName);

    /**
     * Reads the content of a file as a string.
     *
     * @param requestTrackingId optional identifier used to correlate log entries for this request
     * @param fileName The path of the file within the bucket.
     * @return The file content as a string.
     * @throws FileNotFoundException if the file does not exist.
     */
    public abstract String readFile(String requestTrackingId, String fileName) throws FileNotFoundException;

    /**
     * Asynchronously deletes a file using a {@link DeleteFileRequest}.
     *
     * @param request The {@link DeleteFileRequest}.
     * @return A {@link CompletableFuture} that completes with {@code true} if the file was successfully deleted.
     */
    public abstract CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link CosStorageRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    public static Builder<?> builder() {
        return ServiceLoader.load(CosStorageRestClientBuilderFactory.class).findFirst()
            .<Builder<?>>map(Supplier::get)
            .orElseGet(DefaultRestClient::builder);
    }

    /**
     * Builder abstract class for constructing {@link CosStorageRestClient} instances with configurable parameters.
     *
     * @param <B> the concrete builder type
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<B extends Builder<B>> {
        String cosUrl;
        String bucket;
        Authenticator authenticator;
        Authenticator cosAuthenticator;
        HttpClient httpClient;
        Duration timeout;
        boolean logRequests;
        boolean logResponses;
        HttpRequestLogger requestLogger;
        Level requestLogLevel = Level.INFO;
        HttpResponseLogger responseLogger;
        Level responseLogLevel = Level.INFO;
        boolean verifySsl = true;

        public abstract CosStorageRestClient build();

        /**
         * Specifies the COS base URL.
         *
         * @param cosUrl The base COS URL as a string.
         */
        public B cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return (B) this;
        }

        /**
         * Specifies the COS bucket name.
         *
         * @param bucket The COS bucket name.
         */
        public B bucket(String bucket) {
            this.bucket = bucket;
            return (B) this;
        }

        /**
         * Sets the {@link Authenticator} used to authenticate requests.
         *
         * @param authenticator The {@link Authenticator} instance.
         */
        public B authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return (B) this;
        }

        /**
         * Specifies a custom authenticator for COS operations.
         *
         * @param cosAuthenticator The {@link Authenticator} to use for COS operations.
         */
        public B cosAuthenticator(Authenticator cosAuthenticator) {
            this.cosAuthenticator = cosAuthenticator;
            return (B) this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for HTTP communication.
         *
         * @param httpClient The custom {@link HttpClient} to use.
         */
        public B httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return (B) this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout The {@link Duration} timeout.
         */
        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }

        /**
         * Enables or disables logging of the request payload.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise.
         */
        public B logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return (B) this;
        }

        /**
         * Enables or disables logging of the response payload.
         *
         * @param logResponses {@code true} to log the response, {@code false} otherwise.
         */
        public B logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return (B) this;
        }

        /**
         * Enables logging of the request payload through a custom logger, fired at the given level.
         *
         * @param requestLogger The custom request logger.
         * @param requestLogLevel The level at which the logger is fired.
         */
        public B requestLogger(HttpRequestLogger requestLogger, Level requestLogLevel) {
            this.requestLogger = requestLogger;
            this.requestLogLevel = requestLogLevel;
            return (B) this;
        }

        /**
         * Enables logging of the response payload through a custom logger, fired at the given level.
         *
         * @param responseLogger The custom response logger.
         * @param responseLogLevel The level at which the logger is fired.
         */
        public B responseLogger(HttpResponseLogger responseLogger, Level responseLogLevel) {
            this.responseLogger = responseLogger;
            this.responseLogLevel = responseLogLevel;
            return (B) this;
        }

        /**
         * Sets whether SSL/TLS certificate verification should be performed.
         *
         * @param verifySsl {@code true} to enable certificate verification, {@code false} to accept all certificates.
         */
        public B verifySsl(boolean verifySsl) {
            this.verifySsl = verifySsl;
            return (B) this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     */
    public interface CosStorageRestClientBuilderFactory extends Supplier<CosStorageRestClient.Builder<?>> {}
}
