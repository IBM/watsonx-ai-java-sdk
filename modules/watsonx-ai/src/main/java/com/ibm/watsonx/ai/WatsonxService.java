/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.time.Duration;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
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

    protected static final String API_VERSION = "2025-10-01";
    protected static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";
    protected static final Duration TIME_OUT = Duration.ofSeconds(60);

    protected final String baseUrl;
    protected final String version;
    protected final Duration timeout;
    protected final boolean logRequests, logResponses;

    protected WatsonxService(Builder<?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl must be provided");
        version = requireNonNullElse(builder.version, API_VERSION);
        timeout = requireNonNullElse(builder.timeout, TIME_OUT);
        logRequests = requireNonNullElse(builder.logRequests, false);
        logResponses = requireNonNullElse(builder.logResponses, false);
    }

    @SuppressWarnings("unchecked")
    protected static abstract class Builder<T extends Builder<T>> {
        private String baseUrl;
        private String version;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private AuthenticationProvider authenticationProvider;

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param baseUrl the endpoint URL as a string
         */
        public T baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return (T) this;
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param baseUrl the endpoint URL as a string
         */
        public T baseUrl(URI baseUrl) {
            return baseUrl(baseUrl.toString());
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param baseUrl the endpoint URL as a string
         */
        public T baseUrl(CloudRegion baseUrl) {
            return baseUrl(baseUrl.getMlEndpoint());
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
         * Sets an {@link IAMAuthenticator}-based {@link AuthenticationProvider}, initialized from the provided IBM Cloud API key.
         * <p>
         * For alternative authentication mechanisms, use {@link #authenticationProvider(AuthenticationProvider)}.
         *
         * @param apiKey IBM Cloud API key
         */
        public T apiKey(String apiKey) {
            requireNonNull(apiKey, "The apiKey must be provided");
            authenticationProvider = IAMAuthenticator.builder().apiKey(apiKey).build();
            return (T) this;
        }

        /**
         * Sets the {@link AuthenticationProvider} used to authenticate requests.
         * <p>
         * Use this method to specify a custom or non-IAM implementation.
         * <p>
         * For IBM Cloud IAM authentication, {@link #apiKey(String)} provides a simpler alternative.
         *
         * @param authenticationProvider non-null {@link AuthenticationProvider} instance
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
