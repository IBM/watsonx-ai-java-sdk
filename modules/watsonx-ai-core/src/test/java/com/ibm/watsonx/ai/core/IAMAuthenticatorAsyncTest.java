/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class IAMAuthenticatorAsyncTest extends AbstractWatsonxTest {

    @Test
    void test_ok_token() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            assertEquals("my_super_token", assertDoesNotThrow(() -> authenticator.asyncToken().get()));
            assertEquals("https://iam.cloud.ibm.com/identity/token", mockHttpRequest.getValue().uri().toString());
            assertEquals("application/x-www-form-urlencoded",
                mockHttpRequest.getValue().headers().firstValue("Content-Type").get());
            assertEquals("grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=my_super_api_key",
                Utils.bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void test_ko_token() throws Exception {

        when(mockHttpResponse.body()).thenReturn(Utils.WRONG_RESPONSE);
        when(mockHttpResponse.statusCode()).thenReturn(400);

        withWatsonxServiceMock(() -> {
            when(mockHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            var ex = assertThrows(RuntimeException.class, () -> authenticator.asyncToken().join());
            assertEquals(Utils.WRONG_RESPONSE, ex.getCause().getMessage());
        });
    }

    @Test
    void test_cache_token() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            when(mockHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .apiKey("my_super_api_key")
                .build();

            // Execute the http request.
            assertDoesNotThrow(() -> authenticator.asyncToken().get());
            // Get the value from the cache.
            assertDoesNotThrow(() -> authenticator.asyncToken().get());

            verify(mockHttpClient, times(1)).sendAsync(any(), any());
        });
    }

    @Test
    void test_custom_parameters() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));

            AuthenticationProvider authenticator = IAMAuthenticator.builder()
                .grantType("new_grant_type")
                .timeout(Duration.ofSeconds(1))
                .url(URI.create("http://mytest.com"))
                .apiKey("my_super_api_key")
                .build();

            assertEquals("my_super_token", assertDoesNotThrow(() -> authenticator.asyncToken().get()));
            assertEquals("http://mytest.com", mockHttpRequest.getValue().uri().toString());
            assertEquals("application/x-www-form-urlencoded",
                mockHttpRequest.getValue().headers().firstValue("Content-Type").get());
            assertEquals("grant_type=new_grant_type&apikey=my_super_api_key", Utils.bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_executor() throws Exception {

        var mockHttpResponse = Utils.okResponse();

        withWatsonxServiceMock(() -> {

            try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {

                when(mockHttpClient.sendAsync(any(), any(BodyHandler.class)))
                    .thenReturn(completedFuture(mockHttpResponse));

                List<String> threadNames = new ArrayList<>();

                Executor cpuExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
                    threadNames.add(Thread.currentThread().getName());
                    r.run();
                }, "cpu-thread"));

                Executor ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
                    threadNames.add(Thread.currentThread().getName());
                    r.run();
                }, "io-thread"));

                mockedStatic.when(ExecutorProvider::cpuExecutor).thenReturn(cpuExecutor);
                mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

                var authenticator = IAMAuthenticator.builder()
                    .grantType("new_grant_type")
                    .timeout(Duration.ofSeconds(1))
                    .url(URI.create("http://mytest.com"))
                    .apiKey("my_super_api_key")
                    .build();

                assertDoesNotThrow(() -> authenticator.asyncToken()
                    .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), ioExecutor)
                    .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), cpuExecutor)
                    .get(3, TimeUnit.SECONDS));

                assertEquals(4, threadNames.size());
                assertEquals("cpu-thread", threadNames.get(0));
                assertEquals("io-thread", threadNames.get(1));
                assertEquals("io-thread", threadNames.get(2));
                assertEquals("cpu-thread", threadNames.get(3));
            }
        });

    }
}
