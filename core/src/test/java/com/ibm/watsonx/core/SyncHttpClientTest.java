package com.ibm.watsonx.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.core.http.SyncHttpClient;
import com.ibm.watsonx.core.http.SyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class SyncHttpClientTest {

    @Mock
    HttpClient httpClient;

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
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);
        SyncHttpClient client = SyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptor(interceptor1)
            .interceptor(interceptor2)
            .build();

        when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(httpClient.send(request, handler)).thenReturn(expectedResponse);

        var response = client.send(request, handler);

        assertEquals(expectedResponse, response);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
    }

    @Test
    void test_send_request_with_interceptors() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);
        SyncHttpClient client = SyncHttpClient.builder()
            .httpClient(httpClient)
            .interceptors(List.of(interceptor1, interceptor2))
            .build();

        when(interceptor1.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(interceptor2.intercept(eq(request), eq(handler), anyInt(), any()))
            .thenAnswer(CHAIN_MOCK);

        when(httpClient.send(request, handler)).thenReturn(expectedResponse);

        var response = client.send(request, handler);

        assertEquals(expectedResponse, response);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(eq(request), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(eq(request), eq(handler), anyInt(), any());
    }

    @Test
    void test_send_request_without_interceptor() throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        BodyHandler<String> handler = mock(BodyHandler.class);
        HttpResponse<String> expectedResponse = mock(HttpResponse.class);
        SyncHttpClient client = SyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(httpClient.send(request, handler)).thenReturn(expectedResponse);

        var response = client.send(request, handler);
        assertEquals(expectedResponse, response);
    }
}
