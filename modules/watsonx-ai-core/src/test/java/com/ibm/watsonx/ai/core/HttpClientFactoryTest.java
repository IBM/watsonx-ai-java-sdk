/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

@ExtendWith(MockitoExtension.class)
public class HttpClientFactoryTest {

    @Mock
    HttpClient httpClient;

    @Test
    void should_create_sync_client_for_all_log_modes() {
        for (LogMode mode : LogMode.values()) {
            assertNotNull(HttpClientFactory.createSync(null, httpClient, mode));
        }
    }

    @Test
    void should_create_sync_client_when_log_mode_is_null() {
        assertNotNull(HttpClientFactory.createSync(null, httpClient, null));
    }

    @Test
    void should_create_sync_client_with_authenticator() {
        assertNotNull(HttpClientFactory.createSync(mock(Authenticator.class), httpClient, LogMode.BOTH));
    }

    @Test
    void should_create_async_client_for_all_log_modes() {
        for (LogMode mode : LogMode.values()) {
            assertNotNull(HttpClientFactory.createAsync(null, httpClient, mode));
        }
    }

    @Test
    void should_create_async_client_when_log_mode_is_null() {
        assertNotNull(HttpClientFactory.createAsync(null, httpClient, null));
    }

    @Test
    void should_create_async_client_with_authenticator() {
        assertNotNull(HttpClientFactory.createAsync(mock(Authenticator.class), httpClient, LogMode.BOTH));
    }

    @Test
    void should_throw_when_http_client_is_null_for_sync() {
        assertThrows(NullPointerException.class, () -> HttpClientFactory.createSync(null, null, null));
    }

    @Test
    void should_throw_when_http_client_is_null_for_async() {
        assertThrows(NullPointerException.class, () -> HttpClientFactory.createAsync(null, null, null));
    }
}
