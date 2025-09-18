/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class BearerInterceptorTest extends AbstractWatsonxTest {

    @Test
    void test_bearer_interceptor_sync() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            var client = SyncHttpClient.builder()
                .httpClient(mockHttpClient)
                .interceptor(new BearerInterceptor(authenticator))
                .build();

            try {
                when(mockHttpClient.<String>send(mockHttpRequest.capture(), any()))
                    .thenReturn(response);

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
    void test_bearer_interceptor_sync_with_exception() throws Exception {

        var response = Utils.koResponse();

        withWatsonxServiceMock(() -> {
            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            var client = SyncHttpClient.builder()
                .httpClient(mockHttpClient)
                .interceptor(new BearerInterceptor(authenticator))
                .build();

            try {
                when(mockHttpClient.<String>send(mockHttpRequest.capture(), any()))
                    .thenReturn(response);

                var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                    .POST(BodyPublishers.ofString("Call this!"))
                    .build();

                var ex = assertThrows(RuntimeException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()));
                assertEquals(Utils.WRONG_RESPONSE, ex.getMessage());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void test_bearer_interceptor_async() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            var client = AsyncHttpClient.builder()
                .httpClient(mockHttpClient)
                .interceptor(new BearerInterceptor(authenticator))
                .build();

            when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                .thenReturn(completedFuture(response));

            var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                .POST(BodyPublishers.ofString("Call this!"))
                .build();

            assertDoesNotThrow(() -> client.send(fakeRequest, BodyHandlers.ofString()).get());
            assertEquals("Bearer my_super_token", mockHttpRequest.getValue().headers().firstValue("Authorization").get());
        });
    }

    @Test
    void test_bearer_interceptor_async_with_exception() {

        when(mockHttpResponse.body()).thenReturn(Utils.WRONG_RESPONSE);
        when(mockHttpResponse.statusCode()).thenReturn(400);

        withWatsonxServiceMock(() -> {
            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            var client = AsyncHttpClient.builder()
                .httpClient(mockHttpClient)
                .interceptor(new BearerInterceptor(authenticator))
                .build();

            when(mockHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(completedFuture(mockHttpResponse));

            var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
                .POST(BodyPublishers.ofString("Call this!"))
                .build();

            var ex =
                assertThrows(CompletionException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()).join());
            assertEquals(Utils.WRONG_RESPONSE, ex.getCause().getMessage());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_executor() throws Exception {

        var threadNames = new ArrayList<>();

        var cpuExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "cpu-thread"));

        var ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "io-thread"));

        withWatsonxServiceMock(() -> {
            when(mockHttpResponse.body()).thenReturn(Utils.OK_RESPONSE.formatted(12456));
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.sendAsync(any(), any(BodyHandler.class))).thenReturn(completedFuture(mockHttpResponse));

            try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {
                mockedStatic.when(ExecutorProvider::cpuExecutor).thenReturn(cpuExecutor);
                mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

                AuthenticationProvider authenticator = IAMAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                var client = AsyncHttpClient.builder()
                    .httpClient(mockHttpClient)
                    .interceptor(new BearerInterceptor(authenticator))
                    .interceptor(new AsyncHttpInterceptor() {
                        @Override
                        public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, Executor executor,
                            int index, AsyncChain chain) {
                            assertEquals("io-thread", Thread.currentThread().getName());
                            assertEquals(ioExecutor, executor);
                            threadNames.add(Thread.currentThread().getName());
                            return chain.proceed(request, bodyHandler, executor);
                        }
                    }).build();

                assertDoesNotThrow(() -> client.send(HttpRequest.newBuilder()
                    .uri(URI.create("https://test.com"))
                    .GET().build(), BodyHandlers.ofString())
                    .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), ioExecutor)
                    .get(3, TimeUnit.SECONDS));

                assertEquals(4, threadNames.size());
                assertEquals("cpu-thread", threadNames.get(0));
                assertEquals("io-thread", threadNames.get(1));
                assertEquals("io-thread", threadNames.get(2));
                assertEquals("io-thread", threadNames.get(3));
            }
        });
    }
}
