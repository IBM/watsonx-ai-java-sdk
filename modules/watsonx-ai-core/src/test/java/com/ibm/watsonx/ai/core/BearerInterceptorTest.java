/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class BearerInterceptorTest {

  @Mock
  private HttpClient httpClient;

  @Test
  void test_bearer_interceptor_sync() throws Exception {

    var response = Utils.okResponse();
    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    AuthenticationProvider authenticator = IAMAuthenticator.builder()
      .httpClient(httpClient)
      .apiKey("my_super_api_key")
      .build();

    var client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(new BearerInterceptor(authenticator))
      .build();

    when(httpClient.<String>send(requestCaptor.capture(), any()))
      .thenReturn(response);

    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
      .POST(BodyPublishers.ofString("Call this!"))
      .build();

    client.send(fakeRequest, BodyHandlers.ofString());
    assertEquals("Bearer my_super_token", requestCaptor.getValue().headers().firstValue("Authorization").get());
  }

  @Test
  void test_bearer_interceptor_sync_with_exception() throws Exception {

    var response = Utils.koResponse();
    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    AuthenticationProvider authenticator = IAMAuthenticator.builder()
      .httpClient(httpClient)
      .apiKey("my_super_api_key")
      .build();

    var client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(new BearerInterceptor(authenticator))
      .build();

    when(httpClient.<String>send(requestCaptor.capture(), any()))
      .thenReturn(response);

    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
      .POST(BodyPublishers.ofString("Call this!"))
      .build();

    var ex = assertThrows(RuntimeException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()));
    assertEquals(Utils.WRONG_RESPONSE, ex.getMessage());
  }

  @Test
  void test_bearer_interceptor_async() throws Exception {

    var response = Utils.okResponse();
    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    AuthenticationProvider authenticator = IAMAuthenticator.builder()
      .httpClient(httpClient)
      .apiKey("my_super_api_key")
      .build();

    var client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(new BearerInterceptor(authenticator))
      .build();

    when(httpClient.<String>sendAsync(requestCaptor.capture(), any()))
      .thenReturn(completedFuture(response));

    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
      .POST(BodyPublishers.ofString("Call this!"))
      .build();

    client.send(fakeRequest, BodyHandlers.ofString()).get();
    assertEquals("Bearer my_super_token", requestCaptor.getValue().headers().firstValue("Authorization").get());
  }

  @Test
  void test_bearer_interceptor_async_with_exception() throws Exception {

    var response = Utils.koResponse();
    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    AuthenticationProvider authenticator = IAMAuthenticator.builder()
      .httpClient(httpClient)
      .apiKey("my_super_api_key")
      .build();

    var client = AsyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(new BearerInterceptor(authenticator))
      .build();

    when(httpClient.<String>sendAsync(requestCaptor.capture(), any()))
      .thenReturn(completedFuture(response));

    var fakeRequest = HttpRequest.newBuilder(URI.create("http://test"))
      .POST(BodyPublishers.ofString("Call this!"))
      .build();

    var ex =
      assertThrows(CompletionException.class, () -> client.send(fakeRequest, BodyHandlers.ofString()).join());
    assertEquals(Utils.WRONG_RESPONSE, ex.getCause().getMessage());
  }
}
