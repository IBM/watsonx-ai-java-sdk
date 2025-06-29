/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;

@ExtendWith(MockitoExtension.class)
public class RetryInterceptorTest {

  @Mock
  HttpClient httpClient;

  @Mock
  HttpRequest httpRequest;

  @Mock
  HttpResponse<String> httpResponse;

  @Mock
  BodyHandler<String> bodyHandler;

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
    void retry_with_default_values() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .build();

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(NullPointerException.class);

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler));
      assertEquals(NullPointerException.class, ex.getCause().getClass());
      verify(httpClient, times(2)).send(httpRequest, bodyHandler);
    }

    @Test
    void retry_without_exception() throws Exception {
      var ex = assertThrows(RuntimeException.class, () -> RetryInterceptor.builder().build());
      assertEquals("At least one exception must be specified", ex.getMessage());
    }

    @Test
    void retry_only_with_exception() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(NullPointerException.class);

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler));
      assertEquals("Max retries reached", ex.getMessage());
      assertEquals(NullPointerException.class, ex.getCause().getClass());
      verify(httpClient, times(4)).send(httpRequest, bodyHandler);
      verify(mockInterceptor, times(4)).intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any());
    }

    @Test
    void retry_with_exception_and_supplier() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class, ex -> ex.getMessage().equals("Super null pointer exception"))
        .build();

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(new NullPointerException("Super null pointer exception"));

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler));
      assertEquals(NullPointerException.class, ex.getCause().getClass());
      assertEquals("Super null pointer exception", ex.getCause().getMessage());
      verify(httpClient, times(4)).send(httpRequest, bodyHandler);
      verify(mockInterceptor, times(4)).intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any());
    }

    @Test
    void retry_with_unhandled_exception() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(IllegalArgumentException.class);

      var ex = assertThrows(IllegalArgumentException.class, () -> client.send(httpRequest, bodyHandler));
      assertEquals(IllegalArgumentException.class, ex.getClass());
      verify(httpClient, times(1)).send(httpRequest, bodyHandler);
      verify(mockInterceptor, times(1)).intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any());
    }

    @Test
    void retry_with_result() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpResponse.statusCode())
        .thenReturn(200);

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(new NullPointerException())
        .thenReturn(httpResponse);

      var result = client.send(httpRequest, bodyHandler);
      assertEquals(httpResponse, result);
      verify(httpClient, times(2)).send(httpRequest, bodyHandler);
      verify(mockInterceptor, times(2)).intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retry_with_watsonx_exception() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.onTokenExpired(3);

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any()))
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

      when(httpClient.send(httpRequest, bodyHandler))
        .thenReturn(tokenExpiredResponse)
        .thenReturn(httpResponse);

      var result = client.send(httpRequest, bodyHandler);
      assertEquals(httpResponse, result);
      verify(httpClient, times(2)).send(httpRequest, bodyHandler);
      verify(mockInterceptor, times(2)).intercept(eq(httpRequest), eq(bodyHandler), anyInt(), any());
    }

    @Test
    void retry_with_exponential_backoff_fail_retries() throws Exception {
      Duration timeout = Duration.ofMillis(10);
      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryInterval(timeout)
        .exponentialBackoff(true)
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpInterceptor mockInterceptor = new SyncHttpInterceptor() {
        int numCalled = 0;

        @Override
        public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index,
          Chain chain)
          throws WatsonxException, IOException, InterruptedException {
          if (this.numCalled > 0)
            assertEquals((long) (timeout.toMillis() * Math.pow(2, this.numCalled - 1)),
              retryInterceptor.getTimeout().toMillis());
          this.numCalled++;
          return chain.proceed(request, bodyHandler);
        }
      };

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(NullPointerException.class);

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler));
      assertEquals(NullPointerException.class, ex.getCause().getClass());
      verify(httpClient, times(4)).send(httpRequest, bodyHandler);
      assertEquals(retryInterceptor.getTimeout().toMillis(), timeout.toMillis());
    }

    @Test
    void retry_with_exponential_backoff_succeed_after_retry() throws Exception {
      Duration timeout = Duration.ofMillis(10);
      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryInterval(timeout)
        .exponentialBackoff(true)
        .retryOn(NullPointerException.class)
        .build();

      SyncHttpInterceptor mockInterceptor = new SyncHttpInterceptor() {
        int numCalled = 0;

        @Override
        public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index,
          Chain chain)
          throws WatsonxException, IOException, InterruptedException {
          if (this.numCalled > 0)
            assertEquals((long) (timeout.toMillis() * Math.pow(2, this.numCalled - 1)),
              retryInterceptor.getTimeout().toMillis());
          this.numCalled++;
          return chain.proceed(request, bodyHandler);
        }
      };

      SyncHttpClient client = SyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(httpClient.send(httpRequest, bodyHandler))
        .thenThrow(NullPointerException.class)
        .thenReturn(httpResponse);

      when(httpResponse.statusCode())
        .thenReturn(200);

      client.send(httpRequest, bodyHandler);
      verify(httpClient, times(2)).send(httpRequest, bodyHandler);
      assertEquals(retryInterceptor.getTimeout().toMillis(), timeout.toMillis());
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
      Executor executor = invocation.getArgument(2);
      AsyncHttpInterceptor.AsyncChain chain = invocation.getArgument(4);
      return chain.proceed(req, bh, executor);
    };

    @Test
    void retry_only_with_exception() throws Exception {

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

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(CompletableFuture.failedFuture(new NullPointerException()));

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
      assertEquals("Max retries reached", ex.getCause().getMessage());
      assertEquals(NullPointerException.class, ex.getCause().getCause().getClass());
      verify(httpClient, times(3)).sendAsync(httpRequest, bodyHandler);
      verify(mockInterceptor, times(3)).intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any());
    }

    @Test
    void retry_with_exception_and_supplier() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class, ex -> ex.getMessage().equals("Super null pointer exception"))
        .build();

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(CompletableFuture.failedFuture(new NullPointerException("Super null pointer exception")));

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
      assertEquals(NullPointerException.class, ex.getCause().getCause().getClass());
      assertEquals("Super null pointer exception", ex.getCause().getCause().getMessage());
      verify(httpClient, times(3)).sendAsync(httpRequest, bodyHandler);
      verify(mockInterceptor, times(3)).intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any());
    }

    @Test
    void retry_with_unhandled_exception() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class)
        .build();

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
      assertEquals(RuntimeException.class, ex.getCause().getClass());
      verify(httpClient, times(1)).sendAsync(httpRequest, bodyHandler);
      verify(mockInterceptor, times(1)).intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any());
    }

    @Test
    void retry_with_result() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryOn(NullPointerException.class)
        .build();

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(httpResponse.statusCode())
        .thenReturn(200);

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any()))
        .thenAnswer(CHAIN_MOCK);

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(CompletableFuture.failedFuture(new NullPointerException()))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));

      var result = client.send(httpRequest, bodyHandler).join();
      assertEquals(httpResponse, result);
      verify(httpClient, times(2)).sendAsync(httpRequest, bodyHandler);
      verify(mockInterceptor, times(2)).intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retry_with_watsonx_exception() throws Exception {

      RetryInterceptor retryInterceptor = RetryInterceptor.onTokenExpired(3);

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(mockInterceptor.intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any()))
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

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(completedFuture(tokenExpiredResponse))
        .thenReturn(completedFuture(httpResponse));

      var result = client.send(httpRequest, bodyHandler).get();
      assertEquals(httpResponse, result);
      verify(httpClient, times(2)).sendAsync(httpRequest, bodyHandler);
      verify(mockInterceptor, times(2)).intercept(eq(httpRequest), eq(bodyHandler), any(), anyInt(), any());
    }

    @Test
    void retry_with_exponential_backoff_fail_retries() throws Exception {
      Duration timeout = Duration.ofMillis(10);
      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryInterval(timeout)
        .exponentialBackoff(true)
        .retryOn(NullPointerException.class)
        .build();

      AsyncHttpInterceptor mockInterceptor = new AsyncHttpInterceptor() {
        int numCalled = 0;

        @Override
        public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
          Executor executor, int index, AsyncChain chain) {
          if (this.numCalled > 0)
            assertEquals((long) (timeout.toMillis() * Math.pow(2, this.numCalled - 1)),
              retryInterceptor.getTimeout().toMillis());
          this.numCalled++;
          return chain.proceed(request, bodyHandler, executor);
        }
      };

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(CompletableFuture.failedFuture(new NullPointerException()));

      var ex = assertThrows(RuntimeException.class, () -> client.send(httpRequest, bodyHandler).join());
      assertEquals(NullPointerException.class, ex.getCause().getCause().getClass());
      verify(httpClient, times(3)).sendAsync(httpRequest, bodyHandler);
      assertEquals(retryInterceptor.getTimeout().toMillis(), timeout.toMillis());
    }

    @Test
    void retry_with_exponential_backoff_succeed_after_retry() throws Exception {
      Duration timeout = Duration.ofMillis(10);
      RetryInterceptor retryInterceptor = RetryInterceptor.builder()
        .maxRetries(3)
        .retryInterval(timeout)
        .exponentialBackoff(true)
        .retryOn(NullPointerException.class)
        .build();

      AsyncHttpInterceptor mockInterceptor = new AsyncHttpInterceptor() {
        int numCalled = 0;

        @Override
        public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
          Executor executor, int index, AsyncChain chain) {
          if (this.numCalled > 0)
            assertEquals((long) (timeout.toMillis() * Math.pow(2, this.numCalled - 1)),
              retryInterceptor.getTimeout().toMillis());
          this.numCalled++;
          return chain.proceed(request, bodyHandler, executor);
        }
      };

      AsyncHttpClient client = AsyncHttpClient.builder()
        .httpClient(httpClient)
        .interceptor(retryInterceptor)
        .interceptor(mockInterceptor)
        .build();

      when(httpClient.sendAsync(httpRequest, bodyHandler))
        .thenReturn(failedFuture(new NullPointerException()))
        .thenReturn(completedFuture(httpResponse));

      when(httpResponse.statusCode()).thenReturn(200);

      client.send(httpRequest, bodyHandler).join();
      verify(httpClient, times(2)).sendAsync(httpRequest, bodyHandler);
      assertEquals(retryInterceptor.getTimeout().toMillis(), timeout.toMillis());

    }
  }
}
