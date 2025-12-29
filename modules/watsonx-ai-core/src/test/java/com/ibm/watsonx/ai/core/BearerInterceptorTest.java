/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.cp4d.AuthMode;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

@ExtendWith(MockitoExtension.class)
public class BearerInterceptorTest extends AbstractWatsonxTest {

    @Nested
    class Sync {

        @Test
        void should_send_request_with_bearer_token() throws Exception {

            when(mockAuthenticator.token()).thenReturn("my_super_token");
            when(mockHttpResponse.statusCode()).thenReturn(200);

            withWatsonxServiceMock(() -> {

                mockHttpClientSend(mockHttpRequest.capture(), any());

                var client = SyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(mockAuthenticator))
                    .build();

                try {

                    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                        .POST(BodyPublishers.ofString("Call this!"))
                        .build();

                    assertDoesNotThrow(() -> client.send(fakeRequest, BodyHandlers.ofString()));
                    assertEquals("Bearer my_super_token", mockHttpRequest.getValue().headers().firstValue("Authorization").get());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_send_request_with_zen_api_key() throws Exception {

            var cp4dAuthenticatorMock = mock(CP4DAuthenticator.class);
            when(cp4dAuthenticatorMock.token()).thenReturn("#1234");
            when(cp4dAuthenticatorMock.isAuthMode(AuthMode.ZEN_API_KEY)).thenReturn(true);
            when(mockHttpResponse.statusCode()).thenReturn(200);

            withWatsonxServiceMock(() -> {

                mockHttpClientSend(mockHttpRequest.capture(), any());

                var client = SyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(cp4dAuthenticatorMock))
                    .build();

                try {

                    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                        .POST(BodyPublishers.ofString("Call this!"))
                        .build();

                    assertDoesNotThrow(() -> client.send(fakeRequest, BodyHandlers.ofString()));
                    assertEquals("ZenApiKey #1234", mockHttpRequest.getValue().headers().firstValue("Authorization").get());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_throw_exception_when_bearer_token_is_invalid() throws Exception {

            when(mockAuthenticator.token()).thenThrow(new RuntimeException("error"));

            withWatsonxServiceMock(() -> {

                var client = SyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(mockAuthenticator))
                    .build();

                try {

                    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                        .POST(BodyPublishers.ofString("Call this!"))
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()));
                    assertEquals("error", ex.getMessage());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }

    @Nested
    class Async {

        @Test
        void should_send_request_with_bearer_token() throws Exception {

            when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my_super_token"));

            withWatsonxServiceMock(() -> {
                mockHttpClientAsyncSend(mockHttpRequest.capture(), any());

                var client = AsyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(mockAuthenticator))
                    .build();

                var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                    .POST(BodyPublishers.ofString("Call this!"))
                    .build();

                assertDoesNotThrow(() -> client.send(fakeRequest, BodyHandlers.ofString()).get());
                assertEquals("Bearer my_super_token", mockHttpRequest.getValue().headers().firstValue("Authorization").get());
            });
        }

        @Test
        void should_send_request_with_zen_api_key() throws Exception {

            var cp4dAuthenticatorMock = mock(CP4DAuthenticator.class);
            when(cp4dAuthenticatorMock.asyncToken()).thenReturn(completedFuture("#1234"));
            when(cp4dAuthenticatorMock.isAuthMode(AuthMode.ZEN_API_KEY)).thenReturn(true);

            withWatsonxServiceMock(() -> {

                mockHttpClientAsyncSend(mockHttpRequest.capture(), any());

                var client = AsyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(cp4dAuthenticatorMock))
                    .build();

                var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                    .POST(BodyPublishers.ofString("Call this!"))
                    .build();

                assertDoesNotThrow(() -> client.send(fakeRequest, BodyHandlers.ofString()));
                assertEquals("ZenApiKey #1234", mockHttpRequest.getValue().headers().firstValue("Authorization").get());
            });
        }

        @Test
        void should_throw_exception_when_bearer_token_is_invalid() {

            when(mockAuthenticator.asyncToken()).thenThrow(new RuntimeException("error"));

            withWatsonxServiceMock(() -> {

                var client = AsyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(mockAuthenticator))
                    .build();

                var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                    .POST(BodyPublishers.ofString("Call this!"))
                    .build();

                var ex =
                    assertThrows(RuntimeException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()).join());
                assertEquals("error", ex.getMessage());
            });
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_execute_with_custom_executor() throws Exception {

            when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my_super_token"));
            var threadNames = new ArrayList<>();

            var ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
                threadNames.add(Thread.currentThread().getName());
                r.run();
            }, "io-thread"));

            withWatsonxServiceMock(() -> {
                mockHttpClientAsyncSend(mockHttpRequest.capture(), any());
                when(mockHttpClient.sendAsync(any(), any(BodyHandler.class))).thenReturn(completedFuture(mockHttpResponse));

                try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {
                    mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

                    var client = AsyncHttpClient.builder()
                        .httpClient(mockHttpClient)
                        .interceptor(new BearerInterceptor(mockAuthenticator))
                        .interceptor(new AsyncHttpInterceptor() {
                            @Override
                            public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index,
                                AsyncChain chain) {
                                threadNames.add(Thread.currentThread().getName());
                                return chain.proceed(request, bodyHandler);
                            }
                        }).build();

                    assertDoesNotThrow(() -> client.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://test.com"))
                        .GET().build(), BodyHandlers.ofString())
                        .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), ioExecutor)
                        .get(3, TimeUnit.SECONDS));

                    assertEquals(3, threadNames.size());
                    assertEquals("main", threadNames.get(0));
                    assertEquals("io-thread", threadNames.get(1));
                    assertEquals("io-thread", threadNames.get(2));
                }
            });
        }
    }
}
