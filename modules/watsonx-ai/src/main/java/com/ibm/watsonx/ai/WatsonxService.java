/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;
import com.ibm.watsonx.ai.tokenization.TokenizationService;

/**
 * Abstract base class for all watsonx.ai service classes.
 * <p>
 * This class provides common functionality and shared configuration used across various service-specific clients (e.g., {@code ChatService},
 * {@code TextGenerationService}, etc.). Subclasses should extend this class to inherit support for authentication, HTTP communication, logging, and
 * service metadata such as project or model identifiers.
 *
 * @see ChatService
 * @see TextGenerationService
 * @see EmbeddingService
 * @see RerankService
 * @see TokenizationService
 * @see TextExtractionService
 * @see TimeSeriesService
 */
public abstract class WatsonxService {

    public static final String ML_API_PATH = "/ml/v1";
    public static final String ML_API_TEXT_PATH = ML_API_PATH.concat("/text");
    public static final String API_VERSION = "2025-04-23";

    protected final URI url;
    protected final String version;
    protected final String projectId;
    protected final String spaceId;
    protected final String modelId;
    protected final Duration timeout;
    protected final boolean logResponses;
    protected final AuthenticationProvider authenticationProvider;
    protected final FoundationModelService foundationModelService;
    protected final SyncHttpClient syncHttpClient;
    protected final AsyncHttpClient asyncHttpClient;

    protected WatsonxService(Builder<?> builder) {
        url = requireNonNull(builder.url, "The url must be provided");
        version = requireNonNullElse(builder.version, API_VERSION);
        projectId = builder.projectId;
        spaceId = builder.spaceId;

        if (!TextExtractionService.class.isInstance(this))
            modelId = requireNonNull(builder.modelId, "The modelId must be provided");
        else
            modelId = null;

        if (isNull(projectId) && isNull(spaceId))
            throw new NullPointerException("Either projectId or spaceId must be provided");

        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        authenticationProvider = requireNonNull(builder.authenticationProvider, "The authentication provider is mandatory");

        boolean logRequests = requireNonNullElse(builder.logRequests, false);
        logResponses = requireNonNullElse(builder.logResponses, false);

        var httpClient = requireNonNullElse(builder.httpClient, HttpClient.newBuilder().build());
        var syncHttpClientBuilder = SyncHttpClient.builder().httpClient(httpClient);
        var asyncHttpClientBuilder = AsyncHttpClient.builder().httpClient(httpClient);

        var retryInterceptor = RetryInterceptor.onTokenExpired(1);
        syncHttpClientBuilder.interceptor(retryInterceptor);
        asyncHttpClientBuilder.interceptor(retryInterceptor);

        var bearerInterceptor = new BearerInterceptor(authenticationProvider);
        syncHttpClientBuilder.interceptor(bearerInterceptor);
        asyncHttpClientBuilder.interceptor(bearerInterceptor);

        if (logRequests || logResponses) {
            syncHttpClientBuilder.interceptor(new LoggerInterceptor(logRequests, logResponses));
            asyncHttpClientBuilder.interceptor(new LoggerInterceptor(logRequests, false));
        }

        syncHttpClient = syncHttpClientBuilder.build();
        asyncHttpClient = asyncHttpClientBuilder.build();

        foundationModelService = requireNonNullElse(
            builder.foundationModelService, FoundationModelService.builder()
                .url(url)
                .techPreview(true)
                .httpClient(httpClient)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build()
        );
    }

    /**
     * Retrieves model details.
     *
     * @return Details of the the model.
     */
    public FoundationModel getModelDetails() {
        return foundationModelService.getModelDetails(modelId)
            .orElseThrow(() -> new RuntimeException("The model with id \"%s\" doesn't exist".formatted(modelId)));
    }

    @SuppressWarnings("unchecked")
    protected static abstract class Builder<T extends Builder<T>> {
        private URI url;
        private String version;
        private String projectId;
        private String spaceId;
        private String modelId;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private HttpClient httpClient;
        private AuthenticationProvider authenticationProvider;
        private FoundationModelService foundationModelService;

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public T url(URI url) {
            this.url = url;
            return (T) this;
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public T url(String url) {
            return url(URI.create(url));
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public T url(CloudRegion url) {
            return url(URI.create(url.getMlEndpoint()));
        }

        /**
         * The version date for the API of the form YYYY-MM-DD.
         *
         * @param version Version date
         */
        public T version(String version) {
            this.version = version;
            return (T) this;
        }

        /**
         * Sets the default project id.
         *
         * @param projectId Project id value
         */
        public T projectId(String projectId) {
            this.projectId = projectId;
            return (T) this;
        }

        /**
         * Sets the default space id.
         *
         * @param spaceId Space id value
         */
        public T spaceId(String spaceId) {
            this.spaceId = spaceId;
            return (T) this;
        }

        /**
         * Sets the default model.
         *
         * @param modelId the model identifier to use
         */
        public T modelId(String modelId) {
            this.modelId = modelId;
            return (T) this;
        }

        /**
         * Enables or disables logging of the request payload.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise
         */
        public T logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return (T) this;
        }

        /**
         * Enables or disables logging of the response payload.
         *
         * @param logResponses {@code true} to log the response, {@code false} otherwise
         */
        public T logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return (T) this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout {@link Duration} timeout.
         */
        public T timeout(Duration timeout) {
            this.timeout = timeout;
            return (T) this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for making requests.
         *
         * @param httpClient the {@code HttpClient} instance to use
         */
        public T httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return (T) this;
        }

        /**
         * Sets the {@link AuthenticationProvider} used for authenticating requests.
         *
         * @param authenticationProvider {@link AuthenticationProvider} instance
         */
        public T authenticationProvider(AuthenticationProvider authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
            return (T) this;
        }

        /**
         * Sets the {@link FoundationModelService} used for authenticating requests.
         *
         * @param authenticationProvider {@link FoundationModelService} instance
         */
        public T foundationModelService(FoundationModelService foundationModelService) {
            this.foundationModelService = foundationModelService;
            return (T) this;
        }
    }
}
