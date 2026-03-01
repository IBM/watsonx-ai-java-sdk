/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.file.FileService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;
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
 * @see TextClassificationService
 * @see TimeSeriesService
 * @see FoundationModelService
 * @see ToolService
 * @see DetectionService
 * @see FileService
 * @see BatchService
 */
public abstract class WatsonxService {

    protected static final String API_VERSION = "2026-01-07";
    protected static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";
    protected static final Duration TIME_OUT = Duration.ofSeconds(60);

    protected final String baseUrl;
    protected final String version;
    protected final Duration timeout;
    protected final boolean logRequests, logResponses;
    protected final HttpClient httpClient;
    protected final boolean verifySsl;

    // Required by CDI for proxy / bean instantiation
    protected WatsonxService() {
        baseUrl = null;
        version = null;
        timeout = null;
        logRequests = false;
        logResponses = false;
        httpClient = null;
        verifySsl = true;
    }

    protected WatsonxService(Builder<?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl must be provided");
        version = requireNonNullElse(builder.version, API_VERSION);
        timeout = requireNonNullElse(builder.timeout, TIME_OUT);
        logRequests = requireNonNullElse(builder.logRequests, false);
        logResponses = requireNonNullElse(builder.logResponses, false);
        httpClient = builder.httpClient;
        verifySsl = builder.verifySsl;
    }

    /**
     * Abstract builder class for constructing {@link WatsonxService} instances.
     *
     * @param <T> the type of the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    protected static abstract class Builder<T extends Builder<T>> {
        private String baseUrl;
        private String version;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private Authenticator authenticator;
        private HttpClient httpClient;
        private boolean verifySsl = true;

        /**
         * Sets the endpoint URL to which requests will be sent.
         *
         * @param baseUrl the endpoint URL as a string
         */
        public T baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return (T) this;
        }

        /**
         * Sets the endpoint URL to which requests will be sent.
         *
         * @param baseUrl the endpoint URL as a URI
         */
        public T baseUrl(URI baseUrl) {
            return baseUrl(baseUrl.toString());
        }

        /**
         * Sets the endpoint URL to which requests will be sent.
         *
         * @param baseUrl the cloud region containing the endpoint URL
         */
        public T baseUrl(CloudRegion baseUrl) {
            return baseUrl(baseUrl.mlEndpoint());
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
         * Sets an {@link IBMCloudAuthenticator}-based {@link Authenticator}, initialized from the provided IBM Cloud API key.
         * <p>
         * For alternative authentication mechanisms, use {@link #authenticator(Authenticator)}.
         *
         * @param apiKey IBM Cloud API key
         */
        public T apiKey(String apiKey) {
            requireNonNull(apiKey, "The apiKey must be provided");
            authenticator = IBMCloudAuthenticator.builder().httpClient(httpClient).apiKey(apiKey).build();
            return (T) this;
        }

        /**
         * Sets the {@link Authenticator} used to authenticate requests.
         * <p>
         * Use this method to specify a custom or non-IAM implementation.
         * <p>
         * For IBM Cloud IAM authentication, {@link #apiKey(String)} provides a simpler alternative.
         *
         * @param authenticator non-null {@link Authenticator} instance
         */
        public T authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return (T) this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for HTTP communication.
         * <p>
         * This allows customization of the underlying HTTP client, such as configuring a custom {@link javax.net.ssl.SSLContext} for TLS/SSL
         * settings, proxy configuration, connection timeouts, or other HTTP client properties. If not specified, a default {@link HttpClient} will be
         * created automatically.
         *
         * @param httpClient the custom {@link HttpClient} to use
         */
        public T httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return (T) this;
        }

        /**
         * Sets whether SSL/TLS certificate verification should be performed.
         * <p>
         * When set to {@code true} (default), the client validates server certificates against trusted Certificate Authorities. When set to
         * {@code false}, all certificates are accepted without validation, including self-signed certificates.
         * <p>
         * This setting is ignored if a custom {@link HttpClient} is provided via {@link #httpClient(HttpClient)}.
         *
         * @param verifySsl {@code true} to enable certificate verification, {@code false} to accept all certificates
         */
        public T verifySsl(boolean verifySsl) {
            this.verifySsl = verifySsl;
            return (T) this;
        }

        /**
         * Returns the authenticator.
         *
         * @return the configured {@link Authenticator}
         */
        public Authenticator authenticator() {
            return authenticator;
        }
    }

    /**
     * Abstract base class for watsonx services that require a project or space context.
     */
    public static abstract class ProjectService extends WatsonxService {
        protected record ProjectSpace(String projectId, String spaceId) {}

        protected final String projectId;
        protected final String spaceId;

        // Required by CDI for proxy / bean instantiation
        protected ProjectService() {
            super();
            projectId = null;
            spaceId = null;
        }

        protected ProjectService(Builder<?> builder) {
            super(builder);
            projectId = builder.projectId;
            spaceId = builder.spaceId;

            if (isNull(projectId) && isNull(spaceId))
                throw new NullPointerException("Either projectId or spaceId must be provided");
        }

        /**
         * Resolves the project or space context from the provided parameters or falls back to the service defaults.
         *
         * @param parameters the request parameters containing optional project/space overrides
         * @return a {@link ProjectSpace} record containing the resolved project and space identifiers
         */
        protected ProjectSpace resolveProjectSpace(WatsonxParameters parameters) {
            if (isNull(parameters))
                return new ProjectSpace(projectId, spaceId);

            return nonNull(parameters.projectId()) || nonNull(parameters.spaceId())
                ? new ProjectSpace(parameters.projectId(), parameters.spaceId())
                : new ProjectSpace(projectId, spaceId);
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

        // Required by CDI for proxy / bean instantiation.
        protected ModelService() {
            super();
            modelId = null;
        }

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

    /**
     * Abstract base class for watsonx services that support encryption of inference requests using a key reference from a keys management service.
     */
    public static abstract class CryptoService extends ModelService {
        protected final String crypto;

        // Required by CDI for proxy / bean instantiation.
        protected CryptoService() {
            super();
            crypto = null;
        }

        protected CryptoService(Builder<?> builder) {
            super(builder);
            crypto = builder.crypto;
        }

        @SuppressWarnings("unchecked")
        protected static abstract class Builder<T extends Builder<T>> extends ModelService.Builder<T> {
            private String crypto;

            /**
             * Sets the crypto key reference for encrypting requests.
             * <p>
             * The key reference should be an identifier from a keys management service (e.g., IBM Key Protect).
             *
             * @param crypto the key reference identifier (e.g., CRN format for IBM Key Protect)
             * @see <a href=
             *      "https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-generation.html?context=wx&audience=wdp#inf-encrypt">Encrypting
             *      inference requests</a>
             */
            public T crypto(String crypto) {
                this.crypto = crypto;
                return (T) this;
            }
        }
    }
}
