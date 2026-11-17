/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.AuthenticationInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisabledInNativeImage
public abstract class AbstractWatsonxTest {

    protected static final String ML_API_PATH = "/ml/v1";
    protected static final String ML_API_TEXT_PATH = ML_API_PATH.concat("/text");
    protected static final String API_VERSION = "2026-10-28";
    protected static final String TRANSACTION_ID_HEADER = "X-Global-Transaction-Id";

    @Mock
    protected HttpClient mockSecureHttpClient;

    @Mock
    protected HttpClient mockInsecureHttpClient;

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
        when(mockAuthenticator.scheme()).thenReturn("Bearer");
        resetHttpClient();
    }

    protected void withWatsonxServiceMock(Runnable action) {
        try (MockedStatic<HttpClientProvider> mockedStatic = mockStatic(HttpClientProvider.class)) {
            mockedStatic.when(() -> HttpClientProvider.httpClient(true)).thenReturn(mockSecureHttpClient);
            mockedStatic.when(() -> HttpClientProvider.httpClient(false)).thenReturn(mockInsecureHttpClient);
            action.run();
        }
    }

    @SuppressWarnings("unchecked")
    protected void assertCancelsTransport(Supplier<CompletableFuture<?>> operation) {
        var transport = new CompletableFuture<HttpResponse<String>>();
        var started = new CompletableFuture<Void>();
        when(mockSecureHttpClient.sendAsync(any(), any(BodyHandler.class))).thenAnswer(invocation -> {
            started.complete(null);
            return transport;
        });

        withWatsonxServiceMock(() -> {
            var response = operation.get();
            assertDoesNotThrow(() -> started.get(5, TimeUnit.SECONDS));

            assertTrue(response.cancel(true));

            assertDoesNotThrow(() -> transport.handle((r, t) -> null).get(5, TimeUnit.SECONDS));
            assertTrue(transport.isCancelled());
            assertThrows(CancellationException.class, response::join);
        });
    }

    @SuppressWarnings("unchecked")
    protected void assertCancelsRetry(Supplier<CompletableFuture<?>> operation) {
        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        Executor executor = tasks::add;
        var transport = new CompletableFuture<HttpResponse<String>>();
        when(mockSecureHttpClient.sendAsync(any(), any(BodyHandler.class))).thenReturn(transport);
        var retryInterceptor = RetryInterceptor.builder()
            .maxRetries(1)
            .retryInterval(Duration.ofMillis(20))
            .retryOn(WatsonxException.class, error -> ((WatsonxException) error).statusCode() == 503)
            .build();

        withWatsonxServiceMock(() -> {
            try (var executors = mockStatic(ExecutorProvider.class);
                var clients = mockStatic(HttpClientFactory.class, invocation -> {
                    if (!invocation.getMethod().getName().equals("createAsync"))
                        return invocation.callRealMethod();

                    var builder = AsyncHttpClient.builder().httpClient(invocation.getArgument(1));
                    Authenticator authenticator = invocation.getArgument(0);
                    if (nonNull(authenticator))
                        builder.interceptor(new AuthenticationInterceptor(authenticator));
                    return builder.interceptor(retryInterceptor).build();
                })) {
                executors.when(ExecutorProvider::ioExecutor).thenReturn(executor);
                executors.when(ExecutorProvider::callbackExecutor).thenReturn((Executor) Runnable::run);
                executors.when(ExecutorProvider::cpuExecutor).thenReturn((Executor) Runnable::run);
                var response = operation.get();
                verify(mockSecureHttpClient).sendAsync(any(), any(BodyHandler.class));

                transport.completeExceptionally(new WatsonxException("Service unavailable", 503, null));
                var failureHandler = assertDoesNotThrow(() -> tasks.poll(3, TimeUnit.SECONDS));
                assertNotNull(failureHandler, "Expected the HTTP failure to be processed");
                failureHandler.run();

                // Wait for the real backoff to expire, but keep its retry queued until after cancellation.
                var retry = assertDoesNotThrow(() -> tasks.poll(3, TimeUnit.SECONDS));
                assertNotNull(retry, "Expected a scheduled retry after the HTTP 503");
                assertFalse(response.isDone());
                assertTrue(response.cancel(true));

                retry.run();
                for (Runnable task; (task = tasks.poll()) != null;)
                    task.run();

                verify(mockSecureHttpClient, times(1)).sendAsync(any(), any(BodyHandler.class));
                assertThrows(CancellationException.class, response::join);
            }
        });
    }

    protected void mockHttpClientSend(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) {
        try {
            when(mockSecureHttpClient.send(request, responseBodyHandler)).thenReturn(mockHttpResponse);
        } catch (Exception e) {
            fail(e);
        }
    }

    protected void resetHttpClient() {
        try {
            var secureClient = HttpClientProvider.class.getDeclaredField("secureClient");
            secureClient.setAccessible(true);
            secureClient.set(null, null);
            var insecureClient = HttpClientProvider.class.getDeclaredField("insecureClient");
            insecureClient.setAccessible(true);
            insecureClient.set(null, null);
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Polls {@code server} until at least {@code count} requests matching {@code pattern} have been received, or until 2 seconds have elapsed. Use
     * this instead of a fixed {@code Thread.sleep} when verifying that async fire-and-forget calls (e.g. file deletes) eventually reach the mock
     * server.
     *
     * @param server the WireMock server to poll
     * @param pattern the request pattern to wait for
     * @param count the minimum number of matching requests expected
     */
    protected static void waitForRequests(WireMockExtension server, RequestPatternBuilder pattern, int count)
        throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            if (server.countRequestsMatching(pattern.build()).getCount() >= count)
                return;
            Thread.sleep(50);
        }
    }

    /**
     * Waits up to {@code timeoutMs} milliseconds for {@code thread} to enter {@link Thread.State#TIMED_WAITING}. Use this to ensure an interrupt
     * lands inside {@code Thread.sleep} in the poll loop, not inside {@code HttpClient.send}, which would produce a different exception.
     *
     * @param thread the thread to wait for
     * @param timeoutMs maximum wait time in milliseconds
     */
    protected static void waitForTimedWaiting(Thread thread, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (thread.getState() != Thread.State.TIMED_WAITING && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
    }
}
