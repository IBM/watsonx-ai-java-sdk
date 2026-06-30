/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link MergeSchemaRestClient} abstract class.
 */
final class DefaultRestClient extends MergeSchemaRestClient {
    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public boolean deleteRequest(DeleteRequest request) {
        var id = request.requestId();
        var parameters = request.parameters();

        var projectId = parameters.projectId();
        var spaceId = parameters.spaceId();

        var queryParameters = parameters.hardDelete()
            .map(hardDelete -> getQueryParameters(projectId, spaceId).concat("&hard_delete=" + hardDelete))
            .orElse(getQueryParameters(projectId, spaceId));

        var uri = URI.create(baseUrl + "/ml/v1/text/schemas/merge/%s?%s".formatted(id, queryParameters));
        var httpRequest = HttpRequest.newBuilder(uri).timeout(timeout).DELETE();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

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
    public MergeSchemaResponse fetchRequestDetails(MergeFetchDetailsRequest request) {
        var id = request.requestId();
        var parameters = request.parameters();
        var projectId = parameters.projectId();
        var spaceId = parameters.spaceId();

        var queryParameters = getQueryParameters(projectId, spaceId);
        var uri = URI.create(baseUrl + "/ml/v1/text/schemas/merge/%s?%s".formatted(id, queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .timeout(timeout)
            .GET();

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), MergeSchemaResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public MergeSchemaResponse startRequest(StartMergeSchemaRequest request) {
        var httpRequest =
            HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/schemas/merge?version=%s".formatted(version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(request.mergeSchemaRequest())))
                .timeout(timeout);

        if (nonNull(request.requestTrackingId()))
            httpRequest.header(REQUEST_ID_HEADER, request.requestTrackingId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), MergeSchemaResponse.class);

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
    public static class Builder extends MergeSchemaRestClient.Builder {

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
