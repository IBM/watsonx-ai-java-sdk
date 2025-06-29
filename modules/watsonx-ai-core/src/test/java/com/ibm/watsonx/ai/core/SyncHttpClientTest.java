/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class SyncHttpClientTest {

  @Mock
  HttpClient httpClient;

  @Mock
  HttpResponse<String> httpResponse;

  @Mock
  SyncHttpInterceptor interceptor1;

  @Mock
  SyncHttpInterceptor interceptor2;

  Answer<HttpResponse<String>> CHAIN_MOCK = invocation -> {
    HttpRequest req = invocation.getArgument(0);
    BodyHandler<String> bh = invocation.getArgument(1);
    SyncHttpInterceptor.Chain chain = invocation.getArgument(3);
    return chain.proceed(req, bh);
  };

  @Test
  void test_send_request_with_interceptor() throws Exception {

    HttpRequest request = mock(HttpRequest.class);
    BodyHandler<String> handler = mock(BodyHandler.class);
    SyncHttpClient client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptor(interceptor1)
      .interceptor(interceptor2)
      .build();

    when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.send(request, handler)).thenReturn(httpResponse);

    var response = client.send(request, handler);

    assertEquals(httpResponse, response);

    InOrder inOrder = inOrder(interceptor1, interceptor2);
    inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
    inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
  }

  @Test
  void test_send_request_with_interceptors() throws Exception {

    HttpRequest request = mock(HttpRequest.class);
    BodyHandler<String> handler = mock(BodyHandler.class);
    SyncHttpClient client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .interceptors(List.of(interceptor1, interceptor2))
      .build();

    when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
      .thenAnswer(CHAIN_MOCK);

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.send(request, handler)).thenReturn(httpResponse);

    var response = client.send(request, handler);

    assertEquals(httpResponse, response);

    InOrder inOrder = inOrder(interceptor1, interceptor2);
    inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
    inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
  }

  @Test
  void test_send_request_without_interceptor() throws Exception {

    HttpRequest request = mock(HttpRequest.class);
    BodyHandler<String> handler = mock(BodyHandler.class);
    SyncHttpClient client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode())
      .thenReturn(200);

    when(httpClient.send(request, handler)).thenReturn(httpResponse);

    var response = client.send(request, handler);
    assertEquals(httpResponse, response);
  }

  @Test
  void test_send_request_with_401() throws Exception {

    HttpRequest request = mock(HttpRequest.class);
    BodyHandler<String> handler = mock(BodyHandler.class);
    SyncHttpClient client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode())
      .thenReturn(401);

    when(httpResponse.headers())
      .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

    when(httpResponse.body())
      .thenReturn(
        """
          {
              "errors": [
                  {
                      "code": "authentication_token_not_valid",
                      "message": "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
                      "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                  }
              ],
              "trace": "23e11747002c4d2919987401b745f6a7",
              "status_code": 401
          }""");

    when(httpClient.send(request, handler)).thenReturn(httpResponse);
    assertThrows(WatsonxException.class, () -> client.send(request, handler));
  }

  @Test
  void test_send_request_with_no_exception_body() throws Exception {

    HttpRequest request = mock(HttpRequest.class);
    BodyHandler<String> handler = mock(BodyHandler.class);
    SyncHttpClient client = SyncHttpClient.builder()
      .httpClient(httpClient)
      .build();

    when(httpResponse.statusCode())
      .thenReturn(401);

    when(httpClient.send(request, handler)).thenReturn(httpResponse);
    assertThrows(WatsonxException.class, () -> client.send(request, handler));
  }
}
