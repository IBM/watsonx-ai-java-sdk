/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class AsyncHttpClientTest {

  @Mock
  HttpClient httpClient;

  @Mock
  HttpRequest httpRequest;

  @Mock
  BodyHandler<String> handler;

  @Mock
  HttpResponse<String> httpResponse;

  @Mock
  AsyncHttpInterceptor interceptor1;

  @Mock
  AsyncHttpInterceptor interceptor2;

  @RegisterExtension
  WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
    .build();

  static final Answer<CompletableFuture<HttpResponse<String>>> CHAIN_MOCK = invocation -> {
    HttpRequest req = invocation.getArgument(0);
    BodyHandler<String> bh = invocation.getArgument(1);
    Executor executor = invocation.getArgument(2);
    AsyncHttpInterceptor.AsyncChain chain = invocation.getArgument(4);
    return chain.proceed(req, bh, executor);
  };

  @Test
  void test_send_httpRequest_with_interceptor() throws Exception {

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(interceptor1)
      .interceptor(interceptor2)
      .build();

    when(interceptor1.intercept(eq(httpRequest), eq(handler), any(), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(interceptor2.intercept(eq(httpRequest), eq(handler), any(), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.sendAsync(httpRequest, handler))
      .thenReturn(completedFuture(httpResponse));

    var result = client.send(httpRequest, handler);

    assertEquals(httpResponse, result.get());

    InOrder inOrder = inOrder(interceptor1, interceptor2);
    inOrder.verify(interceptor1).intercept(eq(httpRequest), eq(handler), any(), anyInt(), any());
    inOrder.verify(interceptor2).intercept(eq(httpRequest), eq(handler), any(), anyInt(), any());
  }

  @Test
  void test_send_httpRequest_with_interceptors() throws Exception {

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptors(List.of(interceptor1, interceptor2))
      .build();

    when(interceptor1.intercept(eq(httpRequest), eq(handler), any(), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(interceptor2.intercept(eq(httpRequest), eq(handler), any(), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.sendAsync(httpRequest, handler))
      .thenReturn(completedFuture(httpResponse));

    var result = client.send(httpRequest, handler);

    assertEquals(httpResponse, result.get());

    InOrder inOrder = inOrder(interceptor1, interceptor2);
    inOrder.verify(interceptor1).intercept(eq(httpRequest), eq(handler), any(), anyInt(), any());
    inOrder.verify(interceptor2).intercept(eq(httpRequest), eq(handler), any(), anyInt(), any());
  }

  @Test
  void test_send_httpRequest_without_interceptor() throws Exception {

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.sendAsync(httpRequest, handler))
      .thenReturn(completedFuture(httpResponse));

    var result = client.send(httpRequest, handler).get();
    assertEquals(httpResponse, result);
  }

  @Test
  void test_send_httpRequest_with_401() throws Exception {

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode())
      .thenReturn(401);

    when(httpResponse.body())
      .thenReturn(
        """
          {
              "errors": [
                  {
                      "code": "authentication_token_not_valid",
                      "message": "Failed to authenticate the httpRequest due to invalid token: Failed to parse and verify token",
                      "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                  }
              ],
              "trace": "23e11747002c4d2919987401b745f6a7",
              "status_code": 401
          }""");

    when(httpClient.sendAsync(httpRequest, handler)).thenReturn(completedFuture(httpResponse));
    var ex = assertThrows(CompletionException.class, () -> client.send(httpRequest, handler).join());
    assertEquals(WatsonxException.class, ex.getCause().getClass());
  }


  @Test
  void test_send_httpRequest_with_no_exception_body() throws Exception {

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode()).thenReturn(401);

    when(httpClient.sendAsync(httpRequest, handler)).thenReturn(completedFuture(httpResponse));
    var ex = assertThrows(CompletionException.class, () -> client.send(httpRequest, handler).join());
    assertEquals(WatsonxException.class, ex.getCause().getClass());
  }

  @Test
  void test_http_custom_executor() throws Exception {

    Executor executor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
        .setNameFormat("my-super-thread")
        .build()
    );

    when(httpClient.executor()).thenReturn(Optional.of(executor));

    AsyncHttpClient client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    assertEquals(executor, client.executor());
  }

  @Test
  void test_delegate_http_custom_executor() throws Exception {

    Executor executor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
        .setNameFormat("my-super-thread")
        .build()
    );

    CompletableFuture<HttpResponse<String>> mockFuture = mock(CompletableFuture.class);

    when(httpClient.sendAsync(eq(httpRequest), eq(handler))).thenReturn(mockFuture);
    when(httpClient.executor()).thenReturn(Optional.of(executor));

    when(mockFuture.handleAsync(any(), any(Executor.class))).thenAnswer(invocation -> {
      assertEquals(executor, invocation.getArgument(1));
      return CompletableFuture.completedFuture("result-mock");
    });

    AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build()
      .send(httpRequest, handler);
  }

  @Test
  void test_override_http_default_executor_by_parameter() throws Exception {

    Executor customExecutor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
        .setNameFormat("my-super-thread")
        .build()
    );

    Executor overrideExecutor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
        .setNameFormat("my-super-override-thread")
        .build()
    );

    CompletableFuture<HttpResponse<String>> mockFuture = mock(CompletableFuture.class);

    when(httpClient.sendAsync(eq(httpRequest), eq(handler))).thenReturn(mockFuture);
    when(httpClient.executor()).thenReturn(Optional.of(customExecutor));

    when(mockFuture.handleAsync(any(), any(Executor.class))).thenAnswer(invocation -> {
      assertEquals(overrideExecutor, invocation.getArgument(1));
      return CompletableFuture.completedFuture("result-mock");
    });

    AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build()
      .send(httpRequest, handler, overrideExecutor);
  }

  @Test
  void test_executor_by_parameter() throws Exception {

    Executor overrideExecutor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
        .setNameFormat("my-super-override-thread")
        .build()
    );

    CompletableFuture<HttpResponse<String>> mockFuture = mock(CompletableFuture.class);

    when(httpClient.sendAsync(eq(httpRequest), eq(handler))).thenReturn(mockFuture);

    when(mockFuture.handleAsync(any(), any(Executor.class))).thenAnswer(invocation -> {
      assertEquals(overrideExecutor, invocation.getArgument(1));
      return CompletableFuture.completedFuture("result-mock");
    });

    AsyncHttpClient.builder()
      .httpClient(httpClient)
      .build()
      .send(httpRequest, handler, overrideExecutor);
  }

  @Test
  void test_complete_requests_in_response_order_not_call_order() throws Exception {

    var url = "http://localhost:%s/my-path".formatted(wireMock.getPort());

    wireMock.stubFor(post(urlEqualTo("/my-path"))
      .withHeader("response", equalTo("1"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody("Response number 1")
        .withFixedDelay(1000))
    );

    wireMock.stubFor(post(urlEqualTo("/my-path"))
      .withHeader("response", equalTo("2"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody("Response number 2")
        .withFixedDelay(500))
    );

    wireMock.stubFor(post(urlEqualTo("/my-path"))
      .withHeader("response", equalTo("3"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody("Response number 3")
        .withFixedDelay(50))
    );

    var client = AsyncHttpClient.builder()
      .httpClient(HttpClient.newBuilder().executor(Executors.newSingleThreadExecutor()).build())
      .build();

    var response_1 = client.send(
      HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("response", "1")
        .POST(HttpRequest.BodyPublishers.noBody())
        .build(),
      BodyHandlers.ofString()
    );

    var response_2 = client.send(
      HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("response", "2")
        .POST(HttpRequest.BodyPublishers.noBody())
        .build(),
      BodyHandlers.ofString()
    );

    var response_3 = client.send(
      HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("response", "3")
        .POST(HttpRequest.BodyPublishers.noBody())
        .build(),
      BodyHandlers.ofString()
    );

    ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    CompletableFuture.allOf(
      response_1.thenAccept(resp -> queue.add(resp.body())),
      response_2.thenAccept(resp -> queue.add(resp.body())),
      response_3.thenAccept(resp -> queue.add(resp.body()))
    ).join();

    assertEquals("Response number 3", queue.poll());
    assertEquals("Response number 2", queue.poll());
    assertEquals("Response number 1", queue.poll());
  }
}
