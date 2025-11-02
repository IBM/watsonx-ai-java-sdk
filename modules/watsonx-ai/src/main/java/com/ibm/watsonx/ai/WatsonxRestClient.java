/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

public abstract class WatsonxRestClient {

    public static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";

    protected final String baseUrl;
    protected final String version;
    protected final Duration timeout;
    protected final boolean logRequests, logResponses;
    protected final AuthenticationProvider authenticationProvider;

    protected WatsonxRestClient(Builder<?, ?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The url must be provided");
        version = requireNonNull(builder.version, "The version must be provided");
        timeout = requireNonNull(builder.timeout, "The timeout must be provided");
        logRequests = requireNonNullElse(builder.logRequests, false);
        logResponses = requireNonNullElse(builder.logResponses, false);
        authenticationProvider = builder.authenticationProvider;
    }

    @SuppressWarnings("unchecked")
    protected static abstract class Builder<T, B extends Builder<T, B>> {
        private String baseUrl;
        private String version;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        protected AuthenticationProvider authenticationProvider;

        public abstract T build();

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param baseUrl the endpoint URL as a string
         */
        public B baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return (B) this;
        }

        /**
         * The version date for the API of the form YYYY-MM-DD.
         *
         * @param version Version date
         */
        public B version(String version) {
            this.version = version;
            return (B) this;
        }

        /**
         * Enables or disables logging of the request payload.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise
         */
        public B logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return (B) this;
        }

        /**
         * Enables or disables logging of the response payload.
         *
         * @param logResponses {@code true} to log the response, {@code false} otherwise
         */
        public B logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return (B) this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout {@link Duration} timeout.
         */
        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }

        /**
         * Sets the {@link AuthenticationProvider} used to authenticate requests.
         *
         * @param authenticationProvider {@link AuthenticationProvider} instance
         */
        public B authenticationProvider(AuthenticationProvider authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
            return (B) this;
        }
    }
}
