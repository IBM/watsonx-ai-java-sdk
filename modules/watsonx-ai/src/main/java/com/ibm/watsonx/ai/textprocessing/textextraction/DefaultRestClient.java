/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static com.ibm.watsonx.ai.core.HttpUtils.encodePathSegment;
import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.internal.CancellableFutures;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.storage.cos.internal.CosUploadClients;

/**
 * Default implementation of the {@link TextExtractionRestClient} abstract class.
 */
final class DefaultRestClient extends TextExtractionRestClient {
    private final CosUploadClients cosUploadClients;
    private final SyncHttpClient syncHttpClient;
    private final SyncHttpClient syncCosHttpClient;
    private final AsyncHttpClient asyncCosHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        cosUploadClients = new CosUploadClients(cosUrl, authenticator, cosAuthenticator, httpClient, timeout, logRequests, logResponses,
            requestLogger, requestLogLevel, responseLogger, responseLogLevel);
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
        syncCosHttpClient = HttpClientFactory.createSync(cosAuthenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
        asyncCosHttpClient = HttpClientFactory.createAsync(cosAuthenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
    }

    @Override
    public boolean deleteFile(DeleteFileRequest request) throws FileNotFoundException {
        try {
            return deleteFileAsync(request).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WatsonxException ex) {
                Exception mapped = mapIfCosFileNotFound(ex);
                if (mapped instanceof FileNotFoundException fnf)
                    throw fnf;
                else
                    throw ex;
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request) {
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

            var httpFuture = asyncCosHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return CancellableFutures.thenApply(httpFuture, r -> r.statusCode() == 204);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readFile(ReadFileRequest request) throws FileNotFoundException {
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

            return syncCosHttpClient.send(httpRequest.build(), BodyHandlers.ofString()).body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (WatsonxException e) {
            Exception mapped = mapIfCosFileNotFound(e);
            if (mapped instanceof FileNotFoundException fnf)
                throw fnf;
            else
                throw e;
        }
    }

    @Override
    public boolean upload(UploadRequest request) {
        return cosUploadClients.get(request.bucketName())
            .upload(request.requestTrackingId(), request.is(), request.fileName());
    }

    @Override
    public boolean deleteExtraction(DeleteExtractionRequest request) {

        var id = request.extractionId();
        var parameters = request.parameters();

        var projectId = parameters.projectId();
        var spaceId = parameters.spaceId();

        var queryParameters = parameters.hardDelete()
            .map(hardDelete -> getQueryParameters(projectId, spaceId).concat("&hard_delete=" + hardDelete))
            .orElse(getQueryParameters(projectId, spaceId));

        var uri = URI.create(baseUrl + "/ml/v1/text/extractions/%s?%s".formatted(encodePathSegment(id), queryParameters));
        var httpRequest = HttpRequest.newBuilder(uri).timeout(timeout).DELETE();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

        try {

            syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WatsonxException e) {
            if (e.statusCode() == 404)
                return false;
            throw e;
        }
    }

    @Override
    public TextExtractionResponse fetchExtractionDetails(FetchExtractionDetailsRequest request) {
        return fetchExtractionDetails(request, timeout);
    }

    @Override
    public TextExtractionResponse fetchExtractionDetails(FetchExtractionDetailsRequest request, Duration requestTimeout) {

        var id = request.extractionId();
        var parameters = request.parameters();
        var projectId = parameters.projectId();
        var spaceId = parameters.spaceId();

        var queryParameters = getQueryParameters(projectId, spaceId);
        var uri = URI.create(baseUrl + "/ml/v1/text/extractions/%s?%s".formatted(encodePathSegment(id), queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .timeout(requestTimeout.compareTo(timeout) < 0 ? requestTimeout : timeout)
            .GET();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString(), requestTimeout);
            return fromJson(httpResponse.body(), TextExtractionResponse.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
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

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), TextExtractionResponse.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //
    // Generates query parameters for API requests based on provided project_id or space_id.
    //
    private String getQueryParameters(String projectId, String spaceId) {
        if (nonNull(projectId))
            return "version=%s&project_id=%s".formatted(version, URLEncoder.encode(projectId, StandardCharsets.UTF_8));
        else
            return "version=%s&space_id=%s".formatted(version, URLEncoder.encode(spaceId, StandardCharsets.UTF_8));
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
