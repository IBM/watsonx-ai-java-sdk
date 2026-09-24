/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;

/**
 * Default implementation of the {@link CosStorageRestClient} abstract class.
 */
final class DefaultRestClient extends CosStorageRestClient {

    private final SyncHttpClient syncCosHttpClient;
    private final AsyncHttpClient asyncCosHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);

        requireNonNull(cosUrl, "cosUrl cannot be null");
        requireNonNull(bucket, "bucket cannot be null");
        requireNonNull(timeout, "timeout cannot be null");

        var cosAuth = requireNonNullElse(builder.cosAuthenticator, requireNonNull(builder.authenticator, "authenticator cannot be null"));
        var logMode = LogMode.of(builder.logRequests, builder.logResponses);
        var httpClient = requireNonNullElse(builder.httpClient, HttpClientProvider.httpClient(builder.verifySsl));

        syncCosHttpClient = HttpClientFactory.createSync(
            cosAuth, httpClient, logMode, builder.requestLogger,
            builder.requestLogLevel, builder.responseLogger, builder.responseLogLevel
        );

        asyncCosHttpClient = HttpClientFactory.createAsync(
            cosAuth, httpClient, logMode, builder.requestLogger,
            builder.requestLogLevel, builder.responseLogger, builder.responseLogLevel
        );
    }

    @Override
    public boolean upload(String requestTrackingId, InputStream is, String fileName) {
        try {
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucket, encodedFileName));
            var builder = HttpRequest.newBuilder()
                .uri(URI.create(uri.toASCIIString()))
                .timeout(timeout)
                .PUT(BodyPublishers.ofInputStream(() -> is));

            if (nonNull(requestTrackingId))
                builder.header(REQUEST_ID_HEADER, requestTrackingId);

            var response = syncCosHttpClient.send(builder.build(), BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean upload(String requestTrackingId, File file) {
        try (var is = new FileInputStream(file)) {
            return upload(requestTrackingId, is, file.getName());
        } catch (IOException e) {
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
        } catch (IOException | InterruptedException | URISyntaxException e) {
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
