/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
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

    @Test
    @DisabledInNativeImage
    @MockitoSettings(strictness = Strictness.STRICT_STUBS)
    void should_preserve_interruption_when_authenticating_with_ibm_cloud() throws Exception {
        var transport = new CompletableFuture<HttpResponse<String>>();
        var acquisitionStarted = new CountDownLatch(1);
        when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
            acquisitionStarted.countDown();
            return transport;
        });
        var authenticator = newAuthenticator();
        var response = responseWithToken("my_super_token");
        var failure = new CompletableFuture<Throwable>();
        var interrupted = new AtomicBoolean();
        var caller = new Thread(() -> {
            try {
                authenticator.token();
                failure.completeExceptionally(new AssertionError("Expected the token wait to be interrupted"));
            } catch (Throwable error) {
                interrupted.set(Thread.currentThread().isInterrupted());
                failure.complete(error);
            }
        });
        caller.setDaemon(true);
        caller.start();
        try {
            assertTrue(acquisitionStarted.await(3, TimeUnit.SECONDS));
            awaitWaiting(caller);
            var otherWaiter = authenticator.tokenAsync();

            caller.interrupt();

            var error = assertInstanceOf(RuntimeException.class, failure.get(3, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, error.getCause());
            assertTrue(interrupted.get());
            assertFalse(transport.isCancelled());
            assertFalse(otherWaiter.isDone());

            transport.complete(response);

            assertEquals("my_super_token", otherWaiter.get(3, TimeUnit.SECONDS));
            assertEquals("my_super_token", authenticator.token());
            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
        } finally {
            transport.complete(response);
            caller.interrupt();
            caller.join(3000);
            assertFalse(caller.isAlive());
        }
    }

    @Nested
    @DisabledInNativeImage
    class Sync {

        @Test
        void should_return_a_valid_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any())).thenReturn(completedFuture(response));

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

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(failedFuture(new WatsonxException(Utils.WRONG_RESPONSE, 400, null)));

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
                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(completedFuture(response));

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
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
        void should_refresh_token_when_existing_token_is_expired() throws Exception {

            var expiredResponse = Utils.expiredResponse();

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(completedFuture(expiredResponse));

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    authenticator.token();

                    assertEquals("my_super_token", authenticator.token());
                    verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
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
                    when(mockSecureHttpClient.<String>sendAsync(httpRequest.capture(), any())).thenReturn(completedFuture(response));

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
        void should_throw_watsonx_exception_when_error_response_received() throws Exception {
            var errorMessage = """
                {
                    "errorCode": "BXNIM0415E",
                    "errorMessage": "Provided API key could not be found.",
                    "errorDetails": "More details",
                    "context": {
                        "requestId": "xxx"
                    }
                }""";

            var responseInfo = mock(ResponseInfo.class);
            when(responseInfo.statusCode()).thenReturn(400);
            when(responseInfo.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (name, value) -> true));

            withWatsonxServiceMock(() -> {
                try {
                    when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
                        BodyHandler<String> handler = invocation.getArgument(1);
                        var subscriber = handler.apply(responseInfo);
                        subscriber.onSubscribe(new Subscription() {
                            @Override
                            public void request(long count) {}

                            @Override
                            public void cancel() {}
                        });
                        subscriber.onNext(List.of(ByteBuffer.wrap(errorMessage.getBytes(StandardCharsets.UTF_8))));
                        subscriber.onComplete();
                        return subscriber.getBody().<HttpResponse<String>>thenApply(body -> {
                            throw new AssertionError("Expected the IAM error response to fail");
                        }).toCompletableFuture();
                    });

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
                    when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                        .thenReturn(failedFuture(new IOException("IOException")))
                        .thenReturn(completedFuture(Utils.okResponse()));

                    Authenticator authenticator = IBMCloudAuthenticator.builder()
                        .apiKey("my_super_api_key")
                        .build();

                    var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                    assertInstanceOf(IOException.class, ex.getCause());
                    assertEquals(ex.getCause().getMessage(), "IOException");
                    assertEquals("my_super_token", authenticator.token());
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }

    @Nested
    @DisabledInNativeImage
    class Async {

        @Test
        void should_cache_token_when_all_waiters_are_cancelled() {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(transport);

            withWatsonxServiceMock(() -> {
                var authenticator = IBMCloudAuthenticator.builder().apiKey("api-key").build();
                var first = authenticator.tokenAsync();
                var second = authenticator.tokenAsync();

                assertTrue(first.cancel(true));
                assertTrue(second.cancel(true));

                assertFalse(transport.isCancelled());
                assertThrows(CancellationException.class, first::join);
                assertThrows(CancellationException.class, second::join);

                transport.complete(Utils.okResponse());

                assertEquals("my_super_token", authenticator.token());
                verify(mockSecureHttpClient).sendAsync(any(), any());
            });
        }

        @Test
        void should_return_a_valid_token() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                assertEquals("my_super_token", assertDoesNotThrow(() -> authenticator.tokenAsync().get()));
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
                when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                // Execute the http request.
                assertDoesNotThrow(() -> authenticator.tokenAsync().get());
                // Get the value from the cache.
                assertDoesNotThrow(() -> authenticator.tokenAsync().get());

                verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
            });
        }

        @Test
        void should_use_custom_parameters() throws Exception {

            var response = Utils.okResponse();

            withWatsonxServiceMock(() -> {
                when(mockSecureHttpClient.<String>sendAsync(mockHttpRequest.capture(), any()))
                    .thenReturn(CompletableFuture.completedFuture(response));

                Authenticator authenticator = IBMCloudAuthenticator.builder()
                    .grantType("new_grant_type")
                    .timeout(Duration.ofSeconds(1))
                    .baseUrl(URI.create("http://mytest.com"))
                    .apiKey("my_super_api_key")
                    .build();

                assertEquals("my_super_token", assertDoesNotThrow(() -> authenticator.tokenAsync().get()));
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

                    when(mockSecureHttpClient.sendAsync(any(), any(BodyHandler.class)))
                        .thenReturn(completedFuture(mockHttpResponse));

                    Executor ioExecutor = Executors.newSingleThreadExecutor(r -> {
                        var thread = new Thread(r, "io-thread");
                        thread.setDaemon(true);
                        return thread;
                    });

                    mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

                    var authenticator = IBMCloudAuthenticator.builder()
                        .grantType("new_grant_type")
                        .timeout(Duration.ofSeconds(1))
                        .baseUrl(URI.create("http://mytest.com"))
                        .apiKey("my_super_api_key")
                        .build();

                    // The token payload is tiny, so it is parsed on the IO executor: no cpuExecutor hop is expected.
                    assertDoesNotThrow(() -> authenticator.tokenAsync().get(3, TimeUnit.SECONDS));

                    mockedStatic.verify(ExecutorProvider::cpuExecutor, never());
                }
            });
        }
    }

    @Nested
    @DisabledInNativeImage
    class SharedAcquisition {

        @Test
        void should_share_one_acquisition_among_twenty_concurrent_callers() throws Exception {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(transport);
            var authenticator = newAuthenticator();

            var waiters = concurrentWaiters(authenticator, 20);

            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
            waiters.forEach(waiter -> assertFalse(waiter.isDone()));

            transport.complete(Utils.okResponse());

            for (var waiter : waiters)
                assertEquals("my_super_token", waiter.get(3, TimeUnit.SECONDS));
            assertEquals("my_super_token", authenticator.token());
            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
        }

        @Test
        void should_share_one_refresh_among_twenty_concurrent_callers() throws Exception {
            var refresh = new CompletableFuture<HttpResponse<String>>();
            var expired = Utils.expiredResponse();
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(completedFuture(expired))
                .thenReturn(refresh);
            var authenticator = newAuthenticator();
            assertEquals("my_super_token", authenticator.token());

            var waiters = concurrentWaiters(authenticator, 20);

            verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
            waiters.forEach(waiter -> assertFalse(waiter.isDone()));

            refresh.complete(responseWithToken("refreshed-token"));

            for (var waiter : waiters)
                assertEquals("refreshed-token", waiter.get(3, TimeUnit.SECONDS));
            assertEquals("refreshed-token", authenticator.token());
            verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
        }

        @Test
        void should_share_acquisition_when_async_caller_starts_first() throws Exception {
            assertMixedAcquisition(false);
        }

        @Test
        void should_share_acquisition_when_sync_caller_starts_first() throws Exception {
            assertMixedAcquisition(true);
        }

        @Test
        void should_complete_other_waiters_when_one_waiter_is_cancelled() throws Exception {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(transport);
            var authenticator = newAuthenticator();
            var cancelled = authenticator.tokenAsync();
            var surviving = authenticator.tokenAsync();
            assertNotSame(cancelled, surviving);

            assertTrue(cancelled.cancel(true));

            assertFalse(transport.isCancelled());
            assertFalse(surviving.isDone());
            assertThrows(CancellationException.class, cancelled::join);

            transport.complete(Utils.okResponse());

            assertEquals("my_super_token", surviving.get(3, TimeUnit.SECONDS));
            assertEquals("my_super_token", authenticator.token());
            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
        }

        @Test
        void should_fail_all_waiters_and_allow_another_acquisition() throws Exception {
            var transport = new CompletableFuture<HttpResponse<String>>();
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenReturn(transport)
                .thenReturn(completedFuture(Utils.okResponse()));
            var authenticator = newAuthenticator();
            var waiters = concurrentWaiters(authenticator, 20);
            var failure = new IOException("IAM unavailable");

            transport.completeExceptionally(failure);

            for (var waiter : waiters) {
                var error = assertThrows(ExecutionException.class, () -> waiter.get(3, TimeUnit.SECONDS));
                assertSame(failure, error.getCause());
            }
            assertEquals("my_super_token", authenticator.tokenAsync().get(3, TimeUnit.SECONDS));
            verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
        }

        @Test
        void should_recover_when_the_transport_rejects_acquisition_synchronously() throws Exception {
            var failure = new RejectedExecutionException("Token acquisition rejected");
            when(mockSecureHttpClient.<String>sendAsync(any(), any()))
                .thenThrow(failure)
                .thenReturn(completedFuture(Utils.okResponse()));
            var authenticator = newAuthenticator();

            var waiter = assertDoesNotThrow(authenticator::tokenAsync);

            var error = assertThrows(ExecutionException.class, () -> waiter.get(3, TimeUnit.SECONDS));
            assertSame(failure, error.getCause());
            assertEquals("my_super_token", authenticator.tokenAsync().get(3, TimeUnit.SECONDS));
            verify(mockSecureHttpClient, times(2)).sendAsync(any(), any());
        }
    }

    @Test
    @DisabledInNativeImage
    void should_return_the_correct_scheme() {
        var authenticator = IBMCloudAuthenticator.withKey("api-key");
        assertEquals("Bearer", authenticator.scheme());
    }

    private IBMCloudAuthenticator newAuthenticator() {
        var authenticator = new AtomicReference<IBMCloudAuthenticator>();
        withWatsonxServiceMock(() -> authenticator.set(IBMCloudAuthenticator.builder().apiKey("api-key").build()));
        return authenticator.get();
    }

    private List<CompletableFuture<String>> concurrentWaiters(Authenticator authenticator, int count) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        var registrations = new ArrayList<Future<CompletableFuture<String>>>();
        try {
            for (int i = 0; i < count; i++) {
                registrations.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(3, TimeUnit.SECONDS));
                    return authenticator.tokenAsync();
                }));
            }
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();

            var waiters = new ArrayList<CompletableFuture<String>>();
            for (var registration : registrations)
                waiters.add(registration.get(3, TimeUnit.SECONDS));
            return waiters;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    private void assertMixedAcquisition(boolean synchronousFirst) throws Exception {
        var transport = new CompletableFuture<HttpResponse<String>>();
        var acquisitionStarted = new CountDownLatch(1);
        when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenAnswer(invocation -> {
            acquisitionStarted.countDown();
            return transport;
        });
        var authenticator = newAuthenticator();
        var synchronous = new CompletableFuture<String>();
        var caller = new Thread(() -> {
            try {
                synchronous.complete(authenticator.token());
            } catch (Throwable error) {
                synchronous.completeExceptionally(error);
            }
        });
        caller.setDaemon(true);
        try {
            CompletableFuture<String> asynchronous;
            if (synchronousFirst) {
                caller.start();
                assertTrue(acquisitionStarted.await(3, TimeUnit.SECONDS));
                awaitWaiting(caller);
                asynchronous = authenticator.tokenAsync();
            } else {
                asynchronous = authenticator.tokenAsync();
                caller.start();
                awaitWaiting(caller);
            }

            assertFalse(asynchronous.isDone());
            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());

            transport.complete(Utils.okResponse());

            assertEquals("my_super_token", asynchronous.get(3, TimeUnit.SECONDS));
            assertEquals("my_super_token", synchronous.get(3, TimeUnit.SECONDS));
            assertEquals("my_super_token", authenticator.token());
            verify(mockSecureHttpClient, times(1)).sendAsync(any(), any());
        } finally {
            transport.complete(Utils.okResponse());
            caller.interrupt();
            caller.join(3000);
            assertFalse(caller.isAlive());
        }
    }

    private void awaitWaiting(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (thread.getState() != Thread.State.WAITING && thread.isAlive() && System.nanoTime() < deadline)
            Thread.sleep(1);
        assertEquals(Thread.State.WAITING, thread.getState(), "Expected the synchronous caller to wait for the shared acquisition");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> responseWithToken(String token) {
        HttpResponse<String> response = mock(HttpResponse.class);
        var body = Utils.OK_RESPONSE.formatted(Instant.now().plusSeconds(3600).getEpochSecond()).replace("my_super_token", token);
        when(response.body()).thenReturn(body);
        return response;
    }
}
