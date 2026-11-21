/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.ibm.watsonx.ai.core.auth.cp4d.AuthMode.IAM;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.utils.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.cp4d.AuthMode;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class CP4DAuthenticationTest extends AbstractWatsonxTest {

    @Test
    @DisabledInNativeImage
    void should_preserve_interruption_when_authenticating_with_cp4d() throws Exception {
        for (var mode : new AuthMode[] { AuthMode.LEGACY, AuthMode.IAM }) {
            var transport = new CompletableFuture<HttpResponse<String>>();
            var acquired = mode == AuthMode.IAM ? identityResponse(false) : tokenResponse("access-token");
            var validated = mode == AuthMode.IAM ? tokenResponse("access-token") : acquired;
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(transport)
                .thenReturn(completedFuture(validated));
            withWatsonxServiceMock(() -> {
                var authenticator = CP4DAuthenticator.builder().baseUrl("http://localhost")
                    .username("user").password("password").authMode(mode).build();
                var independent = authenticator.tokenAsync();
                var failure = new AtomicReference<Throwable>();
                var interrupted = new AtomicBoolean();
                var waiter = new Thread(() -> {
                    try {
                        authenticator.token();
                    } catch (RuntimeException e) {
                        failure.set(e.getCause());
                        interrupted.set(Thread.currentThread().isInterrupted());
                    }
                });
                try {
                    waiter.start();
                    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                    while (waiter.getState() != Thread.State.WAITING && waiter.isAlive() && System.nanoTime() < deadline)
                        Thread.yield();
                    assertEquals(Thread.State.WAITING, waiter.getState());
                    waiter.interrupt();
                    assertDoesNotThrow(() -> waiter.join(3000));
                    assertFalse(waiter.isAlive());
                    assertInstanceOf(InterruptedException.class, failure.get());
                    assertTrue(interrupted.get());
                    assertFalse(transport.isDone());
                    transport.complete(acquired);
                    assertEquals("access-token", assertDoesNotThrow(() -> independent.get(3, TimeUnit.SECONDS)));
                    assertEquals("access-token", authenticator.token());
                } finally {
                    transport.complete(acquired);
                    waiter.interrupt();
                    assertDoesNotThrow(() -> waiter.join(3000));
                }
            });
        }
        verify(mockSecureHttpClient, never()).send(any(), any());
    }

    @Nested
    @DisabledInNativeImage
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


            withWatsonxServiceMock(() -> {

                try {
                    when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                        .thenReturn(completedFuture(response));

                    Authenticator authenticator = CP4DAuthenticator.builder()
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


            withWatsonxServiceMock(() -> {

                try {
                    when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                        .thenReturn(completedFuture(response));

                    Authenticator authenticator = CP4DAuthenticator.builder()
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

            var message = """
                {
                    "code": 602,
                    "message": "properties.username in body is required"
                }""";
            var expected = new WatsonxException(message, 500, null);

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(failedFuture(expected));

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .apiKey("my_super_api_key")
                        .timeout(Duration.ofSeconds(10))
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertSame(expected, ex);
                    assertEquals(500, expected.statusCode());
                    assertEquals(message, ex.getMessage());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_cached_token() throws Exception {

            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("""
                {
                    "_messageCode_": "200",
                    "message": "Success",
                    "token": "access-token"
                }""");


            // NO IAM.
            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(completedFuture(response));

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl("http://my-url/")
                        .apiKey("my_super_api_key")
                        .build();

                    // Execute the http request.
                    authenticator.token();
                    // Get the value from the cache.
                    authenticator.token();

                    verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_cached_token_with_iam_configuration() throws Exception {

            HttpResponse<String> identityTokenResponse = identityResponse(false);
            HttpResponse<String> validateResponse = mock(HttpResponse.class);

            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {

                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return completedFuture(identityTokenResponse);
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return completedFuture(validateResponse);
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    Authenticator authenticator = CP4DAuthenticator.builder()
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
                    verify(mockSecureHttpClient, times(2)).sendAsync(captor.capture(), any());

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
        void should_fail_if_mandatory_parameters_are_not_set_for_legacy_authentication() throws Exception {

            var ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .baseUrl(URI.create("http://my-url"))
                .build()
            );
            assertEquals("Username must be provided", ex.getMessage());

            ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .build()
            );
            assertEquals("Either password or apiKey must be provided", ex.getMessage());
        }

        @Test
        void should_fail_if_mandatory_parameters_are_not_set_for_iam_authentication() {

            var ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .authMode(AuthMode.IAM)
                .build()
                .token());
            assertEquals("Username must be provided", ex.getMessage());

            ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .authMode(AuthMode.IAM)
                .build()
                .token());
            assertEquals("Password must be provided", ex.getMessage());
        }

        @Test
        void should_fail_if_mandatory_parameters_are_not_set_for_zen_authentication() {

            var ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .baseUrl(URI.create("http://my-url"))
                .authMode(AuthMode.ZEN_API_KEY)
                .build()
                .token()
            );
            assertEquals("Username must be provided", ex.getMessage());

            ex = assertThrows(NullPointerException.class, () -> CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .authMode(AuthMode.ZEN_API_KEY)
                .build()
                .token()
            );
            assertEquals("Either password or apiKey must be provided", ex.getMessage());
        }

        @Test
        void should_catch_the_io_exception_correctly_for_legacy_authentication() throws Exception {
            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(failedFuture(new IOException("IOException")));

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl("http://my-url/")
                        .apiKey("my_api_key")
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertInstanceOf(IOException.class, ex.getCause());
                    assertEquals(ex.getCause().getMessage(), "IOException");
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_catch_the_io_exception_correctly_for_iam_authentication() throws Exception {
            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(failedFuture(new IOException("IOException")));

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .password("password")
                        .baseUrl("http://my-url/")
                        .apiKey("my_api_key")
                        .authMode(IAM)
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertInstanceOf(IOException.class, ex.getCause());
                    assertEquals(ex.getCause().getMessage(), "IOException");
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_iam_authentication() {

            var identityTokenResponse = identityResponse(false);
            HttpResponse<String> validateResponse = mock(HttpResponse.class);

            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return completedFuture(identityTokenResponse);
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return completedFuture(validateResponse);
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    assertEquals("iam-access-token", authenticator.token());

                    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                    verify(mockSecureHttpClient, times(2)).sendAsync(captor.capture(), any());

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
        void should_refresh_token_when_existing_token_is_expired() throws Exception {

            var expiredResponse = identityResponse(true);
            HttpResponse<String> expiredAccessToken = mock(HttpResponse.class);

            when(expiredAccessToken.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");
            var validResponse = identityResponse(false);
            HttpResponse<String> accessToken = mock(HttpResponse.class);

            when(accessToken.body()).thenReturn("""
                {
                    "accessToken": "ok-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(completedFuture(expiredResponse))
                        .thenReturn(completedFuture(expiredAccessToken))
                        .thenReturn(completedFuture(validResponse))
                        .thenReturn(completedFuture(accessToken));

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    authenticator.token();

                    assertEquals("ok-access-token", authenticator.token());
                    verify(mockSecureHttpClient, times(4)).sendAsync(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_treat_iam_token_without_expiration_as_expired() {

            HttpResponse<String> identityResponse = mock(HttpResponse.class);

            when(identityResponse.body()).thenReturn("""
                {
                    "access_token": "identity-token",
                    "scope": "ibm openid"
                }""");

            HttpResponse<String> validateResponse = mock(HttpResponse.class);

            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return completedFuture(identityResponse);
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return completedFuture(validateResponse);
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    assertEquals("iam-access-token", assertDoesNotThrow(() -> authenticator.token()));
                    // A token without expiration is always "expired", so the second call re-runs the full flow.
                    assertEquals("iam-access-token", assertDoesNotThrow(() -> authenticator.token()));
                    verify(mockSecureHttpClient, times(4)).sendAsync(any(), any());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }

        @Test
        void should_use_zen_api_key_authentication_mode() {

            Authenticator authenticator = CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .apiKey("api_key")
                .authMode(AuthMode.ZEN_API_KEY)
                .build();

            var encoded = Base64.encodeBase64String("username:api_key".getBytes());
            assertEquals(encoded, authenticator.token());

            authenticator = CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .password("password")
                .authMode(AuthMode.ZEN_API_KEY)
                .build();

            encoded = Base64.encodeBase64String("username:password".getBytes());
            assertEquals(encoded, authenticator.token());
        }
    }

    @Nested
    @DisabledInNativeImage
    @MockitoSettings(strictness = Strictness.LENIENT)
    @SuppressWarnings("unchecked")
    class Async {

        @Test
        void should_fill_the_legacy_cache_when_all_token_waiters_are_cancelled() {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(transport);

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.LEGACY);
                var first = authenticator.tokenAsync();
                var second = authenticator.tokenAsync();

                assertCancellation(first, transport);
                assertCancellation(second, transport);
                transport.complete(tokenResponse("access-token"));

                assertEquals("access-token", authenticator.token());
                assertEquals("access-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                verify(mockSecureHttpClient).sendAsync(any(), any());
            });
        }

        @Test
        void should_keep_iam_identity_acquisition_when_one_waiter_is_cancelled() {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(transport)
                .thenReturn(completedFuture(tokenResponse("access-token")));

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.IAM);
                var token = authenticator.tokenAsync();
                var independent = authenticator.tokenAsync();

                assertCancellation(token, transport);
                transport.complete(identityResponse(false));

                assertEquals("access-token", assertDoesNotThrow(() -> independent.get(3, TimeUnit.SECONDS)));
                assertEquals("access-token", authenticator.token());
                verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
            });
        }

        @Test
        void should_fill_the_iam_cache_when_all_waiters_cancel_during_validation() {
            var validation = new CompletableFuture<HttpResponse<String>>();
            var validationStarted = new CountDownLatch(1);
            var identityResponse = identityResponse(false);
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(completedFuture(identityResponse))
                .thenAnswer(invocation -> {
                    validationStarted.countDown();
                    return validation;
                });

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.IAM);
                var token = authenticator.tokenAsync();
                assertDoesNotThrow(() -> assertTrue(validationStarted.await(3, TimeUnit.SECONDS)));

                assertCancellation(token, validation);
                validation.complete(tokenResponse("access-token"));

                assertEquals("access-token", authenticator.token());
                assertEquals("access-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
            });
        }

        @Test
        void should_continue_iam_validation_after_waiter_cancellation_during_identity_parsing() throws Exception {
            var identity = new CompletableFuture<HttpResponse<String>>();
            var identityResponse = identityResponse(false);
            var identityBody = identityResponse.body();
            var parsingStarted = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var worker = Executors.newSingleThreadExecutor();
            when(identityResponse.body()).thenAnswer(invocation -> {
                parsingStarted.countDown();
                assertTrue(release.await(3, TimeUnit.SECONDS));
                return identityBody;
            });
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(identity)
                .thenReturn(completedFuture(tokenResponse("access-token")));

            try {
                withWatsonxServiceMock(() -> {
                    try (var executors = mockStatic(ExecutorProvider.class)) {
                        executors.when(ExecutorProvider::ioExecutor).thenReturn((Executor) Runnable::run);
                        var authenticator = authenticator(AuthMode.IAM);
                        var token = authenticator.tokenAsync();
                        var independent = authenticator.tokenAsync();
                        var completingIdentity = worker.submit(() -> identity.complete(identityResponse));
                        assertDoesNotThrow(() -> assertTrue(parsingStarted.await(3, TimeUnit.SECONDS)));

                        assertTrue(token.cancel(true));
                        release.countDown();
                        assertDoesNotThrow(() -> completingIdentity.get(3, TimeUnit.SECONDS));

                        assertThrows(CancellationException.class, token::join);
                        assertEquals("access-token", assertDoesNotThrow(() -> independent.get(3, TimeUnit.SECONDS)));
                        assertEquals("access-token", authenticator.token());
                        verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
                    }
                });
            } finally {
                release.countDown();
                worker.shutdownNow();
            }
        }

        @Test
        void should_keep_iam_validation_transport_registered_after_waiter_cancellation() throws Exception {
            var identity = new CompletableFuture<HttpResponse<String>>();
            var validation = new CompletableFuture<HttpResponse<String>>();
            var identityResponse = identityResponse(false);
            var validationStarted = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var worker = Executors.newSingleThreadExecutor();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(identity).thenAnswer(invocation -> {
                validationStarted.countDown();
                assertTrue(release.await(3, TimeUnit.SECONDS));
                return validation;
            });

            try {
                withWatsonxServiceMock(() -> {
                    try (var executors = mockStatic(ExecutorProvider.class)) {
                        executors.when(ExecutorProvider::ioExecutor).thenReturn((Executor) Runnable::run);
                        var authenticator = authenticator(AuthMode.IAM);
                        var token = authenticator.tokenAsync();
                        var completingIdentity = worker.submit(() -> identity.complete(identityResponse));
                        assertDoesNotThrow(() -> assertTrue(validationStarted.await(3, TimeUnit.SECONDS)));

                        assertTrue(token.cancel(true));
                        release.countDown();
                        assertDoesNotThrow(() -> completingIdentity.get(3, TimeUnit.SECONDS));

                        assertFalse(validation.isDone());
                        validation.complete(tokenResponse("access-token"));
                        assertThrows(CancellationException.class, token::join);
                        assertEquals("access-token", authenticator.token());
                        verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
                    }
                });
            } finally {
                release.countDown();
                worker.shutdownNow();
            }
        }

        private CP4DAuthenticator authenticator(AuthMode mode) {
            return CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .password("password")
                .authMode(mode)
                .build();
        }

        private void assertCancellation(CompletableFuture<String> token, CompletableFuture<HttpResponse<String>> transport) {
            assertTrue(token.cancel(true));
            assertFalse(transport.isDone());
            assertThrows(CancellationException.class, token::join);
        }

        @Test
        void should_share_legacy_token_acquisition_between_twenty_pending_callers() {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(transport);

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.LEGACY);
                var waiters = new ArrayList<CompletableFuture<String>>();
                for (var i = 0; i < 20; i++)
                    waiters.add(authenticator.tokenAsync());

                verify(mockSecureHttpClient).sendAsync(any(), any());
                transport.complete(tokenResponse("shared-legacy-token"));

                for (var waiter : waiters)
                    assertEquals("shared-legacy-token", assertDoesNotThrow(() -> waiter.get(3, TimeUnit.SECONDS)));
                assertEquals("shared-legacy-token", authenticator.token());
                assertEquals("shared-legacy-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                verify(mockSecureHttpClient).sendAsync(any(), any());
            });
        }

        @Test
        void should_share_both_iam_stages_between_twenty_pending_callers() {
            var identity = new CompletableFuture<HttpResponse<String>>();
            var validation = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(identity).thenReturn(validation);

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.IAM);
                var waiters = new ArrayList<CompletableFuture<String>>();
                for (var i = 0; i < 20; i++)
                    waiters.add(authenticator.tokenAsync());

                verify(mockSecureHttpClient).sendAsync(any(), any());
                identity.complete(identityResponse(false));
                verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
                assertTrue(waiters.stream().noneMatch(CompletableFuture::isDone));
                validation.complete(tokenResponse("shared-iam-token"));

                for (var waiter : waiters)
                    assertEquals("shared-iam-token", assertDoesNotThrow(() -> waiter.get(3, TimeUnit.SECONDS)));
                assertEquals("shared-iam-token", authenticator.token());
                assertEquals("shared-iam-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
            });
        }

        @Test
        void should_share_refresh_of_an_expired_iam_token_between_twenty_pending_callers() {
            var identity = new CompletableFuture<HttpResponse<String>>();
            var validation = new CompletableFuture<HttpResponse<String>>();
            var expiredIdentity = identityResponse(true);
            var expiredValidation = tokenResponse("expired-iam-token");
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(completedFuture(expiredIdentity))
                .thenReturn(completedFuture(expiredValidation))
                .thenReturn(identity)
                .thenReturn(validation);

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.IAM);
                assertEquals("expired-iam-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                var waiters = new ArrayList<CompletableFuture<String>>();
                for (var i = 0; i < 20; i++)
                    waiters.add(authenticator.tokenAsync());

                verify(mockSecureHttpClient, times(3)).sendAsync(any(), any());
                identity.complete(identityResponse(false));
                validation.complete(tokenResponse("refreshed-iam-token"));

                for (var waiter : waiters)
                    assertEquals("refreshed-iam-token", assertDoesNotThrow(() -> waiter.get(3, TimeUnit.SECONDS)));
                assertEquals("refreshed-iam-token", authenticator.token());
                verify(mockSecureHttpClient, times(4)).sendAsync(any(), any());
            });
        }

        @Test
        void should_allow_acquisition_after_a_shared_legacy_failure() {
            var transport = new CompletableFuture<HttpResponse<String>>();
            var failure = new IOException("Token endpoint unavailable");
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(transport)
                .thenReturn(completedFuture(tokenResponse("recovered-token")));

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.LEGACY);
                var first = authenticator.tokenAsync();
                var second = authenticator.tokenAsync();
                transport.completeExceptionally(failure);

                assertSame(failure, assertThrows(ExecutionException.class,
                    () -> first.get(3, TimeUnit.SECONDS)).getCause());
                assertSame(failure, assertThrows(ExecutionException.class,
                    () -> second.get(3, TimeUnit.SECONDS)).getCause());
                assertEquals("recovered-token", authenticator.token());
                verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
            });
        }

        @Test
        void should_not_send_a_cancelled_service_request_after_shared_cp4d_authentication() {
            var login = new CompletableFuture<HttpResponse<String>>();
            var serviceResponse = tokenResponse("service-response");
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                return request.uri().getPath().equals("/icp4d-api/v1/authorize")
                    ? login
                    : completedFuture(serviceResponse);
            });

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.LEGACY);
                var client = HttpClientFactory.createAsync(authenticator, mockSecureHttpClient, LogMode.DISABLED);
                var request = HttpRequest.newBuilder(URI.create("https://localhost/resource")).GET().build();
                var cancelled = client.send(request, BodyHandlers.ofString());
                var independent = client.send(request, BodyHandlers.ofString());

                assertTrue(cancelled.cancel(true));
                assertFalse(login.isDone());
                login.complete(tokenResponse("shared-service-token"));

                assertSame(serviceResponse, assertDoesNotThrow(() -> independent.get(3, TimeUnit.SECONDS)));
                assertThrows(CancellationException.class, cancelled::join);
                var requests = ArgumentCaptor.forClass(HttpRequest.class);
                verify(mockSecureHttpClient, times(2)).sendAsync(requests.capture(), any());
                var serviceRequests = requests.getAllValues().stream()
                    .filter(sent -> sent.uri().getPath().equals("/resource"))
                    .toList();
                assertEquals(1, serviceRequests.size());
                assertEquals("Bearer shared-service-token", serviceRequests.get(0).headers().firstValue("Authorization").orElseThrow());

                assertSame(serviceResponse, assertDoesNotThrow(() -> client.send(request, BodyHandlers.ofString())
                    .get(3, TimeUnit.SECONDS)));
                verify(mockSecureHttpClient, times(3)).sendAsync(any(), any());
            });
        }

        @Test
        void should_cache_cp4d_authentication_after_all_service_requests_are_cancelled() {
            var login = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(login);

            withWatsonxServiceMock(() -> {
                var authenticator = authenticator(AuthMode.LEGACY);
                var client = HttpClientFactory.createAsync(authenticator, mockSecureHttpClient, LogMode.DISABLED);
                var request = HttpRequest.newBuilder(URI.create("https://localhost/resource")).GET().build();
                var first = client.send(request, BodyHandlers.ofString());
                var second = client.send(request, BodyHandlers.ofString());

                assertTrue(first.cancel(true));
                assertTrue(second.cancel(false));
                assertFalse(login.isDone());
                login.complete(tokenResponse("cached-service-token"));

                assertEquals("cached-service-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS)));
                assertThrows(CancellationException.class, first::join);
                assertThrows(CancellationException.class, second::join);
                var requests = ArgumentCaptor.forClass(HttpRequest.class);
                verify(mockSecureHttpClient).sendAsync(requests.capture(), any());
                assertEquals("/icp4d-api/v1/authorize", requests.getValue().uri().getPath());
            });
        }

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
                when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(completedFuture(response));

                Authenticator authenticator = CP4DAuthenticator.builder()
                    .username("username")
                    .baseUrl(URI.create("http://my-url"))
                    .apiKey("api_key")
                    .build();

                assertEquals("access-token", assertDoesNotThrow(() -> authenticator.tokenAsync().get()));
            });
        }

        @Test
        void should_use_iam_authentication() {

            // Prima risposta: identitytoken
            HttpResponse<String> identityResponse = identityResponse(false);
            HttpResponse<String> validateResponse = mock(HttpResponse.class);
            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "access-token"
                }
                """);

            withWatsonxServiceMock(() -> {
                try {

                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(completedFuture(identityResponse))
                        .thenReturn(completedFuture(validateResponse));

                    ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .baseUrl("https://localhost")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    String token = authenticator.tokenAsync().get();
                    assertEquals("access-token", token);
                    verify(mockSecureHttpClient, times(2)).sendAsync(reqCaptor.capture(), any());

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

            HttpResponse<String> response = identityResponse(false);

            withWatsonxServiceMock(() -> {
                when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                    .thenReturn(completedFuture(response));

                Authenticator authenticator = CP4DAuthenticator.builder()
                    .username("username")
                    .baseUrl(URI.create("http://my-url"))
                    .apiKey("api_key")
                    .build();

                // Execute the http request.
                assertDoesNotThrow(() -> authenticator.tokenAsync().get());
                // Get the value from the cache.
                assertDoesNotThrow(() -> authenticator.tokenAsync().get());

                verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
            });
        }

        @Test
        void should_use_cached_token_with_iam_configuration() throws Exception {

            HttpResponse<String> identityTokenResponse = identityResponse(false);
            HttpResponse<String> validateResponse = mock(HttpResponse.class);

            when(validateResponse.body()).thenReturn("""
                {
                    "accessToken": "iam-access-token"
                }""");

            withWatsonxServiceMock(() -> {
                try {

                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        String url = invocation.getArgument(0).toString();
                        if (url.endsWith("/idprovider/v1/auth/identitytoken POST"))
                            return completedFuture(identityTokenResponse);
                        else if (url.endsWith("/v1/preauth/validateAuth GET"))
                            return completedFuture(validateResponse);
                        else
                            throw new RuntimeException("Unexpected URL: " + url);
                    });

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .baseUrl("http://my-url/")
                        .username("username")
                        .password("password")
                        .authMode(AuthMode.IAM)
                        .build();

                    var token = authenticator.tokenAsync().get();
                    assertEquals("iam-access-token", token);

                    token = authenticator.tokenAsync().get();
                    assertEquals("iam-access-token", token);

                    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                    verify(mockSecureHttpClient, times(2)).sendAsync(captor.capture(), any());

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
        void should_use_zen_api_key_authentication_mode() {

            CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
                .username("username")
                .baseUrl(URI.create("http://my-url"))
                .apiKey("api_key")
                .authMode(AuthMode.ZEN_API_KEY)
                .build();

            var encoded = Base64.encodeBase64String("username:api_key".getBytes());
            assertEquals(encoded, assertDoesNotThrow(() -> authenticator.tokenAsync().get()));
            assertTrue(authenticator.isAuthMode(AuthMode.ZEN_API_KEY));
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

                    when(mockSecureHttpClient.sendAsync(any(), any(BodyHandler.class)))
                        .thenReturn(completedFuture(mockHttpResponse));

                    Executor ioExecutor = Executors.newSingleThreadExecutor(r -> {
                        var thread = new Thread(r, "io-thread");
                        thread.setDaemon(true);
                        return thread;
                    });

                    mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

                    Authenticator authenticator = CP4DAuthenticator.builder()
                        .username("username")
                        .baseUrl(URI.create("http://my-url"))
                        .apiKey("api_key")
                        .build();

                    // The token payload is tiny, so it is parsed on the IO executor: no cpuExecutor hop is expected.
                    assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS));

                    mockedStatic.verify(ExecutorProvider::cpuExecutor, never());
                }
            });
        }
    }

    @Test
    @DisabledInNativeImage
    void should_return_the_correct_scheme() {

        CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
            .username("username")
            .baseUrl(URI.create("http://my-url"))
            .apiKey("api_key")
            .authMode(AuthMode.ZEN_API_KEY)
            .build();

        assertEquals("ZenApiKey", authenticator.scheme());

        authenticator = CP4DAuthenticator.builder()
            .baseUrl("http://my-url/")
            .username("username")
            .password("password")
            .authMode(AuthMode.IAM)
            .build();

        assertEquals("Bearer", authenticator.scheme());

        authenticator = CP4DAuthenticator.builder()
            .username("username")
            .baseUrl(URI.create("http://my-url"))
            .apiKey("my_super_api_key")
            .timeout(Duration.ofSeconds(10))
            .authMode(AuthMode.LEGACY)
            .build();

        assertEquals("Bearer", authenticator.scheme());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> identityResponse(boolean expired) {
        HttpResponse<String> response = mock(HttpResponse.class);
        var expiration = Instant.now().plusSeconds(expired ? -3600 : 3600).getEpochSecond();
        when(response.body()).thenReturn(Utils.OK_RESPONSE.formatted(expiration));
        return response;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> tokenResponse(String token) {
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.body()).thenReturn("{\"token\":\"%s\"}".formatted(token));
        return response;
    }
}
