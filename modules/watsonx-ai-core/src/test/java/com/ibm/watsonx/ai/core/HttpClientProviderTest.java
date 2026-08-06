/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

public class HttpClientProviderTest {

    @BeforeEach
    @AfterEach
    void resetSingletons() throws Exception {
        for (String name : new String[] { "secureClient", "insecureClient" }) {
            Field f = HttpClientProvider.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, null);
        }
    }

    @Test
    void should_return_non_null_secure_client() {
        HttpClient client = HttpClientProvider.httpClient(true);
        assertNotNull(client);
    }

    @Test
    void should_return_same_secure_client_singleton() {
        assertEquals(HttpClientProvider.httpClient(true), HttpClientProvider.httpClient(true));
    }

    @Test
    void should_return_non_null_insecure_client() {
        HttpClient client = HttpClientProvider.httpClient(false);
        assertNotNull(client);
    }

    @Test
    void should_return_same_insecure_client_singleton() {
        assertEquals(HttpClientProvider.httpClient(false), HttpClientProvider.httpClient(false));
    }
}
