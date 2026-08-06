/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.http.BaseHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;

@ExtendWith(MockitoExtension.class)
public class BaseHttpClientTest {

    @Mock
    HttpClient httpClient;

    @Test
    void should_return_same_request_when_id_header_already_present() throws Exception {
        SyncHttpClient client = SyncHttpClient.builder().httpClient(httpClient).build();

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.com"))
            .header(BaseHttpClient.REQUEST_ID_HEADER, "my-custom-id")
            .GET()
            .build();

        Method method = BaseHttpClient.class.getDeclaredMethod("addRequestIdHeaderIfNotPresent", HttpRequest.class);
        method.setAccessible(true);
        HttpRequest result = (HttpRequest) method.invoke(client, request);

        assertEquals(request, result);
        assertEquals("my-custom-id", result.headers().firstValue(BaseHttpClient.REQUEST_ID_HEADER).orElse(null));
    }

    @Test
    void should_add_request_id_header_when_not_present() throws Exception {
        SyncHttpClient client = SyncHttpClient.builder().httpClient(httpClient).build();

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.com"))
            .GET()
            .build();

        Method method = BaseHttpClient.class.getDeclaredMethod("addRequestIdHeaderIfNotPresent", HttpRequest.class);
        method.setAccessible(true);
        HttpRequest result = (HttpRequest) method.invoke(client, request);

        assertTrue(result.headers().firstValue(BaseHttpClient.REQUEST_ID_HEADER).isPresent());
    }
}
