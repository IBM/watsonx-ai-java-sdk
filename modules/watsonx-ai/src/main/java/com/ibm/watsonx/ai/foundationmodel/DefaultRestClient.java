/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Default implementation of the {@link FoundationModelRestClient} abstract class.
 */
final class DefaultRestClient extends FoundationModelRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public FoundationModelResponse<FoundationModel> getModels(
        Integer start,
        Integer limit,
        String transactionId,
        Boolean techPreview,
        String filters) {

        try {

            var queryParameters = new StringJoiner("&", "", "");
            queryParameters.add("version=" + version);

            if (nonNull(start))
                queryParameters.add("start=" + start);

            if (nonNull(limit))
                queryParameters.add("limit=" + limit);

            if (techPreview)
                queryParameters.add("tech_preview=" + techPreview);

            if (nonNull(filters))
                queryParameters.add("filters=" + URLEncoder.encode(filters.toString(), StandardCharsets.UTF_8));

            var uri =
                URI.create(baseUrl + "/ml/v1/foundation_model_specs?%s".formatted(queryParameters));

            var httpRequest = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET();

            if (nonNull(transactionId))
                httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

            var response = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(response.body(), new TypeToken<FoundationModelResponse<FoundationModel>>() {});

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FoundationModelResponse<FoundationModelTask> getTasks(FoundationModelParameters parameters) {

        parameters = requireNonNullElse(parameters, FoundationModelParameters.builder().build());

        StringJoiner queryParameters = new StringJoiner("&", "", "");
        queryParameters.add("version=" + version);

        if (nonNull(parameters.start()))
            queryParameters.add("start=" + parameters.start());

        if (nonNull(parameters.limit()))
            queryParameters.add("limit=" + parameters.limit());

        var uri =
            URI.create(baseUrl + "/ml/v1/foundation_model_tasks?%s".formatted(queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri).GET();

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

        try {

            var response = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(response.body(), new TypeToken<FoundationModelResponse<FoundationModelTask>>() {});

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
    public final static class Builder extends FoundationModelRestClient.Builder {

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
