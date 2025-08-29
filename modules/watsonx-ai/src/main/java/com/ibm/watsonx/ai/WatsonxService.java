/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;
import com.ibm.watsonx.ai.tokenization.TokenizationService;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * This class provides common functionality and shared configuration used across various service-specific clients (e.g., {@code ChatService},
 * {@code TextGenerationService}, etc.). Subclasses should extend this class to inherit support for authentication, HTTP communication, and so on.
 *
 * @see ChatService
 * @see TextGenerationService
 * @see DeploymentService
 * @see EmbeddingService
 * @see RerankService
 * @see TokenizationService
 * @see TextExtractionService
 * @see TimeSeriesService
 * @see FoundationModelService
 * @see ToolService
 */
public abstract class WatsonxService {

    protected static final String ML_API_PATH = "/ml/v1";
    protected static final String ML_API_TEXT_PATH = ML_API_PATH.concat("/text");
    protected static final String API_VERSION = "2025-04-23";
    protected static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";

    protected final URI url;
    protected final String version;
    protected final Duration timeout;
    protected final boolean logRequests, logResponses;
    protected final SyncHttpClient syncHttpClient;
    protected final AsyncHttpClient asyncHttpClient;
    protected final Executor computationExecutor;

    protected WatsonxService(Builder<?> builder) {
        url = requireNonNull(builder.url, "The url must be provided");
        version = requireNonNullElse(builder.version, API_VERSION);
        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));

        logRequests = requireNonNullElse(builder.logRequests, false);
        logResponses = requireNonNullElse(builder.logResponses, false);

        var httpClient = HttpClientProvider.httpClient();
        var syncHttpClientBuilder = SyncHttpClient.builder().httpClient(httpClient);
        var asyncHttpClientBuilder = AsyncHttpClient.builder().httpClient(httpClient);

        syncHttpClientBuilder.interceptor(RetryInterceptor.ON_TOKEN_EXPIRED);
        asyncHttpClientBuilder.interceptor(RetryInterceptor.ON_TOKEN_EXPIRED);

        if (nonNull(builder.authenticationProvider)) {
            var bearerInterceptor = new BearerInterceptor(builder.authenticationProvider);
            syncHttpClientBuilder.interceptor(bearerInterceptor);
            asyncHttpClientBuilder.interceptor(bearerInterceptor);
        }

        syncHttpClientBuilder.interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES);
        asyncHttpClientBuilder.interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES);

        if (logRequests || logResponses) {
            syncHttpClientBuilder.interceptor(new LoggerInterceptor(logRequests, logResponses));
            asyncHttpClientBuilder.interceptor(new LoggerInterceptor(logRequests, logResponses));
        }

        syncHttpClient = syncHttpClientBuilder.build();
        asyncHttpClient = asyncHttpClientBuilder.build();
        computationExecutor = ExecutorProvider.cpuExecutor();
    }

    @SuppressWarnings("unchecked")
    protected static abstract class Builder<T extends Builder<T>> {
        private URI url;
        private String version;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private AuthenticationProvider authenticationProvider;

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
         * Sets the {@link AuthenticationProvider} used for authenticating requests.
         *
         * @param authenticationProvider {@link AuthenticationProvider} instance
         */
        public T authenticationProvider(AuthenticationProvider authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
            return (T) this;
        }

        /**
         * Returns the authentication provider.
         *
         * @return the configured {@link AuthenticationProvider}, or {@code null} if none has been set.
         */
        public AuthenticationProvider getAuthenticationProvider() {
            return authenticationProvider;
        }
    }

    /**
     * Abstract base class for watsonx services that require a project or space context.
     */
    public static abstract class ProjectService extends WatsonxService {
        protected final String projectId;
        protected final String spaceId;

        protected ProjectService(Builder<?> builder) {
            super(builder);
            projectId = builder.projectId;
            spaceId = builder.spaceId;

            if (isNull(projectId) && isNull(spaceId))
                throw new NullPointerException("Either projectId or spaceId must be provided");
        }

        @SuppressWarnings("unchecked")
        protected static abstract class Builder<T extends Builder<T>> extends WatsonxService.Builder<T> {
            private String projectId;
            private String spaceId;

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
        }
    }

    /**
     * Abstract base class for watsonx services that operate on a specific model.
     */
    public static abstract class ModelService extends ProjectService {

        protected final String modelId;

        protected ModelService(Builder<?> builder) {
            super(builder);
            modelId = requireNonNull(builder.modelId, "The modelId must be provided");
        }

        @SuppressWarnings("unchecked")
        protected static abstract class Builder<T extends Builder<T>> extends ProjectService.Builder<T> {
            private String modelId;

            /**
             * Sets the default model.
             *
             * @param modelId the model identifier to use
             */
            public T modelId(String modelId) {
                this.modelId = modelId;
                return (T) this;
            }
        }
    }
}
