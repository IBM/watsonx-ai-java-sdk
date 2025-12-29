/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.ibm.watsonx.ai.core.auth.cp4d.AuthMode.IAM;
import static com.ibm.watsonx.ai.core.auth.cp4d.AuthMode.LEGACY;
import static com.ibm.watsonx.ai.core.utils.Utils.getFieldValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.http.HttpClient;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

public class CustomHttpClientTest {

    @Test
    void should_use_custom_http_client_for_cp4d_authentication() {

        Stream.of(LEGACY, IAM).forEach(authMode -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                Authenticator authenticator = CP4DAuthenticator.builder()
                    .baseUrl("https://localhost")
                    .username("username")
                    .password("password")
                    .apiKey("apiKey")
                    .authMode(authMode)
                    .httpClient(customClient)
                    .build();

                Object cp4dRestClient = getFieldValue(authenticator, "client");
                assertEquals(customClient, getFieldValue(cp4dRestClient, "httpClient"));
                assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(cp4dRestClient, "httpClient"));

                Object syncHttpClient = getFieldValue(cp4dRestClient, "syncHttpClient");
                assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(cp4dRestClient, "asyncHttpClient");
                assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_default_http_client_for_cp4d_authentication() throws Exception {

        Stream.of(LEGACY, IAM).forEach(authMode -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                Authenticator authenticator = CP4DAuthenticator.builder()
                    .baseUrl("https://localhost")
                    .username("username")
                    .password("password")
                    .build();

                Object cp4dRestClient = getFieldValue(authenticator, "client");
                assertNotEquals(customClient, getFieldValue(cp4dRestClient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(), getFieldValue(cp4dRestClient, "httpClient"));

                Object syncHttpClient = getFieldValue(cp4dRestClient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(cp4dRestClient, "asyncHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_ibm_cloud_authentication() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        Authenticator authenticator = IBMCloudAuthenticator.builder()
            .apiKey("apiKey")
            .httpClient(customClient)
            .build();

        Object ibmCloudRestClient = getFieldValue(authenticator, "client");
        assertEquals(customClient, getFieldValue(ibmCloudRestClient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(ibmCloudRestClient, "httpClient"));

        Object syncHttpClient = getFieldValue(ibmCloudRestClient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(ibmCloudRestClient, "asyncHttpClient");
        assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(), getFieldValue(asyncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_ibm_cloud_authentication() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        Authenticator authenticator = IBMCloudAuthenticator.builder()
            .apiKey("apiKey")
            .build();

        Object ibmCloudRestClient = getFieldValue(authenticator, "client");
        assertNotEquals(customClient, getFieldValue(ibmCloudRestClient, "httpClient"));
        assertEquals(HttpClientProvider.httpClient(), getFieldValue(ibmCloudRestClient, "httpClient"));

        Object syncHttpClient = getFieldValue(ibmCloudRestClient, "syncHttpClient");
        assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertEquals(HttpClientProvider.httpClient(), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(ibmCloudRestClient, "asyncHttpClient");
        assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertEquals(HttpClientProvider.httpClient(), getFieldValue(asyncHttpClient, "delegate"));
    }
}
