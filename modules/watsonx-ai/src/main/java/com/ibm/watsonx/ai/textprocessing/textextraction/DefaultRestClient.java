/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;

/**
 * Default implementation of the {@link TextExtractionRestClient} abstract class.
 */
final class DefaultRestClient extends TextExtractionRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticationProvider, "authenticationProvider is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticationProvider, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticationProvider, LogMode.of(logRequests, logResponses));
    }

    @Override
    public boolean deleteFile(DeleteFileRequest request) {
        try {
            return asyncDeleteFile(request).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WatsonxException ex)
                throw ex;
            else
                throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteFile(DeleteFileRequest request) {
        try {

            var fileName = request.fileName();
            var bucketName = request.bucketName();
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucketName, encodedFileName));

            var httpRequest = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .DELETE();

            if (nonNull(request.requestTrackingId()))
                httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

            return asyncHttpClient.send(httpRequest.build(), BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 204);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readFile(ReadFileRequest request) {
        try {

            var fileName = request.fileName();
            var bucketName = request.bucketName();
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucketName, encodedFileName));

            var httpRequest = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET();

            if (nonNull(request.requestTrackingId()))
                httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

            return syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString()).body();

        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean upload(UploadRequest request) {
        try {

            var fileName = request.fileName();
            var bucketName = request.bucketName();
            var is = request.is();
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucketName, encodedFileName));

            var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(uri.toASCIIString()))
                .timeout(timeout)
                .PUT(BodyPublishers.ofInputStream(() -> is));

            if (nonNull(request.requestTrackingId()))
                httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

            var response = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteExtraction(DeleteExtractionRequest request) {

        var id = request.extractionId();
        var parameters = request.parameters();

        var projectId = parameters.getProjectId();
        var spaceId = parameters.getSpaceId();

        var queryParameters = parameters.getHardDelete()
            .map(nullable -> getQueryParameters(projectId, spaceId).concat("&hard_delete=true"))
            .orElse(getQueryParameters(projectId, spaceId));

        var uri = URI.create(baseUrl + "/ml/v1/text/extractions/%s?%s".formatted(id, queryParameters));
        var httpRequest = HttpRequest.newBuilder(uri).timeout(timeout).DELETE();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return true;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (WatsonxException e) {
            if (e.statusCode() == 404)
                return false;
            throw e;
        }
    }

    @Override
    public TextExtractionResponse fetchExtractionDetails(FetchExtractionDetailsRequest request) {

        var id = request.extractionId();
        var parameters = request.parameters();
        var projectId = parameters.getProjectId();
        var spaceId = parameters.getSpaceId();

        var queryParameters = getQueryParameters(projectId, spaceId);
        var uri = URI.create(baseUrl + "/ml/v1/text/extractions/%s?%s".formatted(id, queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .timeout(timeout)
            .GET();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextExtractionResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TextExtractionResponse startExtraction(StartExtractionRequest request) {

        var httpRequest =
            HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/extractions?version=%s".formatted(version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(request.textExtractionRequest())))
                .timeout(timeout);

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextExtractionResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //
    // Generates query parameters for API requests based on provided project_id or space_id.
    //
    private String getQueryParameters(String projectId, String spaceId) {
        if (nonNull(projectId))
            return "version=%s&project_id=%s".formatted(version, URLEncoder.encode(projectId, Charset.defaultCharset()));
        else
            return "version=%s&space_id=%s".formatted(version, URLEncoder.encode(spaceId, Charset.defaultCharset()));
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
    public static class Builder extends TextExtractionRestClient.Builder {

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
