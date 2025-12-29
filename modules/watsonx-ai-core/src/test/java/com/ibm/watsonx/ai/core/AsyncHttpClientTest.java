/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @BeforeEach
    void setup() {
        when(httpRequest.uri()).thenReturn(URI.create("https://test.com"));
        when(httpRequest.method()).thenReturn("GET");
        when(httpRequest.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));
    }

    @RegisterExtension
    WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort().http2PlainDisabled(true))
        .build();

    static final Answer<CompletableFuture<HttpResponse<String>>> CHAIN_MOCK = invocation -> {
        HttpRequest req = invocation.getArgument(0);
        BodyHandler<String> bh = invocation.getArgument(1);
        AsyncHttpInterceptor.AsyncChain chain = invocation.getArgument(3);
        return chain.proceed(req, bh);
    };

    @Test
    void should_send_http_request_with_interceptor() throws Exception {

        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptor(interceptor1)
            .interceptor(interceptor2)
            .build();

        when(interceptor1.intercept(any(), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(any(), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(httpClient.sendAsync(any(), any(BodyHandler.class)))
            .thenReturn(completedFuture(httpResponse));

        var result = client.send(httpRequest, handler);

        assertEquals(httpResponse, result.get());

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(any(), eq(handler), anyInt(), any());
    }

    @Test
    void should_send_http_request_with_multiple_interceptors() throws Exception {

        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptors(List.of(interceptor1, interceptor2))
            .build();

        when(interceptor1.intercept(any(), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(any(), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(httpClient.sendAsync(any(), any(BodyHandler.class)))
            .thenReturn(completedFuture(httpResponse));

        var result = client.send(httpRequest, handler);

        assertEquals(httpResponse, result.get());

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(any(), eq(handler), anyInt(), any());
    }

    @Test
    void should_send_http_request_without_interceptor() throws Exception {

        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(httpClient.sendAsync(any(), any(BodyHandler.class)))
            .thenReturn(completedFuture(httpResponse));

        var result = client.send(httpRequest, handler).get();
        assertEquals(httpResponse, result);
    }

    @Test
    void should_throw_exception_when_response_status_is_401() throws Exception {

        wireMock.stubFor(get(urlEqualTo("/test-401"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "errors": [
                            {
                                "code": "authentication_token_not_valid",
                                "message": "Failed to authenticate the httpRequest due to invalid token",
                                "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                            }
                        ],
                        "trace": "23e11747002c4d2919987401b745f6a7",
                        "status_code": 401
                    }""")));

        AsyncHttpClient client = AsyncHttpClient.builder().httpClient(HttpClient.newHttpClient()).build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%s/test-401".formatted(wireMock.getPort())))
            .GET()
            .build();

        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        CompletionException ex = assertThrows(CompletionException.class, () -> client.send(request, handler).join());
        assertEquals(WatsonxException.class, ex.getCause().getClass());

        WatsonxException cause = (WatsonxException) ex.getCause();
        assertTrue(cause.getMessage().contains("authentication_token_not_valid"));
    }


    @Test
    void should_throw_exception_when_response_has_no_body() throws Exception {

        wireMock.stubFor(get(urlEqualTo("/test-401"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")));

        AsyncHttpClient client = AsyncHttpClient.builder().httpClient(HttpClient.newHttpClient()).build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%s/test-401".formatted(wireMock.getPort())))
            .GET()
            .build();

        CompletionException ex = assertThrows(CompletionException.class, () -> client.send(request, handler).join());
        assertEquals(WatsonxException.class, ex.getCause().getClass());
    }

    @Test
    void should_return_async_responses_in_order_of_completion_not_submission() throws Exception {

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
