/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractWatsonxTest {

    @Mock
    protected HttpClient mockSecureHttpClient;

    @Mock
    protected HttpClient mockInsecureHttpClient;

    @Mock
    protected Authenticator mockAuthenticator;

    @Captor
    protected ArgumentCaptor<HttpRequest> mockHttpRequest;

    @Mock
    protected HttpResponse<String> mockHttpResponse;

    @BeforeEach
    void setUp() {
        resetHttpClient();
    }

    protected void withWatsonxServiceMock(Runnable action) {
        try (MockedStatic<HttpClientProvider> mockedStatic = mockStatic(HttpClientProvider.class)) {
            mockedStatic.when(() -> HttpClientProvider.httpClient(true)).thenReturn(mockSecureHttpClient);
            mockedStatic.when(() -> HttpClientProvider.httpClient(false)).thenReturn(mockInsecureHttpClient);
            action.run();
        }
    }

    protected void mockHttpClientSend(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) {
        try {
            when(mockSecureHttpClient.send(request, responseBodyHandler)).thenReturn(mockHttpResponse);
        } catch (Exception e) {
            fail(e);
        }
    }

    protected void mockHttpClientAsyncSend(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) {
        try {
            when(mockSecureHttpClient.sendAsync(request, responseBodyHandler)).thenReturn(completedFuture(mockHttpResponse));
        } catch (Exception e) {
            fail(e);
        }
    }

    protected void resetHttpClient() {
        try {
            var secureClient = HttpClientProvider.class.getDeclaredField("secureClient");
            secureClient.setAccessible(true);
            secureClient.set(null, null);
            var insecureClient = HttpClientProvider.class.getDeclaredField("insecureClient");
            insecureClient.setAccessible(true);
            insecureClient.set(null, null);
        } catch (Exception e) {
            fail(e);
        }
    }
}
