/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.MultipartBody;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link FileRestClient} abstract class.
 */
final class DefaultRestClient extends FileRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public FileData upload(FileUploadRequest request) {

        var multiPartRequest = MultipartBody.builder()
            .addPart("purpose", request.purpose().value())
            .addInputStream("file", request.fileName(), request.inputStream())
            .build();

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/files?version=%s".formatted(version)))
            .header("Content-Type", multiPartRequest.contentType())
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        httpRequest
            .POST(BodyPublishers.ofByteArray(multiPartRequest.body()))
            .timeout(timeout);

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), FileData.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileListResponse list(FileListRequest request) {

        var url = new StringBuilder(baseUrl + "/ml/v1/files?version=%s".formatted(version));

        if (nonNull(request.after()))
            url.append("&after=").append(URLEncoder.encode(request.after(), StandardCharsets.UTF_8));

        if (nonNull(request.limit()))
            url.append("&limit=").append(request.limit());

        if (nonNull(request.order()))
            url.append("&order=").append(request.order());

        if (nonNull(request.purpose()))
            url.append("&purpose=").append(request.purpose());

        var httpRequest = HttpRequest.newBuilder(URI.create(url.toString()))
            .GET()
            .timeout(timeout)
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), FileListResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String retrieve(FileRetrieveRequest request) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/files/%s/content?version=%s".formatted(request.fileId(), version)))
            .GET()
            .timeout(timeout)
            .header("Accept", "application/jsonl");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return httpResponse.body();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a new {@link Builder} instance.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    public final static class Builder extends FileRestClient.Builder {

        private Builder() {}

        /**
         * Builds a {@link DefaultRestClient} instance using the configured parameters.
         *
         * @return a new instance of {@link DefaultRestClient}
         */
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
