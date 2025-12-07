/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.factory;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

/**
 * Factory class for creating configured {@link SyncHttpClient} and {@link AsyncHttpClient} instances.
 * <p>
 * This class centralizes the setup of HTTP clients with the standard interceptors used by watsonx.ai:
 * <ul>
 * <li>{@link RetryInterceptor#ON_TOKEN_EXPIRED} – retry on expired authentication tokens</li>
 * <li>{@link RetryInterceptor#ON_RETRYABLE_STATUS_CODES} – retry on retryable status codes (5xx, etc.)</li>
 * <li>{@link BearerInterceptor} – attach an IAM or custom {@link Authenticator}</li>
 * <li>{@link LoggerInterceptor} – optional request/response logging</li>
 * </ul>
 */
public final class HttpClientFactory {

    private HttpClientFactory() {}

    /**
     * Creates and configures a new {@link SyncHttpClient} with standard interceptors.
     *
     * @param authenticator {@link Authenticator} used to attach a bearer token
     * @param logMode Indicate whether logging should be enabled
     * @return {@link SyncHttpClient} instance
     */
    public static SyncHttpClient createSync(Authenticator authenticator, LogMode logMode) {

        var httpClient = HttpClientProvider.httpClient();
        var builder = SyncHttpClient.builder().httpClient(httpClient);

        builder.interceptor(RetryInterceptor.ON_TOKEN_EXPIRED);

        if (nonNull(authenticator)) {
            builder.interceptor(new BearerInterceptor(authenticator));
        }

        builder.interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES);

        if (nonNull(logMode)) {
            switch(logMode) {
                case BOTH -> builder.interceptor(new LoggerInterceptor(true, true));
                case REQUEST -> builder.interceptor(new LoggerInterceptor(true, false));
                case RESPONSE -> builder.interceptor(new LoggerInterceptor(false, true));
                case DISABLED -> {}
            }
        }

        return builder.build();
    }

    /**
     * Creates and configures a new {@link AsyncHttpClient} with standard interceptors.
     *
     * @param authenticator {@link Authenticator} used to attach a bearer token
     * @param logMode Indicate whether logging should be enabled
     * @return {@link AsyncHttpClient} instance
     */
    public static AsyncHttpClient createAsync(Authenticator authenticator, LogMode logMode) {

        var httpClient = HttpClientProvider.httpClient();
        var builder = AsyncHttpClient.builder().httpClient(httpClient);

        builder.interceptor(RetryInterceptor.ON_TOKEN_EXPIRED);

        if (nonNull(authenticator)) {
            builder.interceptor(new BearerInterceptor(authenticator));
        }

        builder.interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES);

        if (nonNull(logMode)) {
            switch(logMode) {
                case BOTH -> builder.interceptor(new LoggerInterceptor(true, true));
                case REQUEST -> builder.interceptor(new LoggerInterceptor(true, false));
                case RESPONSE -> builder.interceptor(new LoggerInterceptor(false, true));
                case DISABLED -> {}
            }
        }

        return builder.build();
    }
}
