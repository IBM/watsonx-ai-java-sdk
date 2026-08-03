/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.StreamOptions;

@SuppressWarnings("unchecked")
public class ModelGatewayServiceTest extends AbstractWatsonxTest {

    private static final String SIMPLE_JSON_RESPONSE =
        """
            {
                "id": "chatcmpl-abc",
                "object": "chat.completion",
                "model": "gpt-4o",
                "choices": [ {
                    "index": 0,
                    "message": { "role": "assistant", "content": "Hi!" },
                    "finish_reason": "stop"
                } ],
                "created": 1749288614,
                "usage": { "completion_tokens": 2, "prompt_tokens": 5, "total_tokens": 7 }
            }""";

    @Test
    void should_send_openai_compatible_payload_and_parse_response() {

        withWatsonxServiceMock(() -> {

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .timeout(Duration.ofSeconds(60))
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version("1988-03-23")
                .build();

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant"),
                UserMessage.text("Hello"));

            var parameters = ModelGatewayParameters.builder()
                .temperature(0.0)
                .maxCompletionTokens(0)
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "id": "chatcmpl-abc",
                        "object": "chat.completion",
                        "model": "gpt-4o",
                        "choices": [ {
                            "index": 0,
                            "message": { "role": "assistant", "content": "Hi!" },
                            "finish_reason": "stop"
                        } ],
                        "created": 1749288614,
                        "usage": { "completion_tokens": 2, "prompt_tokens": 5, "total_tokens": 7 }
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = modelGatewayService.chat(
                ModelGatewayChatRequest.builder().messages(messages).parameters(parameters).build());

            assertEquals("chatcmpl-abc", chatResponse.id());
            assertEquals("gpt-4o", chatResponse.model());
            assertEquals("Hi!", chatResponse.choices().get(0).message().content());
            assertEquals("stop", chatResponse.choices().get(0).finishReason());
            assertNotNull(chatResponse.usage());
            assertEquals(7, chatResponse.usage().totalTokens());

            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals(
                "http://my-cloud-instance.com/ml/gateway/v1/chat/completions?version=1988-03-23",
                actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody =
                """
                    {
                        "model": "gpt-4o",
                        "messages": [
                            { "role": "system", "content": "You are a helpful assistant" },
                            { "role": "user", "content": [ { "type": "text", "text": "Hello" } ] }
                        ],
                        "max_completion_tokens": 0,
                        "temperature": 0.0
                    }""";

            JSONAssert.assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_stream_and_enable_usage_by_default() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("text/event-stream"))
            .withRequestBody(equalToJson(
                """
                    {
                        "model": "gpt-4o",
                        "messages": [ { "role": "user", "content": [ { "type": "text", "text": "Come stai?" } ] } ],
                        "max_completion_tokens": 0,
                        "temperature": 0.0,
                        "stream": true,
                        "stream_options": { "include_usage": true }
                    }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(8, 200)
                .withBody(
                    """
                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Ciao! Va"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" bene, grazie per aver"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"l"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"o chies"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"to!"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" 😊 Sono q"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ui e"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" pronto "},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ad aiutar"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ti."},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"\\n\\nT"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"u come st"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ai? C"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"'è qualcosa in"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" cui posso es"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"serti utile o"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ggi?"},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"stop","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"","logprobs":null}],"created":1785169730,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":{"prompt_tokens":498,"completion_tokens":75,"total_tokens":573},"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var messages = List.<ChatMessage>of(UserMessage.text("Come stai?"));
        var parameters = ModelGatewayParameters.builder().temperature(0.0).maxCompletionTokens(0).build();

        var expectedText =
            "Ciao! Va bene, grazie per averlo chiesto! 😊 Sono qui e pronto ad aiutarti.\n\nTu come stai? C'è qualcosa in cui posso esserti utile oggi?";

        var partial = new StringBuilder();
        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        modelGatewayService.chatStreaming(
            ModelGatewayChatRequest.builder().messages(messages).parameters(parameters).build(),
            new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    partial.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        // The streaming path delivers a ModelGatewayChatResponse (not a bare TextChatResponse) so gateway-only fields can be read via a cast.
        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS)));
        assertEquals("chatcmpl-12c1b14a-9de3-4b8f-8349-a6260376e194", response.id());
        assertEquals("claude-sonnet-5", response.model());
        assertEquals(expectedText, response.choices().get(0).message().content());
        assertEquals("stop", response.choices().get(0).finishReason());
        assertNotNull(response.usage());
        assertEquals(498, response.usage().promptTokens());
        assertEquals(75, response.usage().completionTokens());
        assertEquals(573, response.usage().totalTokens());
        assertEquals(expectedText, partial.toString());
        // This stream reports service_tier/system_fingerprint as null and cached as false.
        assertNull(response.serviceTier());
        assertNull(response.systemFingerprint());
        assertFalse(response.cached());
    }

    @Test
    void should_not_override_caller_supplied_stream_options() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .withRequestBody(equalToJson(
                """
                    {
                        "model": "gpt-4o",
                        "messages": [ { "role": "user", "content": [ { "type": "text", "text": "Hi" } ] } ],
                        "max_completion_tokens": 0,
                        "temperature": 0.0,
                        "stream": true,
                        "stream_options": { "include_usage": false }
                    }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 100)
                .withBody(
                    """
                        data: {"id":"chatcmpl-2","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Hey"},"finish_reason":"","logprobs":null}],"created":1749736055,"model":"claude-sonnet-5","usage":null,"cached":false}

                        data: {"id":"chatcmpl-2","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"stop","logprobs":null}],"created":1749736055,"model":"claude-sonnet-5","usage":null,"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var parameters = ModelGatewayParameters.builder()
            .temperature(0.0)
            .maxCompletionTokens(0)
            .streamOptions(new StreamOptions(false))
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        modelGatewayService.chatStreaming(
            ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).parameters(parameters).build(),
            new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS)));
        assertEquals("Hey", response.choices().get(0).message().content());
        assertEquals("stop", response.choices().get(0).finishReason());
        assertFalse(response.cached());
    }

    @Test
    void should_stream_parallel_tool_calls_in_openai_format() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(10, 200)
                .withBody(
                    """
                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"I'll get"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" the current time for"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" Rome and Germ"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"any."},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" Note that the"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" t"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ool only supports"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" Italy and Germany as count"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ries, so I won"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"'t be able to retrieve"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" the"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" time for Holland/"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Netherlands direct"},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"ly."},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"tooluse_Y4xYRO50dSiWALo4BWdPQY","type":"function","function":{"name":"get_current_time","arguments":""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"{\\"c"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"ity\\": \\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"Rome\\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":", \\"c"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"oun"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"try\\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":": \\"It"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":0,"id":"","type":"function","function":{"name":"","arguments":"alia\\"}"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"tooluse_j459T3a0VrxhZg0MODRLgG","type":"function","function":{"name":"get_current_time","arguments":""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"{\\"city\\": \\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"Berlin"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"\\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":", \\""}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"country\\":"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":" \\"Ger"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"mania"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"","tool_calls":[{"index":1,"id":"","type":"function","function":{"name":"","arguments":"\\"}"}}]},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"tool_calls","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":null,"cached":false}

                        data: {"id":"chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"","logprobs":null}],"created":1785169639,"model":"claude-sonnet-5","system_fingerprint":null,"service_tier":null,"usage":{"prompt_tokens":508,"completion_tokens":346,"total_tokens":854},"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("claude-sonnet-5")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var tools = List.of(
            Tool.of(
                "get_current_time",
                "get the current time in a city",
                JsonSchema.object()
                    .property("city", JsonSchema.string())
                    .property("country", JsonSchema.string())));

        var expectedText =
            "I'll get the current time for Rome and Germany. Note that the tool only supports Italy and Germany as countries, "
                + "so I won't be able to retrieve the time for Holland/Netherlands directly.";

        List<PartialToolCall> partialToolCalls = new ArrayList<>();
        List<CompletedToolCall> completeToolCalls = new ArrayList<>();

        var messages = List.<ChatMessage>of(UserMessage.text("What time is it in Rome and in the Netherlands?"));

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        modelGatewayService.chatStreaming(messages, tools, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                result.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                result.completeExceptionally(error);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                partialToolCalls.add(partialToolCall);
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                completeToolCalls.add(completeToolCall);
            }
        });

        // The tool-call path rebuilds the response via toBuilder().choices(...).build(); the covariant builder must preserve the
        // ModelGatewayChatResponse
        // type.
        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS)));
        assertEquals("chatcmpl-c09d447b-3fcd-43cb-9ace-f01e89e0380e", response.id());
        assertEquals("claude-sonnet-5", response.model());
        assertEquals(1785169639, response.created());
        assertEquals(expectedText, response.choices().get(0).message().content());
        assertEquals("tool_calls", response.choices().get(0).finishReason());

        assertNotNull(response.usage());
        assertEquals(508, response.usage().promptTokens());
        assertEquals(346, response.usage().completionTokens());
        assertEquals(854, response.usage().totalTokens());

        var toolCalls = response.choices().get(0).message().toolCalls();
        assertEquals(2, toolCalls.size());
        assertEquals(new ToolCall(0, "tooluse_Y4xYRO50dSiWALo4BWdPQY", "function",
            new FunctionCall("get_current_time", "{\"city\": \"Rome\", \"country\": \"Italia\"}")), toolCalls.get(0));
        assertEquals(new ToolCall(1, "tooluse_j459T3a0VrxhZg0MODRLgG", "function",
            new FunctionCall("get_current_time", "{\"city\": \"Berlin\", \"country\": \"Germania\"}")), toolCalls.get(1));

        // The trailing usage chunk still carries a populated choices[0]; onCompleteToolCall must fire exactly twice, not three times.
        assertEquals(2, completeToolCalls.size());
        assertEquals("tooluse_Y4xYRO50dSiWALo4BWdPQY", completeToolCalls.get(0).toolCall().id());
        assertEquals("tooluse_j459T3a0VrxhZg0MODRLgG", completeToolCalls.get(1).toolCall().id());
        assertNull(response.choices().get(0).message().refusal());
    }

    @Test
    void should_populate_gateway_only_fields_when_reported() throws Exception {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 100)
                .withBody(
                    """
                        data: {"id":"chatcmpl-tier","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Hi"},"finish_reason":"","logprobs":null}],"created":1749736055,"model":"gpt-4o","system_fingerprint":"fp_abc123","service_tier":"default","usage":null,"cached":true}

                        data: {"id":"chatcmpl-tier","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":""},"finish_reason":"stop","logprobs":null}],"created":1749736055,"model":"gpt-4o","system_fingerprint":"fp_abc123","service_tier":"default","usage":{"prompt_tokens":10,"completion_tokens":1,"total_tokens":11},"cached":true}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        modelGatewayService.chatStreaming(
            ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build(),
            new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS)));
        assertEquals("Hi", response.choices().get(0).message().content());
        assertEquals("default", response.serviceTier());
        assertEquals("fp_abc123", response.systemFingerprint());
        assertTrue(response.cached());
    }

    /**
     * The gateway propagates OpenAI-compatible error bodies of the shape {@code {"error":{"code":...,"message":...,"request_id":...}}}. A non-2xx
     * response must surface as a {@link WatsonxException} carrying the status code, the {@code request_id} as the trace, and the mapped code/message.
     */
    @Test
    void should_throw_watsonx_exception_on_gateway_error() {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                        {
                            "error": {
                                "code": "model_not_found",
                                "message": "The model `foo` does not exist.",
                                "request_id": "req-12345"
                            }
                        }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("foo")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var ex = assertThrows(WatsonxException.class, () -> modelGatewayService.chat("Hi"));
        assertEquals(404, ex.statusCode());
        assertNotNull(ex.details().orElse(null));
        assertEquals("req-12345", ex.details().orElse(null).trace());
        assertEquals(1, ex.details().orElse(null).errors().size());
        assertEquals("model_not_found", ex.details().orElse(null).errors().get(0).code());
        assertEquals("The model `foo` does not exist.", ex.details().orElse(null).errors().get(0).message());
    }

    @Test
    void should_throw_when_model_id_is_missing() {
        var ex = assertThrows(NullPointerException.class, () -> ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(URI.create("http://localhost"))
            .build());
        assertEquals("The modelId must be provided", ex.getMessage());
    }

    @Test
    void should_throw_when_authenticator_is_missing() {
        var ex = assertThrows(NullPointerException.class, () -> ModelGatewayService.builder()
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost"))
            .build());
        assertEquals("authenticator cannot be null", ex.getMessage());
    }

    @Test
    void should_apply_message_interceptor_on_sync_chat() {

        withWatsonxServiceMock(() -> {

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(API_VERSION)
                .messageInterceptor((ctx, message) -> message.toUpperCase())
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "id": "chatcmpl-abc",
                        "object": "chat.completion",
                        "model": "gpt-4o",
                        "choices": [ {
                            "index": 0,
                            "message": { "role": "assistant", "content": "hi there" },
                            "finish_reason": "stop"
                        } ],
                        "created": 1749288614,
                        "usage": { "completion_tokens": 2, "prompt_tokens": 5, "total_tokens": 7 }
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = modelGatewayService.chat("Hi");
            assertEquals("HI THERE", chatResponse.choices().get(0).message().content());
        });
    }

    @Test
    void should_apply_tool_interceptor_on_sync_chat() {

        withWatsonxServiceMock(() -> {

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(API_VERSION)
                .toolInterceptor((ctx, fc) -> fc.withArguments(fc.arguments().replace("Paris", "Rome")))
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "id": "chatcmpl-tool",
                        "object": "chat.completion",
                        "model": "gpt-4o",
                        "choices": [ {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [ {
                                    "id": "call_1",
                                    "type": "function",
                                    "function": { "name": "get_weather", "arguments": "{\\"city\\": \\"Paris\\"}" }
                                } ]
                            },
                            "finish_reason": "tool_calls"
                        } ],
                        "created": 1749288614,
                        "usage": { "completion_tokens": 2, "prompt_tokens": 5, "total_tokens": 7 }
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = modelGatewayService.chat("What's the weather in Paris?");
            var toolCall = chatResponse.choices().get(0).message().toolCalls().get(0);
            assertEquals("get_weather", toolCall.function().name());
            assertEquals("{\"city\": \"Rome\"}", toolCall.function().arguments());
        });
    }

    @Test
    void should_stream_with_consumer_handler() {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 100)
                .withBody(
                    """
                        data: {"id":"chatcmpl-consumer","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":"","logprobs":null}],"created":1,"model":"gpt-4o","usage":null,"cached":false}

                        data: {"id":"chatcmpl-consumer","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":"stop","logprobs":null}],"created":1,"model":"gpt-4o","usage":null,"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var partial = new StringBuilder();
        Consumer<String> consumer = partial::append;

        var future = modelGatewayService.chatStreaming("Hi", consumer);
        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS)));

        assertEquals("Hello world", partial.toString());
        assertEquals("Hello world", response.choices().get(0).message().content());
    }

    @Test
    void should_stream_with_tools_and_consumer_handler() {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 100)
                .withBody(
                    """
                        data: {"id":"chatcmpl-consumer2","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Hi!"},"finish_reason":"stop","logprobs":null}],"created":1,"model":"gpt-4o","usage":null,"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();

        var tools = List.of(Tool.of("get_weather", "get the weather", JsonSchema.object().property("city", JsonSchema.string())));

        var partial = new StringBuilder();
        Consumer<String> consumer = partial::append;

        var future = modelGatewayService.chatStreaming(List.<ChatMessage>of(UserMessage.text("Hi")), tools, consumer);
        assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
        assertEquals("Hi!", partial.toString());
    }

    @Test
    void should_chat_with_varargs_tools() {

        withWatsonxServiceMock(() -> {

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(API_VERSION)
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(SIMPLE_JSON_RESPONSE);

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var tool = Tool.of("get_weather", "get the weather", JsonSchema.object().property("city", JsonSchema.string()));
            var chatResponse = modelGatewayService.chat(List.<ChatMessage>of(UserMessage.text("Hi")), tool);

            assertEquals("Hi!", chatResponse.choices().get(0).message().content());
            assertTrue(bodyPublisherToString(mockHttpRequest).contains("get_weather"));
        });
    }

    @Test
    void should_apply_default_tools_and_parameters_from_builder() {

        withWatsonxServiceMock(() -> {

            var defaultTool = Tool.of("get_weather", "get the weather", JsonSchema.object().property("city", JsonSchema.string()));

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(API_VERSION)
                .tools(defaultTool)
                .parameters(ModelGatewayParameters.builder().temperature(0.5).maxCompletionTokens(10).build())
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(SIMPLE_JSON_RESPONSE);

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            modelGatewayService.chat("Hi");

            var body = bodyPublisherToString(mockHttpRequest);
            assertTrue(body.contains("get_weather"), "default tool should be forwarded");
            assertTrue(body.contains("\"temperature\":0.5"), "default temperature should be forwarded");
            assertTrue(body.contains("\"max_completion_tokens\":10"), "default max tokens should be forwarded");
        });
    }

    @Test
    void should_apply_default_executable_tools_from_builder() {

        withWatsonxServiceMock(() -> {

            var executableTool = new ExecutableTool() {
                @Override
                public String name() {
                    return "exec_weather";
                }

                @Override
                public Tool schema() {
                    return Tool.of("exec_weather", "get the weather", JsonSchema.object().property("city", JsonSchema.string()));
                }

                @Override
                public String execute(ToolArguments args) {
                    return "sunny";
                }
            };

            var modelGatewayService = ModelGatewayService.builder()
                .authenticator(mockAuthenticator)
                .modelId("gpt-4o")
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(API_VERSION)
                .tools(executableTool)
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(SIMPLE_JSON_RESPONSE);

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            modelGatewayService.chat("Hi");
            assertTrue(bodyPublisherToString(mockHttpRequest).contains("exec_weather"));
        });
    }

    @Test
    void should_stream_with_log_responses_enabled() {

        wireMock.stubFor(post("/ml/gateway/v1/chat/completions?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("text/event-stream"))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 100)
                .withBody(
                    """
                        data: {"id":"chatcmpl-log","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"Logged"},"finish_reason":"stop","logprobs":null}],"created":1,"model":"gpt-4o","usage":null,"cached":false}

                        data: [DONE]
                        """)));

        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("my-super-token"));

        var modelGatewayService = ModelGatewayService.builder()
            .authenticator(mockAuthenticator)
            .modelId("gpt-4o")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .logRequests(true)
            .logResponses(true)
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        modelGatewayService.chatStreaming(
            ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build(),
            new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        var response = assertInstanceOf(ModelGatewayChatResponse.class, assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS)));
        assertEquals("Logged", response.choices().get(0).message().content());
    }
}
