/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Optional.ofNullable;
import java.time.Duration;

/**
 * Configuration class for retry behavior in HTTP interceptors.
 * <p>
 * This class provides centralized access to retry configuration parameters that can be customized via environment variables. All methods return
 * default values if the corresponding environment variables are not set.
 * </p>
 * <p>
 * Supported environment variables:
 * <ul>
 * <li>{@code WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES} - Maximum retries for expired token errors (default: 1)</li>
 * <li>{@code WATSONX_RETRY_STATUS_CODES_MAX_RETRIES} - Maximum retries for transient status codes (default: 10)</li>
 * <li>{@code WATSONX_RETRY_STATUS_CODES_BACKOFF_ENABLED} - Enable exponential backoff (default: true)</li>
 * <li>{@code WATSONX_RETRY_STATUS_CODES_INITIAL_INTERVAL_MS} - Initial retry interval in milliseconds (default: 20)</li>
 * </ul>
 * </p>
 */
public final class RetryConfig {
    private static final int DEFAULT_TOKEN_EXPIRED_MAX_RETRIES = 1;
    private static final int DEFAULT_STATUS_CODES_MAX_RETRIES = 10;
    private static final boolean DEFAULT_STATUS_CODES_BACKOFF_ENABLED = true;
    private static final Duration DEFAULT_STATUS_CODES_INITIAL_INTERVAL = Duration.ofMillis(20);

    private RetryConfig() {}

    /**
     * Returns the maximum number of retry attempts for expired authentication token errors.
     * <p>
     * This value can be customized by setting the {@code WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES} environment variable.
     * </p>
     *
     * @return the maximum number of retries for token expiration, defaults to 1
     */
    public static int tokenExpiredMaxRetries() {
        return ofNullable(System.getenv("WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES"))
            .map(Integer::valueOf)
            .orElse(DEFAULT_TOKEN_EXPIRED_MAX_RETRIES);
    }

    /**
     * Returns the maximum number of retry attempts for transient HTTP status codes.
     * <p>
     * This value can be customized by setting the {@code WATSONX_RETRY_STATUS_CODES_MAX_RETRIES} environment variable.
     * <p>
     * Applies to status codes: {@code 429}, {@code 503}, {@code 504}, and {@code 520}.
     * </p>
     *
     * @return the maximum number of retries for status codes, defaults to 10
     */
    public static int statusCodesMaxRetries() {
        return ofNullable(System.getenv("WATSONX_RETRY_STATUS_CODES_MAX_RETRIES"))
            .map(Integer::valueOf)
            .orElse(DEFAULT_STATUS_CODES_MAX_RETRIES);
    }

    /**
     * Returns whether exponential backoff is enabled for status code retries.
     * <p>
     * When enabled, the retry interval doubles after each failed attempt. This value can be customized by setting the
     * {@code WATSONX_RETRY_STATUS_CODES_BACKOFF_ENABLED} environment variable.
     * </p>
     *
     * @return {@code true} if exponential backoff is enabled, defaults to {@code true}
     */
    public static boolean statusCodesExponentialBackoffEnabled() {
        return ofNullable(System.getenv("WATSONX_RETRY_STATUS_CODES_BACKOFF_ENABLED"))
            .map(Boolean::valueOf)
            .orElse(DEFAULT_STATUS_CODES_BACKOFF_ENABLED);
    }

    /**
     * Returns the initial retry interval for status code retries.
     * <p>
     * This value can be customized by setting the {@code WATSONX_RETRY_STATUS_CODES_INITIAL_INTERVAL_MS} environment variable (in milliseconds). When
     * exponential backoff is enabled, this serves as the base interval that gets doubled with each retry.
     * </p>
     *
     * @return the initial retry interval, defaults to 20 milliseconds
     */
    public static Duration statusCodesInitialRetryInterval() {
        return ofNullable(System.getenv("WATSONX_RETRY_STATUS_CODES_INITIAL_INTERVAL_MS"))
            .map(Long::valueOf)
            .map(Duration::ofMillis)
            .orElse(DEFAULT_STATUS_CODES_INITIAL_INTERVAL);
    }
}

