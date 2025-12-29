/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class RetryConfigTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void should_use_default_token_expired_max_retries() {
        assertEquals(1, RetryConfig.tokenExpiredMaxRetries());
    }

    @Test
    void should_use_default_status_codes_max_retries() {
        assertEquals(10, RetryConfig.statusCodesMaxRetries());
    }

    @Test
    void should_use_default_status_codes_backoff_enabled() {
        assertTrue(RetryConfig.statusCodesExponentialBackoffEnabled());
    }

    @Test
    void should_use_default_status_codes_initial_retry_interval() {
        assertEquals(Duration.ofMillis(20), RetryConfig.statusCodesInitialRetryInterval());
    }

    @Test
    void should_customize_token_expired_max_retries() {
        environmentVariables.set("WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES", "5");
        assertEquals(5, RetryConfig.tokenExpiredMaxRetries());
    }

    @Test
    void should_customize_status_codes_max_retries() {
        environmentVariables.set("WATSONX_RETRY_STATUS_CODES_MAX_RETRIES", "3");
        assertEquals(3, RetryConfig.statusCodesMaxRetries());
    }

    @Test
    void should_customize_status_codes_backoff_enabled() {
        environmentVariables.set("WATSONX_RETRY_STATUS_CODES_BACKOFF_ENABLED", "false");
        assertFalse(RetryConfig.statusCodesExponentialBackoffEnabled());
    }

    @Test
    void should_customize_status_codes_initial_retry_interval() {
        environmentVariables.set("WATSONX_RETRY_STATUS_CODES_INITIAL_INTERVAL_MS", "200");
        assertEquals(Duration.ofMillis(200), RetryConfig.statusCodesInitialRetryInterval());
    }
}