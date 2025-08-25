/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

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
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractWatsonxTest {

    @Mock
    protected HttpClient mockHttpClient;

    @Mock
    protected AuthenticationProvider mockAuthenticationProvider;

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
            mockedStatic.when(HttpClientProvider::httpClient).thenReturn(mockHttpClient);
            action.run();
        }
    }

    protected void mockHttpClientSend(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) {
        try {
            when(mockHttpClient.send(request, responseBodyHandler)).thenReturn(mockHttpResponse);
        } catch (Exception e) {
            fail(e);
        }
    }

    protected void resetHttpClient() {
        try {
            var field = HttpClientProvider.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            fail(e);
        }
    }
}
