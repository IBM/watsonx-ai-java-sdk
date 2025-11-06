/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.transcription;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.HttpRequestMultipartBody;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link TranscriptionRestClient} abstract class.
 */
final class DefaultRestClient extends TranscriptionRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public TranscriptionResult transcribe(TranscriptionRequest request) {

        var multiPartRequest = HttpRequestMultipartBody.builder()
            .addPart("model", request.modelId())
            .addPart("language", request.language())
            .addPart("project_id", request.projectId())
            .addPart("space_id", request.spaceId())
            .addInputStream("file", request.inputStream())
            .build();

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/audio/transcriptions?version=%s".formatted(version)))
            .header("Content-Type", multiPartRequest.contentType())
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofByteArray(multiPartRequest.body()))
            .timeout(timeout);

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TranscriptionResult.class);

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
    public final static class Builder extends TranscriptionRestClient.Builder {

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
