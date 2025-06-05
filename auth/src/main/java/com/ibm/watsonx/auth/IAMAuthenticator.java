package com.ibm.watsonx.auth;

import static com.ibm.watsonx.core.Json.fromJson;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.ibm.watsonx.core.auth.AuthenticationProvider;
import com.ibm.watsonx.core.http.AsyncHttpClient;
import com.ibm.watsonx.core.http.SyncHttpClient;

/**
 * The {@code IAMAuthenticator} class is an implementation of the {@link AuthenticationProvider} interface, responsible for authenticating with IBM
 * Cloud Identity and Access Management (IAM) using an API key.
 * <p>
 * It manages token acquisition and caching, and handles automatic refresh if the token expires. This authenticator is suitable for use in services
 * that require secure access to IBM Cloud resources.
 * </p>
 *
 * <p>
 * <b>Example usage:</b>
 * </p>
 *
 * <pre>{@code
 * AuthenticationProvider authenticator = IAMAuthenticator.builder()
 *     .apiKey("your-api-key")
 *     .build();
 * }</pre>
 * 
 * <pre>{@code
 * String accessToken = authenticator.getToken();
 * }</pre>
 *
 * <p>
 * You can also configure the IAM endpoint, grant type, and request timeout using the builder:
 * </p>
 *
 * <pre>{@code
 * AuthenticationProvider authenticator = IAMAuthenticator.builder()
 *     .apiKey("your-api-key")
 *     .url(URI.create("https://iam.cloud.ibm.com/identity/token"))
 *     .grantType("urn:ibm:params:oauth:grant-type:apikey")
 *     .timeout(Duration.ofSeconds(15))
 *     .build();
 * }</pre>
 *
 * @see AuthenticationProvider
 * @see IdentityTokenResponse
 */
public final class IAMAuthenticator implements AuthenticationProvider {

    private final Semaphore lock;
    private final URI url;
    private final String apiKey;
    private final String grantType;
    private final Duration timeout;
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;
    private IdentityTokenResponse token;

    /**
     * Constructs an IAMAuthenticator instance using the provided builder.
     *
     * @param builder the builder instance
     */
    public IAMAuthenticator(Builder builder) {
        this.apiKey = encode(requireNonNull(builder.apiKey));
        this.lock = new Semaphore(1);
        this.url = requireNonNullElse(builder.url, URI.create("https://iam.cloud.ibm.com/identity/token"));
        this.grantType = encode(requireNonNullElse(builder.grantType, "urn:ibm:params:oauth:grant-type:apikey"));
        this.timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        
        var httpClient = requireNonNullElse(builder.httpClient, HttpClient.newBuilder().build());
        this.syncHttpClient = SyncHttpClient.builder().httpClient(httpClient).build();
        this.asyncHttpClient = AsyncHttpClient.builder().httpClient(httpClient).build();
    }

    @Override
    public String getToken() {

        try {

            lock.acquire();

            if (!isExpired(token))
                return token.accessToken();

            var request = createHttpRequest();
            var response = syncHttpClient.send(request, BodyHandlers.ofString());
            var statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                token = fromJson(response.body(), IdentityTokenResponse.class);
                return token.accessToken();
            }

            // The status code is not 2xx.
            throw new RuntimeException(response.body());

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            lock.release();
        }
    }

    @Override
    public CompletableFuture<String> getTokenAsync() {

        try {

            lock.acquire();
            var request = createHttpRequest();

            if (!isExpired(token)) {
                lock.release();
                return completedFuture(token.accessToken());
            }

            return asyncHttpClient.send(request, BodyHandlers.ofString()).thenApply(response -> {

                var statusCode = response.statusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    token = fromJson(response.body(), IdentityTokenResponse.class);
                    return token.accessToken();
                }

                // The status code is not 2xx.
                throw new CompletionException(response.body(), new RuntimeException());

            }).whenCompleteAsync((response, exeception) -> lock.release());

        } catch (InterruptedException e) {
            lock.release();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * A builder class for constructing IAMAuthenticator instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create an HTTP request to retrieve the token.
     */
    private HttpRequest createHttpRequest() {
        BodyPublisher body = BodyPublishers.ofString("grant_type=%s&apikey=%s".formatted(grantType, apiKey));
        return HttpRequest.newBuilder()
            .uri(url).timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(body).build();
    }

    /**
     * Check whether the token has expired.
     */
    private boolean isExpired(IdentityTokenResponse token) {
        if (isNull(token))
            return true;

        Date expiration = new Date(TimeUnit.SECONDS.toMillis(token.expiration()));
        Date now = new Date();

        if (expiration.after(now)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Translates a value into application/x-www-form-urlencoded.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * The builder class for constructing IAMAuthenticator instances.
     */
    public static class Builder {

        private URI url;
        private String apiKey;
        private String grantType;
        private Duration timeout;
        private HttpClient httpClient;

        /**
         * Sets the base URL for the IBM Cloud IAM token endpoint.
         *
         * @param url The base URL for the token endpoint.
         */
        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the API key used for authentication.
         *
         * @param apiKey The API key for authentication.
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the grant type used for authentication.
         *
         * @param grantType The grant type for authentication.
         */
        public Builder grantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        /**
         * Sets the timeout duration for the token request.
         *
         * @param timeout The timeout duration for the token request.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

         /**
         * Sets the http client.
         *
         * @param httpClient {@link HttpClient} instance.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds and returns an IAMAuthenticator instance.
         */
        public IAMAuthenticator build() {
            return new IAMAuthenticator(this);
        }
    }
}
