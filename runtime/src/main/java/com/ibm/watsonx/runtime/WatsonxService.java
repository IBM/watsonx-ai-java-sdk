package com.ibm.watsonx.runtime;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import com.ibm.watsonx.auth.BearerInterceptor;
import com.ibm.watsonx.core.auth.AuthenticationProvider;
import com.ibm.watsonx.core.http.AsyncHttpClient;
import com.ibm.watsonx.core.http.SyncHttpClient;
import com.ibm.watsonx.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.runtime.chat.ChatService;
import com.ibm.watsonx.runtime.chat.model.ChatParameters;

/**
 * Abstract base class for all watsonx.ai service classes.
 * <p>
 * This class provides common functionality and shared configuration used across various service-specific clients (e.g., {@code ChatService},
 * {@code TextInferenceService}, etc.). Subclasses should extend this class to inherit support for authentication, HTTP communication, logging, and
 * service metadata such as project or model identifiers.
 *
 * @see ChatService
 */
public abstract class WatsonxService {

    protected final URI url;
    protected final String version;
    protected final String projectId;
    protected final String spaceId;
    protected final String modelId;
    protected final Duration timeout;
    protected final AuthenticationProvider authenticationProvider;
    protected final SyncHttpClient syncHttpClient;
    protected final AsyncHttpClient asyncHttpClient;

    public WatsonxService(Builder<?> builder) {
        url = URI.create(requireNonNull(builder.url));
        version = requireNonNullElse(builder.version, "2025-04-23");
        projectId = builder.projectId;
        spaceId = builder.spaceId;
        modelId = builder.modelId;
        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        authenticationProvider = requireNonNull(builder.authenticationProvider);

        if (isNull(projectId) && isNull(spaceId))
            throw new IllegalArgumentException("Either projectId or spaceId must be provided");

        boolean logRequests = requireNonNullElse(builder.logRequests, false);
        boolean logResponses = requireNonNullElse(builder.logResponses, false);

        var httpClient =
            requireNonNullElse(builder.httpClient, HttpClient.newBuilder().connectTimeout(timeout).build());
        var syncHttpClientBuilder = SyncHttpClient.builder().httpClient(httpClient);
        var asyncHttpClientBuilder = AsyncHttpClient.builder().httpClient(httpClient);

        if (logRequests || logResponses) {
            var loggerInterceptor = new LoggerInterceptor(logRequests, logResponses);
            syncHttpClientBuilder.interceptor(loggerInterceptor);
            asyncHttpClientBuilder.interceptor(loggerInterceptor);
        }

        var retryInterceptor = RetryInterceptor.onTokenExpired(1);
        syncHttpClientBuilder.interceptor(retryInterceptor);
        asyncHttpClientBuilder.interceptor(retryInterceptor);

        var bearerInterceptor = new BearerInterceptor(authenticationProvider);
        syncHttpClientBuilder.interceptor(bearerInterceptor);
        asyncHttpClientBuilder.interceptor(bearerInterceptor);

        syncHttpClient = syncHttpClientBuilder.build();
        asyncHttpClient = asyncHttpClientBuilder.build();
    }


    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String url;
        private String version;
        private String projectId;
        private String spaceId;
        private String modelId;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private HttpClient httpClient;
        private AuthenticationProvider authenticationProvider;

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public T url(String url) {
            this.url = url;
            return (T) this;
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public T url(CloudRegion url) {
            return url(url.endpoint());
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
         * Sets the default project id to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * 
         * @param spaceId Project id value
         */
        public T projectId(String projectId) {
            this.projectId = projectId;
            return (T) this;
        }

        /**
         * Sets the default space id to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * 
         * @param spaceId Space id value
         */
        public T spaceId(String spaceId) {
            this.spaceId = spaceId;
            return (T) this;
        }

        /**
         * Sets the default model to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * <p>
         * For a full list of available model ids, see the
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx">link</a>.
         *
         * @param modelId the model identifier to use
         * @return the builder instance
         */
        public T modelId(String modelId) {
            this.modelId = modelId;
            return (T) this;
        }

        /**
         * Enables or disables logging of the request payload sent to the model.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise
         */
        public T logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return (T) this;
        }

        /**
         * Enables or disables logging of the model's response payload.
         *
         * @param logResponses {@code true} to log the response; {@code false} otherwise
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
    }
}
