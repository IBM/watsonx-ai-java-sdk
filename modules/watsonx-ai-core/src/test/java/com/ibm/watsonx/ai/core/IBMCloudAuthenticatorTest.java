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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IBMCloudAuthenticatorTest extends AbstractWatsonxTest {

    @Nested
    class Sync {

        @Test
        void should_return_a_valid_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(mockHttpRequest.capture(), any())).thenReturn(response);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    assertEquals("my_super_token", authenticator.token());
                    assertEquals("https://iam.cloud.ibm.com/identity/token", mockHttpRequest.getValue().uri().toString());
                    assertEquals("application/x-www-form-urlencoded",
                        mockHttpRequest.getValue().headers().firstValue("Content-Type").get());
                    assertEquals("grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=my_super_api_key",
                        Utils.bodyPublisherToString(mockHttpRequest));
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_raise_an_exception() throws Exception {

            var response = Utils.koResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertEquals(ex.getMessage(), Utils.WRONG_RESPONSE);
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_cached_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    // Execute the http request.
                    authenticator.token();
                    // Get the value from the cache.
                    authenticator.token();

                    verify(mockHttpClient, times(1)).send(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_refresh_token_when_existing_token_is_expired() throws Exception {

            var expiredResponse = Utils.expiredResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any()))
                        .thenReturn(expiredResponse);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    authenticator.token();

                    assertEquals("my_super_token", authenticator.token());
                    verify(mockHttpClient, times(2)).send(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_custom_parameters_when_provided() throws Exception {

            var response = Utils.okResponse();
            var httpRequest = ArgumentCaptor.forClass(HttpRequest.class);

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(httpRequest.capture(), any())).thenReturn(response);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .grantType("new_grant_type")
                        .timeout(Duration.ofSeconds(1))
                        .baseUrl(URI.create("http://mytest.com"))
                        .apiKey("my_super_api_key")
                        .build();

                    assertEquals("my_super_token", authenticator.token());
                    assertEquals("http://mytest.com/identity/token", httpRequest.getValue().uri().toString());
                    assertEquals("application/x-www-form-urlencoded",
                        httpRequest.getValue().headers().firstValue("Content-Type").get());
                    assertEquals("grant_type=new_grant_type&apikey=my_super_api_key", Utils.bodyPublisherToString(httpRequest));
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_throw_watsonx_exception_when_error_response_received() throws Exception {

            var response = mock(HttpResponse.class);
            var httpRequest = ArgumentCaptor.forClass(HttpRequest.class);
            var errorMessage = """
                {
                    "errorCode": "BXNIM0415E",
                    "errorMessage": "Provided API key could not be found.",
                    "errorDetails": "More details",
                    "context": {
                        "requestId": "xxx"
                    }
                }""";

            when(response.body()).thenReturn(errorMessage);
            when(response.statusCode()).thenReturn(400);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (k, v) -> true));

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(httpRequest.capture(), any())).thenReturn(response);

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .grantType("new_grant_type")
                        .timeout(Duration.ofSeconds(1))
                        .baseUrl(URI.create("http://mytest.com"))
                        .apiKey("my_super_api_key")
                        .build();

                    var ex = assertThrows(WatsonxException.class, () -> authenticator.token());
                    assertEquals(400, ex.statusCode());
                    assertEquals(errorMessage, ex.getMessage());
                    var detail = ex.details().orElseThrow().errors().get(0);
                    assertEquals("BXNIM0415E", detail.code());
                    assertEquals("Provided API key could not be found.", detail.message());
                    assertEquals("More details", detail.moreInfo());

                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_build_authenticator_with_provided_parameters() throws Exception {

            var authenticator = IBMCloudAuthenticator.builder()
                .apiKey("test")
                .grantType("test")
                .timeout(Duration.ofSeconds(3))
                .baseUrl(URI.create("http://test"))
                .build();


            Field apiKey = IBMCloudAuthenticator.class.getDeclaredField("apiKey");
            apiKey.setAccessible(true);
            assertEquals("test", (String) apiKey.get(authenticator));

            Field grantType = IBMCloudAuthenticator.class.getDeclaredField("grantType");
            grantType.setAccessible(true);
            assertEquals("test", (String) grantType.get(authenticator));

            Field timeout = IBMCloudAuthenticator.class.getDeclaredField("timeout");
            timeout.setAccessible(true);
            assertEquals(Duration.ofSeconds(3), (Duration) timeout.get(authenticator));

            Field url = IBMCloudAuthenticator.class.getDeclaredField("baseUrl");
            url.setAccessible(true);
            assertEquals(URI.create("http://test"), (URI) url.get(authenticator));
        }

        @Test
        void should_throw_exception_when_http_client_fails_and_then_recover() throws Exception {

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any()))
                        .thenThrow(new IOException("IOException"))
                        .thenReturn(Utils.okResponse());

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertEquals(ex.getMessage(), "IOException");

                    // Test lock release.
                    assertEquals("my_super_token", authenticator.token());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }

    @Nested
    class Async {

        @Test
        void should_return_a_valid_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
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
        void should_use_a_cached_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockHttpClient.<String>sendAsync(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
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
        void should_use_custom_parameters() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
                    .grantType("new_grant_type")
                    .timeout(Duration.ofSeconds(1))
                    .baseUrl(URI.create("http://mytest.com"))
                    .apiKey("my_super_api_key")
                    .build();

                assertEquals("my_super_token", assertDoesNotThrow(() -> authenticator.asyncToken().get()));
                assertEquals("http://mytest.com/identity/token", mockHttpRequest.getValue().uri().toString());
                assertEquals("application/x-www-form-urlencoded",
                    mockHttpRequest.getValue().headers().firstValue("Content-Type").get());
                assertEquals("grant_type=new_grant_type&apikey=my_super_api_key", Utils.bodyPublisherToString(mockHttpRequest));
            });
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_use_the_correct_executors() throws Exception {

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

                    var authenticator = IBMCloudAuthenticator.builder()
                        .grantType("new_grant_type")
                        .timeout(Duration.ofSeconds(1))
                        .baseUrl(URI.create("http://mytest.com"))
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
}
