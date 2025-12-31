/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.timeseries.TimeSeriesRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
public class DeploymentServiceTest extends AbstractWatsonxTest {

    @RegisterExtension
    WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    @BeforeEach
    void setup() {
        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("token"));
    }

    @Test
    void should_generate_text_with_deployment_id_and_parameters() throws Exception {

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            TextGenerationParameters parameters = TextGenerationParameters.builder()
                .modelId("model-id")
                .projectId("project-id")
                .spaceId("space-id")
                .timeLimit(Duration.ofSeconds(10))
                .build();

            String input = "how far is paris from bangalore:";
            TextRequest EXPECTED_BODY =
                new TextRequest(null, null, null, input, parameters.toSanitized(), null);

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn("""
                {
                  "model_id": "google/flan-ul2",
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "results": [
                    {
                      "generated_text": "4,000 km",
                      "generated_token_count": 4,
                      "input_token_count": 12,
                      "stop_reason": "eos_token"
                    }
                  ]
                }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var request = TextGenerationRequest.builder()
                .input(input)
                .parameters(parameters)
                .deploymentId("my-deployment-id")
                .build();

            var response = deploymentService.generate(request);
            assertEquals("google/flan-ul2", response.modelId());
            assertEquals("2023-07-21T16:52:32.190Z", response.createdAt());
            assertEquals("4,000 km", response.results().get(0).generatedText());
            assertEquals(4, response.results().get(0).generatedTokenCount());
            assertEquals(12, response.results().get(0).inputTokenCount());
            assertEquals("eos_token", response.results().get(0).stopReason());

            JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(mockHttpRequest), true);
            assertEquals(
                URI.create(CloudRegion.DALLAS.getMlEndpoint()
                    .concat("/ml/v1/deployments/my-deployment-id/text/generation?version=%s".formatted(API_VERSION))),
                mockHttpRequest.getValue().uri()
            );
        });
    }

    @Test
    void should_generate_text_with_prompt_template_variables() throws Exception {

        withWatsonxServiceMock(() -> {

            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            TextGenerationParameters parameters = TextGenerationParameters.builder()
                .promptVariables(Map.of("city", "paris"))
                .transactionId("my-transaction-id")
                .timeLimit(Duration.ofSeconds(10))
                .build();

            TextRequest EXPECTED_BODY =
                new TextRequest(null, null, null, null, parameters.toSanitized(), null);

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn("""
                {
                  "model_id": "google/flan-ul2",
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "results": [
                    {
                      "generated_text": "4,000 km",
                      "generated_token_count": 4,
                      "input_token_count": 12,
                      "stop_reason": "eos_token"
                    }
                  ]
                }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var request = TextGenerationRequest.builder()
                .parameters(parameters)
                .deploymentId("my-deployment-id")
                .build();

            var response = deploymentService.generate(request);
            assertEquals("google/flan-ul2", response.modelId());
            assertEquals("2023-07-21T16:52:32.190Z", response.createdAt());
            assertEquals("4,000 km", response.results().get(0).generatedText());
            assertEquals(4, response.results().get(0).generatedTokenCount());
            assertEquals(12, response.results().get(0).inputTokenCount());
            assertEquals("eos_token", response.results().get(0).stopReason());
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");

            JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(mockHttpRequest), true);
            assertEquals(
                URI.create(CloudRegion.DALLAS.getMlEndpoint()
                    .concat("/ml/v1/deployments/my-deployment-id/text/generation?version=%s".formatted(API_VERSION))),
                mockHttpRequest.getValue().uri()
            );
        });
    }

    @Test
    void should_stream_text_generation_response() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        TextGenerationParameters parameters = TextGenerationParameters.builder()
            .modelId("model-id")
            .projectId("project-id")
            .spaceId("space-id")
            .transactionId("my-transaction-id")
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/generation_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson(
                """
                    {
                      "input": "how far is paris from bangalore:",
                      "parameters": {}
                    }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.500Z","results":[{"generated_text":"","generated_token_count":0,"input_token_count":68,"stop_reason":"not_finished"}],"system":{"warnings":[{"message":"Model 'ibm/granite-13b-instruct-v2' is in deprecated state from 2025-06-18. It will be in withdrawn state from 2025-10-15. IDs of alternative models: ibm/granite-3-3-8b-instruct.","id":"deprecation_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-model-lifecycle.html?context=wx&audience=wdp"},{"message":"This API is legacy. Please consider using '/ml/v1/text/chat_stream' instead.","id":"api_legacy"}]}}

                        id: 2
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.516Z","results":[{"generated_text":"40","generated_token_count":1,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 3
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.528Z","results":[{"generated_text":"00","generated_token_count":2,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 4
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.540Z","results":[{"generated_text":"km","generated_token_count":3,"input_token_count":0,"stop_reason":"eos_token"}]}
                        """)));


        var request = TextGenerationRequest.builder()
            .input("how far is paris from bangalore:")
            .parameters(parameters)
            .deploymentId("my-deployment-id")
            .build();

        CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
        deploymentService.generateStreaming(request,
            new TextGenerationHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    assertTrue(partialResponse.equals("40") || partialResponse.equals("00") || partialResponse.equals("km"));
                }

                @Override
                public void onCompleteResponse(TextGenerationResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        TextGenerationResponse response = assertDoesNotThrow(() -> result.get(10, TimeUnit.SECONDS));
        assertNotNull(response);
        assertEquals("4000km", response.toText());
        assertEquals("ibm/granite-13b-instruct-v2", response.modelId());
        assertEquals(6, response.results().get(0).generatedTokenCount());
        assertEquals(68, response.results().get(0).inputTokenCount());
    }

    @Test
    void should_stream_text_generation_with_prompt_template() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        TextGenerationParameters parameters = TextGenerationParameters.builder()
            .promptVariables(Map.of("city", "paris"))
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/generation_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withRequestBody(equalToJson(
                """
                      {
                      "input": "how far is paris from bangalore:",
                      "parameters": {
                            "prompt_variables": {
                                "city": "paris"
                            }
                      }
                    }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.500Z","results":[{"generated_text":"","generated_token_count":0,"input_token_count":68,"stop_reason":"not_finished"}],"system":{"warnings":[{"message":"Model 'ibm/granite-13b-instruct-v2' is in deprecated state from 2025-06-18. It will be in withdrawn state from 2025-10-15. IDs of alternative models: ibm/granite-3-3-8b-instruct.","id":"deprecation_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-model-lifecycle.html?context=wx&audience=wdp"},{"message":"This API is legacy. Please consider using '/ml/v1/text/chat_stream' instead.","id":"api_legacy"}]}}

                        id: 2
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.516Z","results":[{"generated_text":"40","generated_token_count":1,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 3
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.528Z","results":[{"generated_text":"00","generated_token_count":2,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 4
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.540Z","results":[{"generated_text":"km","generated_token_count":3,"input_token_count":0,"stop_reason":"eos_token"}]}
                        """)));


        var request = TextGenerationRequest.builder()
            .input("how far is paris from bangalore:")
            .parameters(parameters)
            .deploymentId("my-deployment-id")
            .build();

        CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
        deploymentService.generateStreaming(request,
            new TextGenerationHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    assertTrue(partialResponse.equals("40") || partialResponse.equals("00") || partialResponse.equals("km"));
                }

                @Override
                public void onCompleteResponse(TextGenerationResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        TextGenerationResponse response = assertDoesNotThrow(() -> result.get(10, TimeUnit.SECONDS));
        assertNotNull(response);
        assertEquals("4000km", response.toText());
        assertEquals("ibm/granite-13b-instruct-v2", response.modelId());
        assertEquals(6, response.results().get(0).generatedTokenCount());
        assertEquals(68, response.results().get(0).inputTokenCount());

        deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .logResponses(true)
            .build();


        deploymentService.generateStreaming(request,
            new TextGenerationHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    assertTrue(partialResponse.equals("40") || partialResponse.equals("00") || partialResponse.equals("km"));
                }

                @Override
                public void onCompleteResponse(TextGenerationResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });
        response = assertDoesNotThrow(() -> result.get(10, TimeUnit.SECONDS));
        assertNotNull(response);
        assertEquals("4000km", response.toText());
        assertEquals("ibm/granite-13b-instruct-v2", response.modelId());
        assertEquals(6, response.results().get(0).generatedTokenCount());
        assertEquals(68, response.results().get(0).inputTokenCount());
        // Wait for logs.
        Thread.sleep(200);
    }

    @Test
    void should_chat_with_deployment_and_return_response() throws Exception {

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            ChatParameters parameters = ChatParameters.builder()
                .modelId("model-id")
                .projectId("project-id")
                .spaceId("space-id")
                .transactionId("my-transaction-id")
                .build();

            TextChatRequest EXPECTED_BODY = TextChatRequest.builder()
                .messages(List.of(UserMessage.text("Hello")))
                .timeLimit(60000L)
                .build();

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn("""
                {
                  "id": "cmpl-15475d0dea9b4429a55843c77997f8a9",
                  "model_id": "ibm/granite-3-2b-instruct",
                  "created": 1689958352,
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "The 2020 World Series was played at the Globe Life Field in Arlington, Texas.\\n"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "completion_tokens": 27,
                    "prompt_tokens": 186,
                    "total_tokens": 213
                  }
                }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var request = ChatRequest.builder()
                .messages(List.of(UserMessage.text("Hello")))
                .parameters(parameters)
                .deploymentId("my-deployment-id")
                .build();

            var response = deploymentService.chat(request);
            assertEquals("cmpl-15475d0dea9b4429a55843c77997f8a9", response.id());
            assertEquals("ibm/granite-3-2b-instruct", response.modelId());
            assertEquals("2023-07-21T16:52:32.190Z", response.createdAt());
            assertEquals(1689958352, response.created());
            assertEquals(27, response.usage().completionTokens());
            assertEquals(186, response.usage().promptTokens());
            assertEquals(213, response.usage().totalTokens());
            assertEquals(1, response.choices().size());
            assertEquals("The 2020 World Series was played at the Globe Life Field in Arlington, Texas.\n",
                response.choices().get(0).message().content());
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");

            JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(mockHttpRequest), true);
            assertEquals(
                URI.create(CloudRegion.DALLAS.getMlEndpoint()
                    .concat("/ml/v1/deployments/my-deployment-id/text/chat?version=%s".formatted(API_VERSION))),
                mockHttpRequest.getValue().uri()
            );

            request = ChatRequest.builder()
                .messages(List.of(UserMessage.text("Hello")))
                .deploymentId("my-deployment-id")
                .build();

            assertEquals("cmpl-15475d0dea9b4429a55843c77997f8a9", response.id());
        });
    }

    @Test
    void should_stream_chat_response() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson("""
                {
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are an expert translator and you give the translation of single words"
                        },
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Translate \\"Hello\\" in Italian"
                                }
                            ]
                        }
                    ],
                    "max_completion_tokens": 0,
                    "temperature": 0,
                    "context": "test",
                    "time_limit": 60000
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"C"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.542Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"iao"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":"stop","delta":{"content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.563Z"}

                        id: 5
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.564Z","usage":{"completion_tokens":3,"prompt_tokens":38,"total_tokens":41}}
                        """)));

        var messages = List.<ChatMessage>of(
            SystemMessage.of("You are an expert translator and you give the translation of single words"),
            UserMessage.text("Translate \"Hello\" in Italian")
        );

        var chatParameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .temperature(0.0)
            .context("test")
            .transactionId("my-transaction-id")
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        var chatHandler = new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                assertNotNull(partialChatResponse.choices());
                assertEquals(1, partialChatResponse.choices().size());
                var chunk = partialChatResponse.choices().get(0).delta().content();
                assertTrue(chunk.equals(partialResponse));
                assertTrue(chunk.equals("C") || chunk.equals("iao"));
            }

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
                fail();
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                fail();
            }
        };

        var request = ChatRequest.builder()
            .messages(messages)
            .parameters(chatParameters)
            .deploymentId("my-deployment-id")
            .build();

        deploymentService.chatStreaming(request, chatHandler);
        ChatResponse response = assertDoesNotThrow(() -> result.get(30, TimeUnit.SECONDS));
        assertNotNull(response);
        assertNotNull(response.choices());
        assertEquals(1, response.choices().size());
        assertEquals("stop", response.choices().get(0).finishReason());
        assertEquals(0, response.choices().get(0).index());
        assertEquals("Ciao", response.choices().get(0).message().content());
        assertEquals("Ciao", response.toAssistantMessage().content());
        assertNotNull(response.created());
        assertNotNull(response.createdAt());
        assertNotNull(response.id());
        assertNotNull(response.model());
        assertNotNull(response.modelId());
        assertNotNull(response.modelVersion());
        assertNotNull(response.object());
        assertNotNull(response.usage());
        assertNotNull(response.usage().completionTokens());
        assertNotNull(response.usage().promptTokens());
        assertNotNull(response.usage().totalTokens());

        deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .logResponses(true)
            .build();

        chatParameters = ChatParameters.builder()
            .modelId("model-id")
            .spaceId("space-id")
            .projectId("project-id")
            .maxCompletionTokens(0)
            .temperature(0.0)
            .context("test")
            .transactionId("my-transaction-id")
            .build();

        request = ChatRequest.builder()
            .messages(messages)
            .parameters(chatParameters)
            .deploymentId("my-deployment-id")
            .build();

        deploymentService.chatStreaming(request, chatHandler);
        response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        assertNotNull(response);
    }

    @Test
    void should_stream_chat_response_with_thinking() throws Exception {

        String BODY = new String(ClassLoader.getSystemResourceAsStream("granite_thinking_streaming_response.txt").readAllBytes());

        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withRequestBody(equalToJson("""
                {
                    "messages": [
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Translate \\"Hello\\" in Italian"
                                }
                            ]
                        }
                    ],
                    "chat_template_kwargs" : {
                        "thinking" : true
                    },
                    "time_limit": 60000
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(159, 200)
                .withBody(BODY)));

        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("my-super-token"));

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        ChatRequest chatRequest = ChatRequest.builder()
            .messages(UserMessage.text("Translate \"Hello\" in Italian"))
            .thinking(ExtractionTags.of("think", "response"))
            .deploymentId("my-deployment-id")
            .build();

        StringBuilder thinkingResponse = new StringBuilder();
        StringBuilder response = new StringBuilder();


        deploymentService.chatStreaming(chatRequest, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                response.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                result.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                result.completeExceptionally(error);
            }

            @Override
            public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
                thinkingResponse.append(partialThinking);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                fail();
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                fail();
            }
        }).get(3, TimeUnit.SECONDS);

        var chatResponse = result.get();
        assertEquals(thinkingResponse.toString(), chatResponse.toAssistantMessage().thinking());
        assertEquals(response.toString(), chatResponse.toAssistantMessage().content());

        assertEquals(
            "The translation of \"Hello\" in Italian is straightforward. \"Hello\" in English directly translates to \"Ciao\" in Italian, which is a common informal greeting. For a more formal context, \"Buongiorno\" can be used, meaning \"Good day.\" However, since the request is for a direct translation of \"Hello,\" \"Ciao\" is the most appropriate response.",
            thinkingResponse.toString()
        );

        assertTrue(response.toString().contains(
            "This is the informal equivalent, widely used in everyday conversation. For a formal greeting, one would say \"Buongiorno,\" but given the direct translation request, \"Ciao\" is the most fitting response."));


    }

    @Test
    void should_forecast_time_series_with_deployment() throws Exception {

        var EXPECTED = """
            {
                  "model_id": "ibm/ttm-1024-96-r2",
                  "created_at": "2020-05-02T16:27:51Z",
                  "results": [
                    {
                      "date": [
                        "2020-01-05T02:00:00",
                        "2020-01-05T03:00:00",
                        "2020-01-06T00:00:00"
                      ],
                      "ID1": [
                        "D1",
                        "D1",
                        "D1"
                      ],
                      "TARGET1": [
                        1.86,
                        3.24,
                        6.78
                      ]
                    }
                  ],
                  "input_data_points": 512,
                  "output_data_points": 1024
            }""";

        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        InputSchema inputSchema = InputSchema.builder()
            .timestampColumn("date")
            .addIdColumn("ID1")
            .build();

        ForecastData data = ForecastData.create()
            .add("date", "2020-01-01T00:00:00")
            .add("date", "2020-01-01T01:00:00")
            .add("date", "2020-01-05T01:00:00")
            .addAll("ID1", Arrays.asList("D1", "D1", "D1"))
            .addAll("TARGET1", Arrays.asList(1.46, 2.34, 4.55));

        TimeSeriesParameters parameters = TimeSeriesParameters.builder()
            .modelId("modelId")
            .projectId("projectId")
            .spaceId("spaceId")
            .transactionId("my-transaction-id")
            .futureData(
                ForecastData.create()
                    .add("date", "2021-01-01T00:00:00")
                    .add("ID1", "D1")
                    .add("TARGET1", 5)
            )
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/time_series/forecast?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson("""
                {
                    "schema": {
                        "timestamp_column": "date",
                        "id_columns": [ "ID1" ]
                    },
                    "data": {
                        "date": [
                            "2020-01-01T00:00:00",
                            "2020-01-01T01:00:00",
                            "2020-01-05T01:00:00"
                        ],
                        "ID1": [
                            "D1",
                            "D1",
                            "D1"
                        ],
                        "TARGET1": [
                            1.46,
                            2.34,
                            4.55
                        ]
                    },
                    "future_data": {
                        "date": [
                            "2021-01-01T00:00:00"
                        ],
                        "ID1": [
                            "D1"
                        ],
                        "TARGET1": [
                            5
                        ]
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(EXPECTED)));

        var request = TimeSeriesRequest.builder()
            .inputSchema(inputSchema)
            .data(data)
            .parameters(parameters)
            .deploymentId("my-deployment-id")
            .build();

        var result = deploymentService.forecast(request);
        JSONAssert.assertEquals(EXPECTED, toJson(result), true);
    }

    @Test
    void should_find_deployment_by_id_for_prompt_tune() throws Exception {

        var EXPECTED_RESPONSE = """
            {
                "metadata": {
                        "id": "6213cf1-252f-424b-b52d-5cdd9814956c",
                        "created_at": "2023-05-02T16:27:51Z",
                        "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f",
                        "name": "text_classification",
                        "description": "Classification prompt tuned model deployment",
                        "tags": [
                            "classification"
                        ]
                },
                "entity": {
                    "asset": {
                        "id": "4cedab6d-e8e4-4214-b81a-2ddb122db2ab"
                    },
                    "online": {},
                    "deployed_asset_type": "prompt_tune",
                    "base_model_id": "google/flan-ul2",
                    "status": {
                        "state": "ready",
                        "message": {
                            "level": "info",
                            "text": "The deployment is successful"
                    },
                    "inference": [
                        {
                            "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/2cd0bcda-581d-4f04-8028-ec2bc90cc375/text/generation"
                        },
                        {
                            "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/2cd0bcda-581d-4f04-8028-ec2bc90cc375/text/generation_stream",
                            "sse": true
                        }
                    ]
                    }
                }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(EXPECTED_RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var response = deploymentService.findById(
                FindByIdRequest.builder()
                    .projectId("my-project-id")
                    .deploymentId("mysuperdeployment")
                    .build()
            );

            JSONAssert.assertEquals(EXPECTED_RESPONSE, toJson(response), true);
            assertTrue(mockHttpRequest.getValue().uri().toString().contains("/mysuperdeployment"));
            assertTrue(mockHttpRequest.getValue().uri().getQuery().contains("my-project-id"));
        });
    }

    @Test
    void should_find_deployment_by_id_for_prompt_template() throws Exception {

        var EXPECTED_RESPONSE = """
            {
                "metadata": {
                    "id": "6213cf1-252f-424b-b52d-5cdd9814956c",
                    "created_at": "2023-05-02T16:27:51Z",
                    "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f",
                    "name": "text_classification",
                    "description": "Classification prompt template deployment",
                    "tags": [
                    "classification"
                    ]
                },
                "entity": {
                    "prompt_template": {
                    "id": "4cedab6d-e8e4-4214-b81a-2ddb122db2ab"
                    },
                    "online": {},
                    "deployed_asset_type": "foundation_model",
                    "base_model_id": "google/flan-t5-xl",
                    "status": {
                    "state": "ready",
                    "message": {
                        "level": "info",
                        "text": "The deployment is successful"
                    },
                    "inference": [
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/2cd0bcda-581d-4f04-8028-ec2bc90cc375/text/generation"
                        },
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/2cd0bcda-581d-4f04-8028-ec2bc90cc375/text/generation_stream",
                        "sse": true
                        }
                    ]
                    }
                }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(EXPECTED_RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var response = deploymentService.findById(
                FindByIdRequest.builder()
                    .spaceId("my-space-id")
                    .deploymentId("my-deployment-id")
                    .build()
            );

            JSONAssert.assertEquals(EXPECTED_RESPONSE, toJson(response), true);
            assertTrue(mockHttpRequest.getValue().uri().toString().contains("/my-deployment-id"));
            assertTrue(mockHttpRequest.getValue().uri().getQuery().contains("my-space-id"));
        });
    }

    @Test
    void should_find_deployment_by_id_for_foundation_model() throws Exception {

        var EXPECTED_RESPONSE = """
            {
                "metadata": {
                    "id": "6213cf1-252f-424b-b52d-5cdd9814956c",
                    "created_at": "2023-05-02T16:27:51Z",
                    "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f",
                    "name": "my_tuned_flan"
                },
                "entity": {
                    "asset": {
                    "id": "366c31e9-1a6b-417a-8e25-06178a1514a1"
                    },
                    "online": {
                    "parameters": {
                        "serving_name": "myflan"
                    }
                    },
                    "deployed_asset_type": "custom_foundation_model",
                    "base_model_id": "google/flan-t5-xl",
                    "status": {
                    "state": "ready",
                    "message": {
                        "level": "info",
                        "text": "The deployment is successful"
                    },
                    "inference": [
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/6213cf1-252f-424b-b52d-5cdd9814956c/text/generation"
                        },
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/myflan/text/generation",
                        "uses_serving_name": true
                        },
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/6213cf1-252f-424b-b52d-5cdd9814956c/text/generation_stream",
                        "sse": true
                        },
                        {
                        "url": "https://us-south.ml.cloud.ibm.com/ml/v1/deployments/myflan/text/generation_stream",
                        "sse": true,
                        "uses_serving_name": true
                        }
                    ]
                    }
                }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(EXPECTED_RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var response = deploymentService.findById(
                FindByIdRequest.builder()
                    .deploymentId("override-deployment")
                    .spaceId("my-space-id")
                    .build()
            );

            JSONAssert.assertEquals(EXPECTED_RESPONSE, toJson(response), true);
            assertFalse(mockHttpRequest.getValue().uri().toString().contains("my-deployment-id"));
            assertTrue(mockHttpRequest.getValue().uri().toString().contains("/override-deployment"));
            assertTrue(mockHttpRequest.getValue().uri().getQuery().contains("my-space-id"));

            assertThrows(IllegalArgumentException.class, () -> deploymentService.findById(
                FindByIdRequest.builder()
                    .deploymentId("override-deployment")
                    .build()
            ), "Either projectId or spaceId must be provided");

            deploymentService.findById(
                FindByIdRequest.builder()
                    .deploymentId("override-deployment")
                    .spaceId("my-space-id")
                    .transactionId("my-transaction-id")
                    .build()
            );
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_convert_deployment_resource_with_all_fields() throws Exception {

        var EXPECTED_RESPONSE = """
            {
                "metadata": {
                    "id": "6213cf1-252f-424b-b52d-5cdd9814956c",
                    "created_at": "2023-05-02T16:27:51Z",
                    "rev": "5",
                    "owner": "user-12345",
                    "modified_at": "2023-05-05T10:15:30Z",
                    "parent_id": "parent-abc",
                    "name": "text_classification",
                    "description": "Classification prompt tuned model deployment",
                    "tags": ["classification", "nlp", "production"],
                    "commit_info": {
                        "committed_at": "2023-05-03T09:00:00Z",
                        "commit_message": "Initial deployment commit"
                    },
                    "space_id": "3fc54cf1-252f-424b-b52d-5cdd9814987f",
                    "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
                },
                "entity": {
                    "online": {
                    "parameters": {
                        "serving_name": "text_classification_deploy",
                        "batch_size": 16
                    }
                    },
                    "custom": {
                    "env": "production",
                    "team": "nlp"
                    },
                    "prompt_template": {
                    "id": "template-123"
                    },
                    "hardware_spec": {
                    "id": "hw-456",
                    "rev": "2",
                    "name": "gpu_m",
                    "num_nodes": 2
                    },
                    "hardware_request": {
                    "size": "gpu_m",
                    "num_nodes": 2
                    },
                    "asset": {
                        "id": "4cedab6d-e8e4-4214-b81a-2ddb122db2ab",
                        "rev": "3",
                        "resource_key": "f52fe20c-a1fe-4e54-9b78-6bf2ff61b455"
                    },
                    "base_model_id": "google/flan-ul2",
                    "deployed_asset_type": "prompt_tune",
                    "verbalizer": "classification_verbalizer",
                    "status": {
                    "state": "ready",
                    "message": {
                        "level": "info",
                        "text": "The deployment is successful"
                    },
                    "failure": {
                        "trace": "3fd543d2-36e0-4f83-9be3-5c6dd498af4f",
                        "errors": [
                        {
                            "code": "missing_field",
                            "message": "The 'name' field is required.",
                            "more_info": "https://cloud.ibm.com/apidocs/machine-learning#models-get",
                            "target": {
                            "type": "field",
                            "name": "name"
                            }
                        }
                        ]
                    },
                    "inference": [
                        {
                        "url": "https://ml.cloud.ibm.com/ml/v1/deployments/abc123/text/generation",
                        "sse": false,
                        "uses_serving_name": true
                        },
                        {
                        "url": "https://ml.cloud.ibm.com/ml/v1/deployments/abc123/text/generation_stream",
                        "sse": true,
                        "uses_serving_name": true
                        }
                    ]
                    },
                    "tooling": {
                    "tool": "custom-ui",
                    "version": "1.2.3"
                    }
                }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(EXPECTED_RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var response = deploymentService.findById(
                FindByIdRequest.builder()
                    .deploymentId("override-deployment")
                    .spaceId("my-space-id")
                    .build()
            );

            JSONAssert.assertEquals(EXPECTED_RESPONSE, toJson(response), true);
        });
    }

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    void should_use_correct_executors() throws Exception {

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"C"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.542Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"iao"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":"stop","delta":{"content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.563Z"}

                        id: 5
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.564Z","usage":{"completion_tokens":3,"prompt_tokens":38,"total_tokens":41}}
                        """)));

        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-token"));

        var deploymentService = DeploymentService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticator(mockAuthenticator)
            .build();

        var request = ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hello")))
            .deploymentId("my-deployment-id")
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        deploymentService.chatStreaming(request, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                assertTrue(Thread.currentThread().getName().startsWith("thread-"));
                assertNotNull(partialResponse);
                assertNotNull(partialChatResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                assertTrue(Thread.currentThread().getName().startsWith("thread-"));
                result.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                fail(error);
            }
        });

        assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
    }

    @Test
    void should_throw_exception_when_deployment_id_not_provided() throws Exception {
        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();


            var textGenerationRequest = TextGenerationRequest.builder()
                .input("test")
                .build();

            var chatRequest = ChatRequest.builder()
                .messages(UserMessage.text("test"))
                .build();

            var forecastRequest = TimeSeriesRequest.builder()
                .inputSchema(InputSchema.builder().timestampColumn("test").build())
                .data(ForecastData.create())
                .build();

            var ex = assertThrows(RuntimeException.class, () -> deploymentService.generate(textGenerationRequest));
            assertEquals(ex.getMessage(), "deploymentId must be provided");
            ex = assertThrows(RuntimeException.class, () -> deploymentService.chat(chatRequest));
            assertEquals(ex.getMessage(), "deploymentId must be provided");
            ex = assertThrows(RuntimeException.class,
                () -> deploymentService.forecast(forecastRequest));
            assertEquals(ex.getMessage(), "deploymentId must be provided");
            ex = assertThrows(RuntimeException.class,
                () -> deploymentService.findById(FindByIdRequest.builder().build()));
            assertEquals(ex.getMessage(), "deploymentId must be provided");
        });
    }

    @Test
    void should_throw_exception_when_http_request_fails() throws Exception {

        when(mockHttpClient.send(any(), any()))
            .thenThrow(new IOException("IOException"))
            .thenThrow(new InterruptedException("InterruptedException"));

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var textGenerationRequest = TextGenerationRequest.builder()
                .deploymentId("my-deployment-id")
                .input("test")
                .build();

            var chatRequest = ChatRequest.builder()
                .deploymentId("my-deployment-id")
                .messages(UserMessage.text("test"))
                .build();

            var forecastRequest = TimeSeriesRequest.builder()
                .deploymentId("my-deployment-id")
                .inputSchema(InputSchema.builder().timestampColumn("test").build())
                .data(ForecastData.create())
                .build();

            var findByIdRequest = FindByIdRequest.builder()
                .deploymentId("my-deployment")
                .projectId("project-id")
                .build();

            var ex = assertThrows(RuntimeException.class, () -> deploymentService.generate(textGenerationRequest));
            assertEquals(ex.getCause().getMessage(), "IOException");
            ex = assertThrows(RuntimeException.class, () -> deploymentService.chat(chatRequest));
            assertEquals(ex.getCause().getMessage(), "InterruptedException");
            ex = assertThrows(RuntimeException.class,
                () -> deploymentService.forecast(forecastRequest));
            assertEquals(ex.getCause().getMessage(), "InterruptedException");
            ex = assertThrows(RuntimeException.class,
                () -> deploymentService.findById(findByIdRequest), "InterruptedException");
            assertEquals(ex.getCause().getMessage(), "InterruptedException");
        });
    }

    @Test
    void should_map_function_call_correctly() {

        String RESPONSE = """
            {
                "id": "chatcmpl-3a7d590b3ec34f89a007f7f16c7c682b",
                "object": "chat.completion",
                "model_id": "ibm/granite-4-h-small",
                "model": "ibm/granite-4-h-small",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "tool_calls": [
                                {
                                    "id": "chatcmpl-tool-ce5a84405758488fb88eab9d50e908d5",
                                    "type": "function",
                                    "function": {
                                        "name": "get_current_time",
                                        "arguments": "\\"{\\\\n  \\\\\\"country\\\\\\": \\\\\\"Italy\\\\\\"\\\\n}\\""
                                    }
                                }
                            ]
                        },
                        "finish_reason": "tool_calls"
                    }
                ],
                "created": 1764688210,
                "model_version": "4.0.0",
                "created_at": "2025-12-02T15:10:11.178Z",
                "usage": {
                    "completion_tokens": 28,
                    "prompt_tokens": 192,
                    "total_tokens": 220
                }
            }""";

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var deploymentService = DeploymentService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(CloudRegion.LONDON)
                .messageInterceptor((ctx, message) -> "New message")
                .toolInterceptor((ctx, fc) -> fc.withArguments(Json.fromJson(fc.arguments(), String.class)))
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var tool = Tool.of("get_current_time", JsonSchema.object().property("country", JsonSchema.string()));
            var messages = List.<ChatMessage>of(UserMessage.text("What time is it in Italy?"));
            var chatRequest = ChatRequest.builder().deploymentId("my-deployment-id").messages(messages).tools(tool).build();
            var chatResponse = deploymentService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertNull(assistantMessage.content());
            JSONAssert.assertEquals("""
                {
                    "id": "chatcmpl-tool-ce5a84405758488fb88eab9d50e908d5",
                    "type": "function",
                    "function": {
                        "name": "get_current_time",
                        "arguments" : "{\\n  \\"country\\": \\"Italy\\"\\n}"
                    }
                }""",
                Json.toJson(assistantMessage.toolCalls().get(0)),
                false
            );
            assistantMessage.processTools((toolName, toolArgs) -> {
                assertEquals("get_current_time", toolName);
                JSONAssert.assertEquals("{ \"country\": \"Italy\" }", Json.toJson(toolArgs), true);
                return false;
            });
        });
    }

    @Test
    void should_override_assistant_content() {

        String RESPONSE = """
            {
                "id": "chatcmpl-622a62745da348948b7668a76a553b30",
                "object": "chat.completion",
                "model_id": "ibm/granite-4-h-small",
                "model": "ibm/granite-4-h-small",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Hello! I'm doing well, thanks for asking. How can I help you today?"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "created": 1764692241,
                "model_version": "4.0.0",
                "created_at": "2025-12-02T16:17:22.255Z",
                "usage": {
                    "completion_tokens": 19,
                    "prompt_tokens": 190,
                    "total_tokens": 209
                }
            }""";

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var deploymentService = DeploymentService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(CloudRegion.LONDON)
                .messageInterceptor((ctx, message) -> "I don't feel good.")
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatRequest = ChatRequest.builder()
                .deploymentId("my-deployment-id")
                .messages(UserMessage.text("What time is it in Italy?"))
                .build();

            var chatResponse = deploymentService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("I don't feel good.", assistantMessage.content());
            assertFalse(assistantMessage.hasToolCalls());
        });
    }

    @Test
    void should_not_override_assistant_content_in_streaming() {

        String REQUEST = """
            {
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "How are you?"
                            }
                        ]
                    }
                ],
                "time_limit": 60000
            }""";

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withRequestBody(equalToJson(REQUEST))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(20, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.590Z","system":{"warnings":[{"message":"The value of 'max_tokens' for this model was set to value 1024","id":"unspecified_max_token","additional_properties":{"limit":0,"new_value":1024,"parameter":"max_tokens","value":0}}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":"Hello"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.590Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":"!"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.610Z"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" I"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.629Z"}

                        id: 5
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":"'m"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.651Z"}

                        id: 6
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" doing"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.670Z"}

                        id: 7
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" well"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.689Z"}

                        id: 8
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":","}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.711Z"}

                        id: 9
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" thank"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.729Z"}

                        id: 10
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" you"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.749Z"}

                        id: 11
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":"."}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.770Z"}

                        id: 12
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" How"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.789Z"}

                        id: 13
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" can"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.808Z"}

                        id: 14
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" I"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.831Z"}

                        id: 15
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" assist"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.851Z"}

                        id: 16
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" you"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.870Z"}

                        id: 17
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":" today"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.891Z"}

                        id: 18
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"content":"?"}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.910Z"}

                        id: 19
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":"stop","delta":{"content":""}}],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.928Z"}

                        id: 20
                        event: message
                        data: {"id":"chatcmpl-1677d3cfb8fb41ffa1de5bdb9ea32a3c","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[],"created":1764692529,"model_version":"4.0.0","created_at":"2025-12-02T16:22:09.928Z","usage":{"completion_tokens":18,"prompt_tokens":190,"total_tokens":208}}
                        """)));

        var httpPort = wireMock.getPort();
        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var delpoymentService = DeploymentService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .toolInterceptor((ctx, fc) -> fc)
            .messageInterceptor((ctx, message) -> "I don't feel good.")
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        List<String> partialResponses = new ArrayList<>();
        ChatRequest chatRequest = ChatRequest.builder()
            .deploymentId("my-deployment-id")
            .messages(UserMessage.text("How are you?"))
            .build();
        delpoymentService.chatStreaming(chatRequest, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                partialResponses.add(partialResponse);
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

        var chatResponse = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        var assistantMessage = chatResponse.toAssistantMessage();
        assertEquals("Hello! I'm doing well, thank you. How can I assist you today?", assistantMessage.content());
        assertFalse(assistantMessage.hasToolCalls());
        assertEquals("Hello! I'm doing well, thank you. How can I assist you today?", partialResponses.stream().collect(Collectors.joining()));
    }

    @Test
    void should_map_function_call_correctly_in_streaming() {

        String REQUEST = """
            {
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "What time is it in Italy?"
                            }
                        ]
                    }
                ],
                "tools": [
                    {
                        "type": "function",
                        "function": {
                            "name": "get_current_time",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "country": {
                                        "type": "string"
                                    }
                                }
                            }
                        }
                    }
                ],
            "time_limit": 60000
            }""";

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withRequestBody(equalToJson(REQUEST))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(4, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-xyz","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}]}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-xyz","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"id":"chatcmpl-tool-1","type":"function","function":{"name":"get_current_time","arguments":""}}]}}]}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-xyz","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":"tool_calls","delta":{"tool_calls":[{"index":0,"function":{"name":"","arguments":"\\"{\\\\n  \\\\\\"country\\\\\\": \\\\\\"Italy\\\\\\"\\\\n}\\""}}]}}]}

                        id: 4
                        event: close
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.565Z","usage":{"completion_tokens":49,"prompt_tokens":319,"total_tokens":368}}
                        """)));

        var httpPort = wireMock.getPort();
        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var deploymentService = DeploymentService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .toolInterceptor((ctx, fc) -> fc.withName("new_name").withArguments(Json.fromJson(fc.arguments(), String.class)))
            .build();


        var tools = Tool.of("get_current_time", JsonSchema.object().property("country", JsonSchema.string()));
        var messages = List.<ChatMessage>of(UserMessage.text("What time is it in Italy?"));

        List<PartialToolCall> toolFetchers = new ArrayList<>();
        List<CompletedToolCall> toolCalls = new ArrayList<>();
        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        ChatRequest chatRequest = ChatRequest.builder()
            .deploymentId("my-deployment-id")
            .messages(messages)
            .tools(tools)
            .build();

        deploymentService.chatStreaming(chatRequest, new ChatHandler() {

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
                toolFetchers.add(partialToolCall);
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                toolCalls.add(completeToolCall);
            }
        });

        ChatResponse response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        AssistantMessage assistantMessage = response.toAssistantMessage();
        JSONAssert.assertEquals("""
            {
                "index": 0,
                "id": "chatcmpl-tool-1",
                "type": "function",
                "function": {
                    "name": "new_name",
                    "arguments" : "{\\n  \\"country\\": \\"Italy\\"\\n}"
                }
            }""",
            Json.toJson(assistantMessage.toolCalls().get(0)),
            true
        );
        assistantMessage.processTools((toolName, toolArgs) -> {
            assertEquals("new_name", toolName);
            JSONAssert.assertEquals("{ \"country\": \"Italy\" }", Json.toJson(toolArgs), true);
            return false;
        });

        assertEquals(1, toolCalls.size());
        JSONAssert.assertEquals("""
            {
                "index": 0,
                "id": "chatcmpl-tool-1",
                "type": "function",
                "function": {
                    "name": "new_name",
                    "arguments" : "{\\n  \\"country\\": \\"Italy\\"\\n}"
                }
            }""",
            Json.toJson(toolCalls.get(0).toolCall()),
            true
        );

        assertEquals(
            "\"{\\n  \\\"country\\\": \\\"Italy\\\"\\n}\"",
            toolFetchers.stream().map(PartialToolCall::arguments).collect(Collectors.joining()));
    }

    @Test
    void should_use_default_chat_parameters() throws Exception {

        withWatsonxServiceMock(() -> {

            var tool = Tool.of("get_current_time", "Get the current time");
            var deploymentService = DeploymentService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(CloudRegion.FRANKFURT)
                .tools(tool)
                .parameters(
                    ChatParameters.builder()
                        .context("context")
                        .frequencyPenalty(2.0)
                        .guidedChoice(Set.of("1"))
                        .guidedGrammar("guidedGrammar")
                        .guidedRegex("guidedRegex")
                        .lengthPenalty(1.0)
                        .logitBias(Map.of("test", 1))
                        .logprobs(true)
                        .maxCompletionTokens(0)
                        .n(2)
                        .presencePenalty(3.0)
                        .repetitionPenalty(4.0)
                        .responseAsJson()
                        .seed(2)
                        .stop(List.of("stop"))
                        .temperature(1.0)
                        .timeLimit(Duration.ofMinutes(120))
                        .toolChoice("toolChoice")
                        .toolChoiceOption(ToolChoiceOption.REQUIRED)
                        .topLogprobs(3)
                        .topP(6.0)
                        .build()
                ).build();

            var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "id": "chatcmpl-43962cc06e5346ccbd653a04a48e4b5b",
                        "object" : "chat.completion",
                        "model_id" : "my-super-model",
                        "model" : "my-super-model-model",
                        "choices" : [ {
                        "index" : 0,
                        "message" : {
                            "role" : "assistant",
                            "content" : "Hello!!!"
                        },
                        "finish_reason" : "stop"
                        }],
                        "created" : 1749288614,
                        "model_version" : "1.1.0",
                        "created_at" : "2025-06-07T09:30:15.122Z",
                        "usage" : {
                        "completion_tokens" : 37,
                        "prompt_tokens" : 66,
                        "total_tokens" : 103
                        }
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatRequest = ChatRequest.builder()
                .deploymentId("deploymentId")
                .messages(messages)
                .build();

            deploymentService.chat(chatRequest);
            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals("https://eu-de.ml.cloud.ibm.com/ml/v1/deployments/deploymentId/text/chat?version=%s".formatted(API_VERSION),
                actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody = Json.toJson(
                TextChatRequest.builder()
                    .messages(messages)
                    .context("context")
                    .frequencyPenalty(2.0)
                    .guidedChoice(Set.of("1"))
                    .guidedGrammar("guidedGrammar")
                    .guidedRegex("guidedRegex")
                    .lengthPenalty(1.0)
                    .logitBias(Map.of("test", 1))
                    .logprobs(true)
                    .maxCompletionTokens(0)
                    .n(2)
                    .presencePenalty(3.0)
                    .repetitionPenalty(4.0)
                    .responseFormat("json_object")
                    .seed(2)
                    .stop(List.of("stop"))
                    .temperature(1.0)
                    .toolChoice(Map.of("type", "function", "function", Map.of("name", "toolChoice")))
                    .toolChoiceOption("required")
                    .topLogprobs(3)
                    .topP(6.0)
                    .timeLimit(7200000L)
                    .tools(List.of(tool))
                    .build());

            assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void should_override_default_chat_parameters() throws Exception {

        withWatsonxServiceMock(() -> {

            var defaultTool = Tool.of("get_current_time", "Get the current time");
            var ovverideTool = Tool.of("get_current_time_ovveride", "Get the current time override");

            var deploymentService = DeploymentService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(CloudRegion.FRANKFURT)
                .tools(defaultTool)
                .parameters(
                    ChatParameters.builder()
                        .context("context")
                        .frequencyPenalty(2.0)
                        .guidedChoice(Set.of("1"))
                        .guidedGrammar("guidedGrammar")
                        .guidedRegex("guidedRegex")
                        .lengthPenalty(1.0)
                        .logitBias(Map.of("test", 1))
                        .logprobs(true)
                        .maxCompletionTokens(0)
                        .n(2)
                        .presencePenalty(3.0)
                        .repetitionPenalty(4.0)
                        .responseAsJson()
                        .seed(2)
                        .stop(List.of("stop"))
                        .temperature(1.0)
                        .timeLimit(Duration.ofMinutes(120))
                        .toolChoice("toolChoice")
                        .toolChoiceOption(ToolChoiceOption.REQUIRED)
                        .topLogprobs(3)
                        .topP(6.0)
                        .build()
                ).build();

            var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "id": "chatcmpl-43962cc06e5346ccbd653a04a48e4b5b",
                        "object" : "chat.completion",
                        "model_id" : "my-super-model",
                        "model" : "my-super-model-model",
                        "choices" : [ {
                        "index" : 0,
                        "message" : {
                            "role" : "assistant",
                            "content" : "Hello!!!"
                        },
                        "finish_reason" : "stop"
                        }],
                        "created" : 1749288614,
                        "model_version" : "1.1.0",
                        "created_at" : "2025-06-07T09:30:15.122Z",
                        "usage" : {
                        "completion_tokens" : 37,
                        "prompt_tokens" : 66,
                        "total_tokens" : 103
                        }
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatRequest = ChatRequest.builder()
                .deploymentId("deploymentId")
                .messages(messages)
                .tools(ovverideTool)
                .parameters(
                    ChatParameters.builder()
                        .context("context_override")
                        .frequencyPenalty(0.0)
                        .guidedChoice(Set.of("1_override"))
                        .guidedGrammar("guidedGrammar_override")
                        .guidedRegex("guidedRegex_override")
                        .lengthPenalty(0.0)
                        .logitBias(Map.of("test", 0))
                        .logprobs(false)
                        .maxCompletionTokens(100)
                        .n(0)
                        .presencePenalty(0.0)
                        .repetitionPenalty(0.0)
                        .responseAsText()
                        .seed(0)
                        .stop(List.of("stop_override"))
                        .temperature(0.0)
                        .timeLimit(Duration.ofSeconds(1))
                        .toolChoice("toolChoice_override")
                        .toolChoiceOption(ToolChoiceOption.NONE)
                        .topLogprobs(0)
                        .topP(0.0)
                        .build()
                ).build();

            deploymentService.chat(chatRequest);
            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals("https://eu-de.ml.cloud.ibm.com/ml/v1/deployments/deploymentId/text/chat?version=%s".formatted(API_VERSION),
                actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody = Json.toJson(
                TextChatRequest.builder()
                    .messages(messages)
                    .context("context_override")
                    .frequencyPenalty(0.0)
                    .guidedChoice(Set.of("1_override"))
                    .guidedGrammar("guidedGrammar_override")
                    .guidedRegex("guidedRegex_override")
                    .lengthPenalty(0.0)
                    .logitBias(Map.of("test", 0))
                    .logprobs(false)
                    .maxCompletionTokens(100)
                    .n(0)
                    .presencePenalty(0.0)
                    .repetitionPenalty(0.0)
                    .responseFormat("text")
                    .seed(0)
                    .stop(List.of("stop_override"))
                    .temperature(0.0)
                    .toolChoice(Map.of("type", "function", "function", Map.of("name", "toolChoice_override")))
                    .toolChoiceOption("none")
                    .topLogprobs(0)
                    .topP(0.0)
                    .timeLimit(1000L)
                    .tools(List.of(ovverideTool))
                    .build());

            assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest));
        });
    }
}
