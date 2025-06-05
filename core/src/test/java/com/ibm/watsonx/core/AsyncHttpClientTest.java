package com.ibm.watsonx.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.core.exeception.WatsonxException;
import com.ibm.watsonx.core.http.AsyncHttpClient;
import com.ibm.watsonx.core.http.AsyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class AsyncHttpClientTest {

    @Mock
    HttpClient httpClient;

    @Mock
    AsyncHttpInterceptor interceptor1;

    @Mock
    AsyncHttpInterceptor interceptor2;

    static final Answer<CompletableFuture<HttpResponse<String>>> CHAIN_MOCK = invocation -> {
        HttpRequest req = invocation.getArgument(0);
        BodyHandler<String> bh = invocation.getArgument(1);
        AsyncHttpInterceptor.AsyncChain chain = invocation.getArgument(3);
        return chain.proceed(req, bh);
    };

    @Test
    void test_send_request_with_interceptor() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptor(interceptor1)
            .interceptor(interceptor2)
            .build();

        when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(response.statusCode())
            .thenReturn(200);

        when(httpClient.sendAsync(request, handler))
            .thenReturn(completedFuture(response));

        var result = client.send(request, handler);

        assertEquals(response, result.get());

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
    }

    @Test
    void test_send_request_with_interceptors() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptors(List.of(interceptor1, interceptor2))
            .build();

        when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(response.statusCode())
            .thenReturn(200);

        when(httpClient.sendAsync(request, handler))
            .thenReturn(completedFuture(response));

        var result = client.send(request, handler);

        assertEquals(response, result.get());

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
    }

    @Test
    void test_send_request_without_interceptor() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(response.statusCode())
            .thenReturn(200);

        when(httpClient.sendAsync(request, handler))
            .thenReturn(completedFuture(response));

        var result = client.send(request, handler).get();
        assertEquals(response, result);
    }

    @Test
    void test_send_request_with_401() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(response.statusCode())
            .thenReturn(401);

        when(response.body())
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

        when(httpClient.sendAsync(request, handler)).thenReturn(completedFuture(response));
        var ex = assertThrows(CompletionException.class, () -> client.send(request, handler).join());
        assertEquals(WatsonxException.class, ex.getCause().getClass());
    }


    @Test
    void test_send_request_with_no_exception_body() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        AsyncHttpClient client = AsyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(response.statusCode())
            .thenReturn(401);

        when(httpClient.sendAsync(request, handler)).thenReturn(completedFuture(response));
        var ex = assertThrows(CompletionException.class, () -> client.send(request, handler).join());
        assertEquals(WatsonxException.class, ex.getCause().getClass());
    }
}
