/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

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
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.cp4d.AuthMode;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class CP4DAuthenticationTest extends AbstractWatsonxTest {

    @Nested
    @SuppressWarnings("unchecked")
    class Sync {

        @Test
        void should_return_a_valid_token() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");

            when(response.statusCode()).thenReturn(200);

            withWatsonxServiceMock(() -> {

                try {
                    when(mockHttpClient.<String>send(mockHttpRequest.capture(), any())).thenReturn(response);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .apiKey("api_key")
                        .build();

                    assertEquals("access-token", authenticator.token());
                    assertEquals("http://my-url/icp4d-api/v1/authorize", mockHttpRequest.getValue().uri().toString());
                    JSONAssert.assertEquals("""
                        {
                            "username": "username",
                            "api_key": "api_key"
                        }
                        """, Utils.bodyPublisherToString(mockHttpRequest), true);
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_return_a_valid_token_using_password() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");

            when(response.statusCode()).thenReturn(200);

            withWatsonxServiceMock(() -> {

                try {
                    when(mockHttpClient.<String>send(mockHttpRequest.capture(), any())).thenReturn(response);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .password("password")
                        .build();

                    assertEquals("access-token", authenticator.token());
                    assertEquals("http://my-url/icp4d-api/v1/authorize", mockHttpRequest.getValue().uri().toString());
                    JSONAssert.assertEquals("""
                        {
                            "username": "username",
                            "password": "password"
                        }
                        """, Utils.bodyPublisherToString(mockHttpRequest), true);
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_raise_an_exception() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(500);
            when(response.body()).thenReturn("""
                {
                    "code": 602,
                    "message": "properties.username in body is required"
                } """);

            when(response.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .apiKey("my_super_api_key")
                        .timeout(Duration.ofSeconds(10))
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertEquals(ex.getMessage(), """
                        {
                            "code": 602,
                            "message": "properties.username in body is required"
                        }""");
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_cached_token() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");


            // NO IAM.
            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl("http://my-url/")
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
        void should_use_cached_token_with_iam_configuration() throws Exception {

            HttpResponse<String> identityTokenResponse = Utils.okResponse();
            HttpResponse<String> validateResponse = mock(HttpResponse.class);
            when(validateResponse.statusCode()).thenReturn(200);
            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {

                    when(mockHttpClient.<String>send(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return identityTokenResponse;
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return validateResponse;
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .baseUrl("http://my-url/")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    var token = authenticator.token();
                    assertEquals("iam-access-token", token);

                    token = authenticator.token();
                    assertEquals("iam-access-token", token);

                    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                    verify(mockHttpClient, times(2)).send(captor.capture(), any());

                    List<HttpRequest> requests = captor.getAllValues();
                    long identityCalls = requests.stream()
                        .filter(r -> r.uri().toString().endsWith("/idprovider/v1/auth/identitytoken"))
                        .count();
                    long validateCalls = requests.stream()
                        .filter(r -> r.uri().toString().endsWith("/v1/preauth/validateAuth"))
                        .count();

                    assertEquals(1, identityCalls, "Identity token should be called once");
                    assertEquals(1, validateCalls, "ValidateAuth should be called once");

                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_fail_if_both_password_and_api_key_are_not_set() throws Exception {

            var ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .build()
            );
            assertEquals("Either password or apiKey must be provided", ex.getMessage());
        }

        @Test
        void should_catch_the_io_exception_correctly() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any()))
                        .thenThrow(new IOException("IOException"))
                        .thenReturn(response);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl("http://my-url/")
                        .apiKey("my_api_key")
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertEquals(ex.getMessage(), "IOException");

                    assertEquals("access-token", authenticator.token());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_iam_authentication() {

            var identityTokenResponse = Utils.okResponse();
            HttpResponse<String> validateResponse = mock(HttpResponse.class);
            when(validateResponse.statusCode()).thenReturn(200);
            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return identityTokenResponse;
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return validateResponse;
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    assertEquals("iam-access-token", authenticator.token());

                    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                    verify(mockHttpClient, times(2)).send(captor.capture(), any());

                    List<HttpRequest> requests = captor.getAllValues();
                    HttpRequest identityRequest = requests.get(0);
                    assertEquals("https://localhost/idprovider/v1/auth/identitytoken", identityRequest.uri().toString());
                    assertEquals("application/x-www-form-urlencoded",
                        identityRequest.headers().firstValue("Content-Type").orElse(""));
                    assertEquals("grant_type=password&username=username&password=password&scope=openid",
                        Utils.bodyPublisherToString(identityRequest));

                    HttpRequest validateRequest = requests.get(1);
                    assertEquals("https://localhost/v1/preauth/validateAuth", validateRequest.uri().toString());
                    assertEquals("username", validateRequest.headers().firstValue("username").orElse(""));
                    assertEquals("my_super_token", validateRequest.headers().firstValue("iam-token").orElse(""));

                } catch (Exception e) {
                    fail(e);
                }
            });
        }


        @Test
        void should_fail_when_iam_enabled_without_password() {
            var ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .authMode(AuthMode.IAM)
                .build());
            assertEquals("IAM authentication requires a password", ex.getMessage());
        }

        @Test
        void should_refresh_token_when_existing_token_is_expired() throws Exception {

            var expiredResponse = Utils.expiredResponse();
            HttpResponse<String> expiredAccessToken = mock(HttpResponse.class);
            when(expiredAccessToken.statusCode()).thenReturn(200);
            when(expiredAccessToken.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");
            var validResponse = Utils.okResponse();
            HttpResponse<String> accessToken = mock(HttpResponse.class);
            when(accessToken.statusCode()).thenReturn(200);
            when(accessToken.body()).thenReturn("""
                {
                    "accessToken": "ok-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockHttpClient.<String>send(any(), any()))
                        .thenReturn(expiredResponse)
                        .thenReturn(expiredAccessToken)
                        .thenReturn(validResponse)
                        .thenReturn(accessToken);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    authenticator.token();

                    assertEquals("ok-access-token", authenticator.token());
                    verify(mockHttpClient, times(4)).send(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    @SuppressWarnings("unchecked")
    class Async {

        @Test
        void should_return_a_valid_token() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");

            withWatsonxServiceMock(() -> {
                when(mockHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                    .username("username")
                    .baseUrl(URI.create("http://my-url"))
                    .apiKey("api_key")
                    .build();

                assertEquals("access-token", assertDoesNotThrow(() -> authenticator.asyncToken().get()));
            });
        }

        @Test
        void should_use_iam_authentication() {

            // Prima risposta: identitytoken
            HttpResponse<String> identityResponse = Utils.okResponse();
            HttpResponse<String> validateResponse = mock(HttpResponse.class);
            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "access-token"
                }
                """);

            withWatsonxServiceMock(() -> {
                try {

                    when(mockHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(CompletableFuture.completedFuture(identityResponse))
                        .thenReturn(CompletableFuture.completedFuture(validateResponse));

                    ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    String token = authenticator.asyncToken().get();
                    assertEquals("access-token", token);
                    verify(mockHttpClient, times(2)).sendAsync(reqCaptor.capture(), any());

                    HttpRequest firstReq = reqCaptor.getAllValues().get(0);
                    HttpRequest secondReq = reqCaptor.getAllValues().get(1);

                    assertEquals("https://localhost/idprovider/v1/auth/identitytoken",
                        firstReq.uri().toString());
                    assertEquals("application/x-www-form-urlencoded",
                        firstReq.headers().firstValue("Content-Type").orElse(""));

                    String firstBody = Utils.bodyPublisherToString(firstReq);
                    assertEquals(
                        "grant_type=password&username=username&password=password&scope=openid",
                        firstBody
                    );

                    assertEquals("https://localhost/v1/preauth/validateAuth", secondReq.uri().toString());
                    assertEquals("username", secondReq.headers().firstValue("username").orElse(""));
                    assertEquals("my_super_token", secondReq.headers().firstValue("iam-token").orElse(""));

                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_cached_token() throws Exception {

            HttpResponse<String> response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockHttpClient.<String>sendAsync(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                    .username("username")
                    .baseUrl(URI.create("http://my-url"))
                    .apiKey("api_key")
                    .build();

                // Execute the http request.
                assertDoesNotThrow(() -> authenticator.asyncToken().get());
                // Get the value from the cache.
                assertDoesNotThrow(() -> authenticator.asyncToken().get());

                verify(mockHttpClient, times(1)).sendAsync(any(), any());
            });
        }

        @Test
        void should_use_cached_token_with_iam_configuration() throws Exception {

            HttpResponse<String> identityTokenResponse = Utils.okResponse();
            HttpResponse<String> validateResponse = mock(HttpResponse.class);
            when(validateResponse.statusCode()).thenReturn(200);
            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {

                    when(mockHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return CompletableFuture.completedFuture(identityTokenResponse);
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return CompletableFuture.completedFuture(validateResponse);
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .baseUrl("http://my-url/")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    var token = authenticator.asyncToken().get();
                    assertEquals("iam-access-token", token);

                    token = authenticator.asyncToken().get();
                    assertEquals("iam-access-token", token);

                    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                    verify(mockHttpClient, times(2)).sendAsync(captor.capture(), any());

                    List<HttpRequest> requests = captor.getAllValues();
                    long identityCalls = requests.stream()
                        .filter(r -> r.uri().toString().endsWith("/idprovider/v1/auth/identitytoken"))
                        .count();
                    long validateCalls = requests.stream()
                        .filter(r -> r.uri().toString().endsWith("/v1/preauth/validateAuth"))
                        .count();

                    assertEquals(1, identityCalls, "Identity token should be called once");
                    assertEquals(1, validateCalls, "ValidateAuth should be called once");

                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_the_correct_executors() throws Exception {

            mockHttpResponse = mock(HttpResponse.class);
            when(mockHttpResponse.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");

            withWatsonxServiceMock(() -> {

                try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {

                    when(mockHttpClient.sendAsync(any(), any(BodyHandler.class)))
                        .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));

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

                    AuthenticationProvider authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .apiKey("api_key")
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
