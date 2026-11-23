/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.BaseHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisabledInNativeImage
public class RetryInterceptorTest {

    @Mock
    HttpClient httpClient;

    @Mock
    HttpRequest httpRequest;

    @Mock
    HttpResponse<String> httpResponse;

    @Mock
    BodyHandler<String> bodyHandler;

    @BeforeEach
    void setup() {
        when(httpRequest.uri()).thenReturn(URI.create("https://test.com"));
        when(httpRequest.method()).thenReturn("GET");
        when(httpRequest.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));
    }

    @Test
    void should_preserve_incomplete_forbidden_errors_without_retrying_sync_or_async() throws Exception {
        var incompleteDetails = new ArrayList<WatsonxError>();
        incompleteDetails.add(null);
        incompleteDetails.add(new WatsonxError(403, null, null));
        incompleteDetails.add(new WatsonxError(403, null, List.of()));
        incompleteDetails.add(new WatsonxError(403, null, List.of(new WatsonxError.Error(null, "forbidden", null))));
        var request = HttpRequest.newBuilder(URI.create("https://example.com"))
            .header(BaseHttpClient.REQUEST_ID_HEADER, "incomplete-error-request").GET().build();

        for (var details : incompleteDetails) {
            var original = new WatsonxException("forbidden", 403, details);
            var syncChain = mock(SyncHttpInterceptor.Chain.class);
            when(syncChain.proceed(request, bodyHandler)).thenThrow(original);
            assertSame(original, assertThrows(WatsonxException.class,
                () -> RetryInterceptor.ON_TOKEN_EXPIRED.intercept(request, bodyHandler, 0, syncChain)));
            verify(syncChain, times(1)).proceed(request, bodyHandler);

            var asyncChain = mock(AsyncHttpInterceptor.AsyncChain.class);
            when(asyncChain.proceed(request, bodyHandler)).thenReturn(failedFuture(original));
            var future = RetryInterceptor.ON_TOKEN_EXPIRED.intercept(request, bodyHandler, 0, asyncChain);
            var failure = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertSame(original, failure.getCause());
            verify(asyncChain, times(1)).proceed(request, bodyHandler);
        }
    }

    @Test
    void should_still_retry_cos_access_denied_when_other_error_items_are_incomplete() throws Exception {
        var details = new WatsonxError(403, null, List.of(new WatsonxError.Error(null, "missing code", null),
            new WatsonxError.Error(WatsonxError.Code.COS_ACCESS_DENIED.value(), "access denied", null)));
        var original = new WatsonxException("forbidden", 403, details);
        var request = HttpRequest.newBuilder(URI.create("https://example.com"))
            .header(BaseHttpClient.REQUEST_ID_HEADER, "cos-error-request").GET().build();
        var chain = mock(SyncHttpInterceptor.Chain.class);
        when(chain.proceed(request, bodyHandler)).thenThrow(original).thenReturn(httpResponse);

        assertSame(httpResponse, RetryInterceptor.ON_TOKEN_EXPIRED.intercept(request, bodyHandler, 0, chain));
        verify(chain, times(2)).proceed(request, bodyHandler);
    }

    @Test
    void should_throw_exception_when_exponential_backoff_enabled_without_retry_interval() {
        assertThrows(IllegalArgumentException.class, () -> RetryInterceptor.builder()
            .exponentialBackoff(true)
            .retryOn(NullPointerException.class)
            .build(), "Retry interval must be positive when exponential backoff is enabled");
    }

    @Nested
    public class Sync {

        @Mock
        SyncHttpInterceptor.Chain chain;

        @Mock
        SyncHttpInterceptor mockInterceptor;

        Answer<HttpResponse<String>> CHAIN_MOCK = invocation -> {
            HttpRequest req = invocation.getArgument(0);
            BodyHandler<String> bh = invocation.getArgument(1);
            SyncHttpInterceptor.Chain chain = invocation.getArgument(3);
            return chain.proceed(req, bh);
        };

        @Test
        void should_not_retry_interrupted_requests_even_when_retry_rules_match() throws Exception {
            for (var retryType : List.of(Exception.class, InterruptedException.class)) {
                var transport = mock(HttpClient.class);
                var interrupted = new InterruptedException("interrupted request");
                var retryDecisions = new AtomicInteger();
                when(transport.send(any(), any())).thenThrow(interrupted);
                var retry = RetryInterceptor.builder().maxRetries(3).retryOn(retryType, error -> {
                    retryDecisions.incrementAndGet();
                    return true;
                }).build();
                var client = SyncHttpClient.builder().httpClient(transport).interceptor(retry).build();
                var request = HttpRequest.newBuilder(URI.create("https://example.com")).timeout(Duration.ofSeconds(1)).GET().build();

                assertSame(interrupted, assertThrows(InterruptedException.class,
                    () -> client.send(request, BodyHandlers.ofString())));

                assertEquals(0, retryDecisions.get());
                verify(transport, times(1)).send(any(), any());
            }
        }

        @Test
        void should_retry_when_null_pointer_exception_occurs() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .retryOn(NullPointerException.class)
                .build();

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .build();

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(NullPointerException.class);

            var ex = assertThrows(NullPointerException.class, () -> client.send(httpRequest, bodyHandler));
            assertEquals(NullPointerException.class, ex.getClass());
            verify(httpClient, times(2)).send(any(), any(BodyHandler.class));
        }

        @Test
        void should_throw_exception_when_no_exceptions_configured_for_retry() throws Exception {
            var ex = assertThrows(RuntimeException.class, () -> RetryInterceptor.builder().build());
            assertEquals("At least one exception must be specified", ex.getMessage());
        }

        @Test
        void should_retry_specified_number_of_times_when_null_pointer_exception_occurs() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class)
                .build();

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(NullPointerException.class);

            // After exhausting the retries the original exception is propagated, not a wrapper.
            var ex = assertThrows(NullPointerException.class, () -> client.send(httpRequest, bodyHandler));
            assertEquals(NullPointerException.class, ex.getClass());
            verify(httpClient, times(4)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(4)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        void should_retry_when_exception_matches_and_supplier_condition_is_true() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class, ex -> ex.getMessage().equals("Super null pointer exception"))
                .build();

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(new NullPointerException("Super null pointer exception"));

            var ex = assertThrows(NullPointerException.class, () -> client.send(httpRequest, bodyHandler));
            assertEquals(NullPointerException.class, ex.getClass());
            assertEquals("Super null pointer exception", ex.getMessage());
            verify(httpClient, times(4)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(4)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        void should_not_retry_when_exception_is_not_in_retry_list() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class)
                .build();

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(IllegalArgumentException.class);

            var ex = assertThrows(IllegalArgumentException.class, () -> client.send(httpRequest, bodyHandler));
            assertEquals(IllegalArgumentException.class, ex.getClass());
            verify(httpClient, times(1)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(1)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        void should_succeed_after_retry_when_initial_request_fails() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class)
                .build();

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(new NullPointerException())
                .thenReturn(httpResponse);

            var result = client.send(httpRequest, bodyHandler);
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_watsonx_exception_has_token_expired_error() throws Exception {

            RetryInterceptor retryInterceptor = onTokenExpired(3);

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            var tokenExpiredResponse = mock(HttpResponse.class);
            when(tokenExpiredResponse.statusCode()).thenReturn(401);
            when(tokenExpiredResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));
            when(tokenExpiredResponse.body()).thenReturn(
                """
                    {
                        "errors": [
                            {
                                "code": "authentication_token_expired",
                                "message": "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
                                "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                            }
                        ],
                        "trace": "23e11747002c4d2919987401b745f6a7",
                        "status_code": 401
                    }""");

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenReturn(tokenExpiredResponse)
                .thenReturn(httpResponse);

            var result = client.send(httpRequest, bodyHandler);
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_tool_exception_has_token_expired_error() throws Exception {

            RetryInterceptor retryInterceptor = onTokenExpired(3);

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            var tokenExpiredResponse = mock(HttpResponse.class);
            when(tokenExpiredResponse.statusCode()).thenReturn(401);
            when(tokenExpiredResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));
            when(tokenExpiredResponse.body()).thenReturn(
                """
                    {
                        "code": 401,
                        "error": "Unauthorized",
                        "reason": "Unauthorized",
                        "message": "Access denied",
                        "description": "jwt expired"
                    }""");

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenReturn(tokenExpiredResponse)
                .thenReturn(httpResponse);

            var result = client.send(httpRequest, bodyHandler);
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        void should_retry_with_exponential_backoff_and_succeed_after_multiple_attempts() throws Exception {
            Duration timeout = Duration.ofMillis(10);
            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(timeout)
                .exponentialBackoff(true)
                .retryOn(NullPointerException.class)
                .build();

            SyncHttpInterceptor mockInterceptor = new SyncHttpInterceptor() {
                @Override
                public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
                    throws WatsonxException, IOException, InterruptedException {
                    return chain.proceed(request, bodyHandler);
                }
            };

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenThrow(NullPointerException.class)
                .thenThrow(NullPointerException.class)
                .thenThrow(NullPointerException.class)
                .thenReturn(httpResponse);

            when(httpResponse.statusCode())
                .thenReturn(200);

            client.send(httpRequest, bodyHandler);
            verify(httpClient, times(4)).send(any(), any(BodyHandler.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_plain_text_response_has_retryable_status_code() throws Exception {

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            var badGatewayResponse = mock(HttpResponse.class);
            when(badGatewayResponse.statusCode()).thenReturn(502);
            when(badGatewayResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("text/plain")), (t, u) -> true));
            when(badGatewayResponse.body()).thenReturn("error code: 502");

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class)))
                .thenReturn(badGatewayResponse)
                .thenReturn(httpResponse);

            var result = client.send(httpRequest, bodyHandler);
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).send(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

    }

    @Nested
    public class Async {

        @Mock
        AsyncHttpInterceptor.AsyncChain chain;

        @Mock
        AsyncHttpInterceptor mockInterceptor;

        Answer<CompletableFuture<HttpResponse<String>>> CHAIN_MOCK = invocation -> {
            HttpRequest req = invocation.getArgument(0);
            BodyHandler<String> bh = invocation.getArgument(1);
            AsyncHttpInterceptor.AsyncChain chain = invocation.getArgument(3);
            return chain.proceed(req, bh);
        };

        @Test
        void should_complete_success_on_transport_thread_when_io_executor_rejects_tasks() throws Exception {
            var submissions = new AtomicInteger();
            Executor executor = task -> {
                submissions.incrementAndGet();
                throw new RejectedExecutionException("IO executor must not be used for a successful response");
            };
            var attempt = new CompletableFuture<HttpResponse<String>>();
            when(chain.proceed(httpRequest, bodyHandler)).thenReturn(attempt);
            var retry = RetryInterceptor.builder().retryOn(RuntimeException.class).build();
            var completer = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "http-completer"));

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);
                var response = retry.intercept(httpRequest, bodyHandler, 0, chain);
                var continuationThread = response.thenApply(result -> Thread.currentThread());

                var transportThread = completer.submit(() -> {
                    attempt.complete(httpResponse);
                    return Thread.currentThread();
                });

                // A waiting get() can run pending continuations, so let the transport completion finish first.
                var expectedThread = transportThread.get(3, TimeUnit.SECONDS);
                assertSame(httpResponse, response.get(3, TimeUnit.SECONDS));
                assertSame(expectedThread, continuationThread.get(3, TimeUnit.SECONDS));
                assertEquals(0, submissions.get());
                verify(chain).proceed(httpRequest, bodyHandler);
            } finally {
                completer.shutdownNow();
            }
        }

        @Test
        void should_return_completed_success_when_io_executor_rejects_tasks() throws Exception {
            var submissions = new AtomicInteger();
            Executor executor = task -> {
                submissions.incrementAndGet();
                throw new RejectedExecutionException("IO executor must not be used for a successful response");
            };
            when(chain.proceed(httpRequest, bodyHandler)).thenReturn(completedFuture(httpResponse));
            var retry = RetryInterceptor.builder().retryOn(RuntimeException.class).build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(httpRequest, bodyHandler, 0, chain);

                assertSame(httpResponse, response.get(3, TimeUnit.SECONDS));
                assertEquals(0, submissions.get());
                verify(chain).proceed(httpRequest, bodyHandler);
            }
        }

        @Test
        void should_cancel_pending_attempt_when_retry_response_is_cancelled() {
            var attempt = new CompletableFuture<HttpResponse<String>>();
            when(chain.proceed(httpRequest, bodyHandler)).thenReturn(attempt);

            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(RuntimeException.class)
                .build();

            var response = retry.intercept(httpRequest, bodyHandler, 0, chain);

            assertTrue(response.cancel(true));
            assertTrue(attempt.isCancelled());
            verify(chain).proceed(httpRequest, bodyHandler);
        }

        @Test
        void should_not_retry_when_cancelled_before_failure_is_processed() throws Exception {
            BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
            Executor executor = tasks::add;
            var attempt = new CompletableFuture<HttpResponse<String>>();
            var request = cancellationRequest();
            when(chain.proceed(request, bodyHandler)).thenReturn(attempt);

            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(RuntimeException.class)
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);
                attempt.completeExceptionally(new IllegalStateException("retryable failure"));
                Runnable failureHandler = nextTask(tasks);

                assertTrue(response.cancel(true));
                failureHandler.run();

                assertTrue(response.isCancelled());
                verify(chain).proceed(request, bodyHandler);
            }
        }

        @Test
        void should_not_send_retry_already_queued_on_executor_after_cancellation() throws Exception {
            BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
            Executor executor = tasks::add;
            var attempt = new CompletableFuture<HttpResponse<String>>();
            var request = cancellationRequest();
            when(chain.proceed(request, bodyHandler)).thenReturn(attempt);

            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(Duration.ofMillis(20))
                .retryOn(RuntimeException.class)
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);
                attempt.completeExceptionally(new IllegalStateException("retryable failure"));
                nextTask(tasks).run();
                Runnable scheduledRetry = nextTask(tasks);

                assertTrue(response.cancel(true));
                scheduledRetry.run();

                assertTrue(response.isCancelled());
                verify(chain).proceed(request, bodyHandler);
            }
        }

        @Test
        void should_not_retry_cancellation_even_when_broad_retry_rule_matches() throws Exception {
            Executor executor = Runnable::run;

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                for (Throwable cancellation : List.of(new CancellationException("cancelled"),
                    new CompletionException(new CompletionException(new CancellationException("wrapped cancellation"))))) {
                    var cancellationChain = mock(AsyncHttpInterceptor.AsyncChain.class);
                    var request = cancellationRequest();
                    var decisions = new AtomicInteger();
                    when(cancellationChain.proceed(request, bodyHandler)).thenReturn(failedFuture(cancellation));

                    var retry = RetryInterceptor.builder()
                        .maxRetries(3)
                        .retryOn(Exception.class, failure -> {
                            decisions.incrementAndGet();
                            return true;
                        })
                        .build();

                    var response = retry.intercept(request, bodyHandler, 0, cancellationChain);

                    assertThrows(CancellationException.class, () -> response.get(3, TimeUnit.SECONDS));
                    assertTrue(response.isCancelled());
                    assertEquals(0, decisions.get());
                    verify(cancellationChain).proceed(request, bodyHandler);
                }
            }
        }

        @Test
        void should_retry_nested_async_wrappers_using_the_original_exception() throws Exception {
            BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
            Executor executor = tasks::add;
            var request = cancellationRequest();
            var original = new WatsonxException("Service unavailable", 503, null);
            var wrapped = new CompletionException(new ExecutionException(new CompletionException(original)));
            var decisions = new AtomicInteger();
            when(chain.proceed(request, bodyHandler))
                .thenReturn(failedFuture(wrapped))
                .thenReturn(completedFuture(httpResponse));
            var retry = RetryInterceptor.builder()
                .maxRetries(1)
                .retryOn(WatsonxException.class, failure -> {
                    assertSame(original, failure);
                    decisions.incrementAndGet();
                    return true;
                })
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);
                nextTask(tasks).run();
                nextTask(tasks).run();

                assertSame(httpResponse, response.get(3, TimeUnit.SECONDS));
                assertEquals(1, decisions.get());
                verify(chain, times(2)).proceed(request, bodyHandler);
                verify(chain).resetToIndex(1);
            }
        }

        @Test
        void should_preserve_application_exception_with_its_own_cause() throws Exception {
            Executor executor = Runnable::run;
            var request = cancellationRequest();
            var original = new IllegalStateException("Application failure", new IOException("Underlying cause"));
            var decisions = new AtomicInteger();
            when(chain.proceed(request, bodyHandler)).thenReturn(failedFuture(original));
            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(IllegalStateException.class, failure -> {
                    assertSame(original, failure);
                    decisions.incrementAndGet();
                    return false;
                })
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);

                var failure = assertThrows(ExecutionException.class, () -> response.get(3, TimeUnit.SECONDS));
                assertSame(original, failure.getCause());
                assertEquals(1, decisions.get());
                verify(chain).proceed(request, bodyHandler);
            }
        }

        @Test
        void should_complete_exceptionally_when_failure_processing_is_rejected() throws Exception {
            var rejection = new RejectedExecutionException("executor rejected failure processing");
            Executor executor = task -> {
                throw rejection;
            };
            var attempt = new CompletableFuture<HttpResponse<String>>();
            var request = cancellationRequest();
            when(chain.proceed(request, bodyHandler)).thenReturn(attempt);

            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(RuntimeException.class)
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);
                attempt.completeExceptionally(new IllegalStateException("retryable failure"));

                var failure = assertThrows(ExecutionException.class, () -> response.get(3, TimeUnit.SECONDS));
                assertSame(rejection, failure.getCause());
                verify(chain).proceed(request, bodyHandler);
            }
        }

        @Test
        void should_complete_exceptionally_when_delayed_retry_dispatch_is_rejected() throws Exception {
            var rejection = new RejectedExecutionException("executor rejected retry dispatch");
            var submissions = new AtomicInteger();
            Executor executor = task -> {
                if (submissions.getAndIncrement() == 0)
                    task.run();
                else
                    throw rejection;
            };
            var attempt = new CompletableFuture<HttpResponse<String>>();
            var request = cancellationRequest();
            when(chain.proceed(request, bodyHandler)).thenReturn(attempt);

            var retry = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(Duration.ofMillis(20))
                .retryOn(RuntimeException.class)
                .build();

            try (MockedStatic<ExecutorProvider> executors = mockStatic(ExecutorProvider.class)) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);

                var response = retry.intercept(request, bodyHandler, 0, chain);
                attempt.completeExceptionally(new IllegalStateException("retryable failure"));

                var failure = assertThrows(ExecutionException.class, () -> response.get(3, TimeUnit.SECONDS));
                assertSame(rejection, failure.getCause());
                assertEquals(2, submissions.get());
                verify(chain).proceed(request, bodyHandler);
            }
        }

        private HttpRequest cancellationRequest() {
            return HttpRequest.newBuilder(URI.create("https://test.com"))
                .header("Watsonx-AI-SDK-Request-Id", "cancellation-test")
                .build();
        }

        private Runnable nextTask(BlockingQueue<Runnable> tasks) throws InterruptedException {
            Runnable task = tasks.poll(3, TimeUnit.SECONDS);
            assertNotNull(task, "Expected an asynchronous retry task");
            return task;
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_specified_number_of_times_when_null_pointer_exception_occurs() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(Duration.ofMillis(10))
                .retryOn(NullPointerException.class)
                .build();

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new NullPointerException()));

            var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
            assertEquals(NullPointerException.class, ex.getCause().getClass());
            verify(httpClient, times(4)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(4)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_use_base_interval_for_first_retry_with_exponential_backoff() throws Exception {

            Duration base = Duration.ofMillis(200);
            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(base)
                .exponentialBackoff(true)
                .retryOn(NullPointerException.class)
                .build();

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            List<Long> callTimestamps = new CopyOnWriteArrayList<>();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenAnswer(invocation -> {
                    callTimestamps.add(System.nanoTime());
                    return CompletableFuture.failedFuture(new NullPointerException());
                });

            assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());

            assertEquals(4, callTimestamps.size());

            long firstRetryDelayMs = (callTimestamps.get(1) - callTimestamps.get(0)) / 1_000_000;
            long secondRetryDelayMs = (callTimestamps.get(2) - callTimestamps.get(1)) / 1_000_000;

            assertTrue(firstRetryDelayMs >= 120 && firstRetryDelayMs < 320,
                () -> "First retry should wait ~200ms (base), but waited " + firstRetryDelayMs + "ms");

            assertTrue(secondRetryDelayMs >= 320,
                () -> "Second retry should wait ~400ms (2*base), but waited " + secondRetryDelayMs + "ms");
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_exception_matches_and_supplier_condition_is_true() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class, ex -> ex.getMessage().equals("Super null pointer exception"))
                .build();

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new NullPointerException("Super null pointer exception")));

            var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
            assertEquals(NullPointerException.class, ex.getCause().getClass());
            assertEquals("Super null pointer exception", ex.getCause().getMessage());
            verify(httpClient, times(4)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(4)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_not_retry_when_exception_is_not_in_retry_list() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class)
                .build();

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));

            var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
            assertEquals(RuntimeException.class, ex.getCause().getClass());
            verify(httpClient, times(1)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(1)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_succeed_after_retry_when_initial_request_fails() throws Exception {

            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryOn(NullPointerException.class)
                .build();

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new NullPointerException()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

            var result = client.send(httpRequest, bodyHandler).join();
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_watsonx_exception_has_token_expired_error() throws Exception {

            RetryInterceptor retryInterceptor = onTokenExpired(3);

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenAnswer(invocation -> {
                    BodyHandler<String> handler = invocation.getArgument(1);
                    var responseInfo = mock(HttpResponse.ResponseInfo.class);
                    when(responseInfo.statusCode()).thenReturn(401);
                    when(responseInfo.headers())
                        .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

                    var subscriber = handler.apply(responseInfo);

                    String errorBody = """
                        {
                            "errors": [
                                {
                                    "code": "authentication_token_expired",
                                    "message": "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "23e11747002c4d2919987401b745f6a7",
                            "status_code": 401
                        }""";

                    subscriber.onSubscribe(mock(Flow.Subscription.class));
                    subscriber.onNext(List.of(ByteBuffer.wrap(errorBody.getBytes(StandardCharsets.UTF_8))));
                    subscriber.onComplete();

                    return subscriber.getBody().handle((body, throwable) -> {
                        if (throwable != null) {
                            return CompletableFuture.failedFuture(throwable);
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException("Exception was expected"));
                    }).thenCompose(cf -> cf);
                })
                .thenReturn(completedFuture(httpResponse));

            var result = client.send(httpRequest, bodyHandler).get();
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_with_exponential_backoff_and_succeed_after_multiple_attempts() throws Exception {
            Duration timeout = Duration.ofMillis(10);
            RetryInterceptor retryInterceptor = RetryInterceptor.builder()
                .maxRetries(3)
                .retryInterval(timeout)
                .exponentialBackoff(true)
                .retryOn(NullPointerException.class)
                .build();

            AsyncHttpInterceptor mockInterceptor = new AsyncHttpInterceptor() {
                @Override
                public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index,
                    AsyncChain chain) {
                    return chain.proceed(request, bodyHandler);
                }
            };

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(retryInterceptor)
                .interceptor(mockInterceptor)
                .build();

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenReturn(failedFuture(new NullPointerException()))
                .thenReturn(failedFuture(new NullPointerException()))
                .thenReturn(failedFuture(new NullPointerException()))
                .thenReturn(completedFuture(httpResponse));

            client.send(httpRequest, bodyHandler).join();
            verify(httpClient, times(4)).sendAsync(any(), any(BodyHandler.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_retry_when_plain_text_response_has_retryable_status_code() throws Exception {

            AsyncHttpClient client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(RetryInterceptor.ON_RETRYABLE_STATUS_CODES)
                .interceptor(mockInterceptor)
                .build();

            when(mockInterceptor.intercept(any(), eq(bodyHandler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpClient.sendAsync(any(), any(BodyHandler.class)))
                .thenAnswer(invocation -> {
                    BodyHandler<String> handler = invocation.getArgument(1);
                    var responseInfo = mock(HttpResponse.ResponseInfo.class);
                    when(responseInfo.statusCode()).thenReturn(502);
                    when(responseInfo.headers())
                        .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("text/plain")), (t, u) -> true));

                    var subscriber = handler.apply(responseInfo);

                    subscriber.onSubscribe(mock(Flow.Subscription.class));
                    subscriber.onNext(List.of(ByteBuffer.wrap("error code: 502".getBytes(StandardCharsets.UTF_8))));
                    subscriber.onComplete();

                    return subscriber.getBody().handle((body, throwable) -> {
                        if (throwable != null) {
                            return CompletableFuture.failedFuture(throwable);
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException("Exception was expected"));
                    }).thenCompose(cf -> cf);
                })
                .thenReturn(completedFuture(httpResponse));

            var result = client.send(httpRequest, bodyHandler).get();
            assertEquals(httpResponse, result);
            verify(httpClient, times(2)).sendAsync(any(), any(BodyHandler.class));
            verify(mockInterceptor, times(2)).intercept(any(), eq(bodyHandler), anyInt(), any());
        }

    }

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    @SuppressWarnings("unchecked")
    void should_use_correct_cached_executor() throws Exception {

        var threadNames = new ArrayList<>();
        var mockHttpResponse = mock(HttpResponse.class);

        when(httpClient.executor()).thenReturn(Optional.of(ExecutorProvider.ioExecutor()));
        when(httpClient.sendAsync(any(), any(BodyHandler.class)))
            .thenReturn(failedFuture(new NullPointerException("First error")))
            .thenReturn(failedFuture(new NullPointerException("Second error")))
            .thenReturn(failedFuture(new NullPointerException("Third error")))
            .thenReturn(completedFuture(mockHttpResponse));

        RetryInterceptor retryInterceptor = RetryInterceptor.builder()
            .maxRetries(3)
            .retryInterval(Duration.ofMillis(1))
            .retryOn(NullPointerException.class)
            .build();

        var client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptor(retryInterceptor)
            .interceptor(new AsyncHttpInterceptor() {
                @Override
                public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
                    int index, AsyncChain chain) {
                    return chain.proceed(request, bodyHandler)
                        .thenApplyAsync(httpResponse -> {
                            threadNames.add(Thread.currentThread().getName());
                            return httpResponse;
                        }, ExecutorProvider.cpuExecutor())
                        .thenApplyAsync(httpResponse -> {
                            threadNames.add(Thread.currentThread().getName());
                            return httpResponse;
                        }, ExecutorProvider.ioExecutor());
                }
            }).build();

        client.send(HttpRequest.newBuilder()
            .uri(URI.create("https://test.com"))
            .GET().build(), BodyHandlers.ofString())
            .thenRun(() -> threadNames.add(Thread.currentThread().getName()))
            .get(3, TimeUnit.SECONDS);

        assertEquals(3, threadNames.size());
        assertEquals("ForkJoinPool.commonPool-worker-1", threadNames.get(0));
        assertTrue(String.valueOf(threadNames.get(1)).startsWith("http-io-"));
        assertTrue(String.valueOf(threadNames.get(2)).startsWith("http-io-"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_use_correct_io_executor() throws Exception {

        var threadNames = new ArrayList<>();
        var mockHttpResponse = mock(HttpResponse.class);
        when(httpClient.executor()).thenReturn(Optional.of(ExecutorProvider.ioExecutor()));
        when(httpClient.sendAsync(any(), any(BodyHandler.class)))
            .thenReturn(failedFuture(new NullPointerException("First error")))
            .thenReturn(failedFuture(new NullPointerException("Second error")))
            .thenReturn(failedFuture(new NullPointerException("Third error")))
            .thenReturn(completedFuture(mockHttpResponse));

        RetryInterceptor retryInterceptor = RetryInterceptor.builder()
            .maxRetries(3)
            .retryInterval(Duration.ofMillis(1))
            .retryOn(NullPointerException.class)
            .build();

        var client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptor(retryInterceptor)
            .interceptor(new AsyncHttpInterceptor() {
                @Override
                public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
                    int index, AsyncChain chain) {
                    return chain.proceed(request, bodyHandler);
                }
            }).build();

        client.send(HttpRequest.newBuilder()
            .uri(URI.create("https://test.com"))
            .GET().build(), BodyHandlers.ofString())
            .thenRun(() -> threadNames.add(Thread.currentThread().getName()))
            .get(3, TimeUnit.SECONDS);

        assertEquals(1, threadNames.size());
        assertTrue(String.valueOf(threadNames.get(0)).startsWith("http-io-"));
    }

    @Test
    void should_have_empty_predicate_when_retry_on_class_only() {
        RetryInterceptor interceptor = RetryInterceptor.builder()
            .retryOn(RuntimeException.class)
            .build();
        assertEquals(1, interceptor.retryOn().size());
        assertTrue(interceptor.retryOn().get(0).predicate().isEmpty());
    }

    @Test
    void should_expose_configured_retry_settings_via_accessors() {
        RetryInterceptor interceptor = RetryInterceptor.builder()
            .maxRetries(3)
            .retryInterval(java.time.Duration.ofMillis(50))
            .exponentialBackoff(true)
            .retryOn(RuntimeException.class)
            .build();
        assertEquals(3, interceptor.maxRetries());
        assertEquals(java.time.Duration.ofMillis(50), interceptor.retryInterval());
        assertTrue(interceptor.exponentialBackoff());
    }

    private RetryInterceptor onTokenExpired(int maxRetries) {
        return RetryInterceptor.builder()
            .maxRetries(maxRetries)
            .retryOn(
                WatsonxException.class,
                ex -> {
                    var e = (WatsonxException) ex;
                    boolean watsonxTokenExpired = e.statusCode() == 401 && e.details().map(detail -> detail.errors().stream()
                        .anyMatch(err -> err.is(WatsonxError.Code.AUTHENTICATION_TOKEN_EXPIRED))).orElse(false);
                    boolean cosTokenExpired = e.statusCode() == 403 && e.details().map(detail -> detail.errors().stream()
                        .anyMatch(err -> err.is(WatsonxError.Code.COS_ACCESS_DENIED))).orElse(false);
                    return watsonxTokenExpired || cosTokenExpired;
                }
            ).build();
    }
}
