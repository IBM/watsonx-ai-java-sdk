/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractWatsonxTest {

    protected static final String ML_API_PATH = "/ml/v1";
    protected static final String ML_API_TEXT_PATH = ML_API_PATH.concat("/text");
    protected static final String API_VERSION = "2025-12-05";
    protected static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";
    protected static final Executor originalExecutor = ExecutorProvider.ioExecutor();

    @Mock
    protected HttpClient mockHttpClient;

    @Mock
    protected Authenticator mockAuthenticator;

    @Captor
    protected ArgumentCaptor<HttpRequest> mockHttpRequest;

    @Mock
    protected HttpResponse<String> mockHttpResponse;

    @RegisterExtension
    protected WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

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

    protected void withCustomExecutor(Runnable action, Executor executor) {
        setIoExecutor(executor);
        action.run();
        setIoExecutor(originalExecutor);
    }

    private void setIoExecutor(Executor executor) {
        try {
            Field ioExecutorField = ExecutorProvider.class.getDeclaredField("ioExecutor");
            ioExecutorField.setAccessible(true);
            ioExecutorField.set(null, executor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
