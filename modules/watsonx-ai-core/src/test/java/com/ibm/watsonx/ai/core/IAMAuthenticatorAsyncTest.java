/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class IAMAuthenticatorAsyncTest {

    @Mock
    private HttpClient httpClient;

    @Test
    void test_ok_token() throws Exception {

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        var response = Utils.okResponse();

        when(httpClient.<String>sendAsync(requestCaptor.capture(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        AuthenticationProvider authenticator = IAMAuthenticator.builder()
            .httpClient(httpClient)
            .apiKey("my_super_api_key")
            .build();

        assertEquals("my_super_token", authenticator.getTokenAsync().get());
        assertEquals("https://iam.cloud.ibm.com/identity/token", requestCaptor.getValue().uri().toString());
        assertEquals("application/x-www-form-urlencoded",
            requestCaptor.getValue().headers().firstValue("Content-Type").get());
        assertEquals("grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=my_super_api_key",
            Utils.bodyPublisherToString(requestCaptor));
    }

    @Test
    void test_expired_token() throws Exception {

        var response = Utils.expiredResponse();

        when(httpClient.<String>sendAsync(any(), any()))
            .thenReturn(completedFuture(response));

        AuthenticationProvider authenticator = IAMAuthenticator.builder()
            .httpClient(httpClient)
            .apiKey("my_super_api_key")
            .build();

        authenticator.getTokenAsync().get();

        assertEquals("my_super_token", authenticator.getTokenAsync().get());
        verify(httpClient, times(2)).sendAsync(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_ko_token() throws Exception {

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(Utils.WRONG_RESPONSE);
        when(response.statusCode()).thenReturn(400);

        when(httpClient.<String>sendAsync(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        AuthenticationProvider authenticator = IAMAuthenticator.builder()
            .httpClient(httpClient)
            .apiKey("my_super_api_key")
            .build();

        var ex = assertThrows(RuntimeException.class, () -> authenticator.getTokenAsync().join());
        assertEquals(Utils.WRONG_RESPONSE, ex.getCause().getMessage());
    }

    @Test
    void test_cache_token() throws Exception {

        var response = Utils.okResponse();

        when(httpClient.<String>sendAsync(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        AuthenticationProvider authenticator = IAMAuthenticator.builder()
            .httpClient(httpClient)
            .apiKey("my_super_api_key")
            .build();

        // Execute the http request.
        authenticator.getTokenAsync().get();
        // Get the value from the cache.
        authenticator.getTokenAsync().get();

        verify(httpClient, times(1)).sendAsync(any(), any());
    }

    @Test
    void test_custom_parameters() throws Exception {

        var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        var response = Utils.okResponse();

        when(httpClient.<String>sendAsync(requestCaptor.capture(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        AuthenticationProvider authenticator = IAMAuthenticator.builder()
            .httpClient(httpClient)
            .grantType("new_grant_type")
            .timeout(Duration.ofSeconds(1))
            .url(URI.create("http://mytest.com"))
            .apiKey("my_super_api_key")
            .build();

        assertEquals("my_super_token", authenticator.getTokenAsync().get());
        assertEquals("http://mytest.com", requestCaptor.getValue().uri().toString());
        assertEquals("application/x-www-form-urlencoded",
            requestCaptor.getValue().headers().firstValue("Content-Type").get());
        assertEquals("grant_type=new_grant_type&apikey=my_super_api_key", Utils.bodyPublisherToString(requestCaptor));
    }
}
