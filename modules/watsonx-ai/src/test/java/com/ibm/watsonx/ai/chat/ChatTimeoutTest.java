/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;

@SuppressWarnings("unchecked")
@DisabledInNativeImage
class ChatTimeoutTest extends AbstractWatsonxTest {

    private static final String TIME_LIMIT_RESPONSE = """
        {"choices":[{"index":0,"message":{"role":"assistant","content":"Partial answer"},"finish_reason":"time_limit"}]}
        """;
    private static final String TIME_LIMIT_STREAM = """
        data: {"choices":[{"index":0,"delta":{"role":"assistant","content":"Partial answer"},"finish_reason":"time_limit"}]}

        data: [DONE]

        """;

    @ParameterizedTest
    @MethodSource("timeouts")
    void should_keep_http_timeout_independent_of_sync_generation_limit(Long defaultLimit, Long requestLimit, Duration timeout) {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(TIME_LIMIT_RESPONSE);
        mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

        withWatsonxServiceMock(() -> {
            var service = service(defaultLimit, timeout);
            service.chat(request(requestLimit));

            assertRequest(defaultLimit, requestLimit, timeout);
        });
    }

    @ParameterizedTest
    @MethodSource("timeouts")
    void should_keep_http_timeout_independent_of_streaming_generation_limit(Long defaultLimit, Long requestLimit, Duration timeout) {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));
        var failure = new IOException("Transport failed");
        when(mockSecureHttpClient.sendAsync(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(failedFuture(failure));

        withWatsonxServiceMock(() -> {
            var service = service(defaultLimit, timeout);
            var response = service.chatStreaming(request(requestLimit), (partial, chunk) -> {});
            var error = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));

            assertSame(failure, error.getCause());
            assertRequest(defaultLimit, requestLimit, timeout);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_receive_time_limit_response_after_generation_limit(boolean streaming) throws Exception {
        stubDelayedResponse(streaming, 200, streaming ? TIME_LIMIT_STREAM : TIME_LIMIT_RESPONSE);
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        var service = delayedResponseService();
        var request = request(100L);
        var response = streaming
            ? service.chatStreaming(request, (partial, chunk) -> {}).get(5, TimeUnit.SECONDS)
            : service.chat(request);

        assertEquals(FinishReason.TIME_LIMIT, response.finishReason());
        assertEquals("Partial answer", response.toAssistantMessage().content());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_receive_server_error_after_generation_limit(boolean streaming) {
        stubDelayedResponse(streaming, 500, """
            {"errors":[{"code":"downstream_request_failed","message":"context deadline exceeded"}]}
            """);
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        var service = delayedResponseService();
        var request = request(100L);
        WatsonxException error;
        if (streaming) {
            var response = service.chatStreaming(request, (partial, chunk) -> {});
            var failure = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));
            error = assertInstanceOf(WatsonxException.class, failure.getCause());
        } else {
            error = assertThrows(WatsonxException.class, () -> service.chat(request));
        }

        assertEquals(500, error.statusCode());
        assertEquals("downstream_request_failed", error.details().orElseThrow().errors().get(0).code());
    }

    private static Stream<Arguments> timeouts() {
        return Stream.of(
            Arguments.of(null, null, Duration.ofSeconds(60)),
            Arguments.of(null, 2000L, Duration.ofSeconds(60)),
            Arguments.of(null, 50000L, Duration.ofSeconds(60)),
            Arguments.of(null, 60000L, Duration.ofSeconds(60)),
            Arguments.of(null, 120000L, Duration.ofSeconds(60)),
            Arguments.of(4000L, null, Duration.ofSeconds(1)),
            Arguments.of(90000L, 2000L, Duration.ofSeconds(1)),
            Arguments.of(null, Long.MAX_VALUE, Duration.ofSeconds(60)));
    }

    private ChatService service(Long defaultLimit, Duration timeout) {
        return ChatService.builder().baseUrl(CloudRegion.DALLAS).authenticator(mockAuthenticator).projectId("project").modelId("model")
            .timeout(timeout).parameters(parameters(defaultLimit)).build();
    }

    private ChatService delayedResponseService() {
        return ChatService.builder().baseUrl("http://localhost:%d".formatted(wireMock.getPort())).version(API_VERSION)
            .authenticator(mockAuthenticator)
            .projectId("project")
            .modelId("model").timeout(Duration.ofSeconds(1)).build();
    }

    private void stubDelayedResponse(boolean streaming, int status, String body) {
        var path = streaming ? "chat_stream" : "chat";
        wireMock.stubFor(post("/ml/v1/text/%s?version=%s".formatted(path, API_VERSION))
            .willReturn(aResponse().withStatus(status).withFixedDelay(300)
                .withHeader("Content-Type", streaming && status == 200 ? "text/event-stream" : "application/json").withBody(body)));
    }

    private static ChatRequest request(Long timeLimit) {
        return ChatRequest.builder().messages(UserMessage.text("Hello")).parameters(parameters(timeLimit)).build();
    }

    private static ChatParameters parameters(Long timeLimit) {
        return ChatParameters.builder().timeLimit(timeLimit == null ? null : Duration.ofMillis(timeLimit)).build();
    }

    private void assertRequest(Long defaultLimit, Long requestLimit, Duration expectedTimeout) {
        assertEquals(expectedTimeout, mockHttpRequest.getValue().timeout().orElseThrow());
        var body = Json.fromJson(bodyPublisherToString(mockHttpRequest), Map.class);
        var expectedLimit = requestLimit == null ? defaultLimit : requestLimit;
        if (expectedLimit == null)
            assertFalse(body.containsKey("time_limit"));
        else
            assertEquals(expectedLimit.longValue(), ((Number) body.get("time_limit")).longValue());
    }
}
