/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.nonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;

/**
 * Default implementation of the {@link CosStorageRestClient} abstract class.
 */
final class DefaultRestClient extends CosStorageRestClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRestClient.class);
    private final SyncHttpClient syncCosHttpClient;
    private final AsyncHttpClient asyncCosHttpClient;
    private final Path uploadDirectory;

    DefaultRestClient(Builder builder) {
        this(builder, null);
    }

    DefaultRestClient(Builder builder, Path uploadDirectory) {
        super(builder);
        this.uploadDirectory = uploadDirectory;

        var logMode = LogMode.of(logRequests, logResponses);

        syncCosHttpClient = HttpClientFactory.createSync(
            cosAuthenticator, httpClient, logMode, requestLogger,
            requestLogLevel, responseLogger, responseLogLevel
        );

        asyncCosHttpClient = HttpClientFactory.createAsync(
            cosAuthenticator, httpClient, logMode, requestLogger,
            requestLogLevel, responseLogger, responseLogLevel
        );
    }

    @Override
    public boolean upload(String requestTrackingId, InputStream is, String fileName) {
        try {

            var temporaryFile = nonNull(uploadDirectory)
                ? Files.createTempFile(uploadDirectory, "watsonx-ai-cos-upload-", ".tmp")
                : Files.createTempFile("watsonx-ai-cos-upload-", ".tmp");

            Throwable failure = null;

            try {

                try (is; var output = Files.newOutputStream(temporaryFile)) {
                    is.transferTo(output);
                }
                return upload(requestTrackingId, temporaryFile, fileName);

            } catch (IOException | RuntimeException | Error e) {

                failure = e;
                throw e;

            } finally {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    if (nonNull(failure))
                        failure.addSuppressed(e);
                    else
                        logger.warn("Failed to delete COS upload temporary file {}", temporaryFile, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean upload(String requestTrackingId, File file) {
        return upload(requestTrackingId, file.toPath(), file.getName());
    }

    private boolean upload(String requestTrackingId, Path file, String fileName) {
        try {

            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucket, encodedFileName));
            var builder = HttpRequest.newBuilder()
                .uri(URI.create(uri.toASCIIString()))
                .timeout(timeout)
                .PUT(BodyPublishers.ofFile(file));

            if (nonNull(requestTrackingId))
                builder.header(REQUEST_ID_HEADER, requestTrackingId);

            var response = syncCosHttpClient.send(builder.build(), BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String uploadAndGetId(String requestTrackingId, InputStream is, String fileName) {
        upload(requestTrackingId, is, fileName);
        return null;
    }

    @Override
    public String readFile(String requestTrackingId, String fileName) throws FileNotFoundException {
        try {

            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucket, encodedFileName));
            var builder = HttpRequest.newBuilder(uri).timeout(timeout).GET();

            if (nonNull(requestTrackingId))
                builder.header(REQUEST_ID_HEADER, requestTrackingId);

            return syncCosHttpClient.send(builder.build(), BodyHandlers.ofString()).body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request) {
        return deleteAsync(request.fileName(), request.requestTrackingId());
    }

    private CompletableFuture<Boolean> deleteAsync(String fileName, String requestTrackingId) {
        try {

            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucket, encodedFileName));
            var builder = HttpRequest.newBuilder(uri).timeout(timeout).DELETE();

            if (nonNull(requestTrackingId))
                builder.header(REQUEST_ID_HEADER, requestTrackingId);

            return asyncCosHttpClient.send(builder.build(), BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 204);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    public static final class Builder extends CosStorageRestClient.Builder<Builder> {

        /**
         * Builds a {@link DefaultRestClient} instance using the configured parameters.
         *
         * @return a new instance of {@link DefaultRestClient}
         */
        @Override
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
