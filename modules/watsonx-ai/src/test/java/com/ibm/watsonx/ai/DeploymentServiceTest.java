/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.WatsonxService.API_VERSION;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.utils.Utils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
public class DeploymentServiceTest {

    @Mock
    HttpClient mockHttpClient;

    @Mock
    AuthenticationProvider mockAuthenticationProvider;

    @Captor
    ArgumentCaptor<HttpRequest> httpRequest;

    @Mock
    HttpResponse<String> mockHttpResponse;

    @RegisterExtension
    WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    @BeforeEach
    void setup() {
        when(mockAuthenticationProvider.getToken()).thenReturn("token");
        when(mockAuthenticationProvider.getTokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
    }

    @Test
    void test_generate() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .deployment("my-deployment-id")
            .build();

        TextGenerationParameters parameters = TextGenerationParameters.builder()
            .modelId("model-id")
            .projectId("project-id")
            .spaceId("space-id")
            .build();

        String input = "how far is paris from bangalore:";
        TextGenerationRequest EXPECTED_BODY =
            new TextGenerationRequest(null, null, null, input, parameters, null);

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
        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var response = deploymentService.generate(input, parameters);
        assertEquals("google/flan-ul2", response.modelId());
        assertEquals("2023-07-21T16:52:32.190Z", response.createdAt());
        assertEquals("4,000 km", response.results().get(0).generatedText());
        assertEquals(4, response.results().get(0).generatedTokenCount());
        assertEquals(12, response.results().get(0).inputTokenCount());
        assertEquals("eos_token", response.results().get(0).stopReason());

        JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(httpRequest), true);
        assertEquals(
            URI.create(CloudRegion.DALLAS.getMlEndpoint()
                .concat("/ml/v1/deployments/my-deployment-id/text/generation?version=%s".formatted(API_VERSION))),
            httpRequest.getValue().uri()
        );
    }

    @Test
    void test_generate_prompt_template() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .deployment("my-deployment-id")
            .build();

        TextGenerationParameters parameters = TextGenerationParameters.builder()
            .promptVariables(Map.of("city", "paris"))
            .build();

        TextGenerationRequest EXPECTED_BODY =
            new TextGenerationRequest(null, null, null, null, parameters, null);

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
        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var response = deploymentService.generate(null, parameters);
        assertEquals("google/flan-ul2", response.modelId());
        assertEquals("2023-07-21T16:52:32.190Z", response.createdAt());
        assertEquals("4,000 km", response.results().get(0).generatedText());
        assertEquals(4, response.results().get(0).generatedTokenCount());
        assertEquals(12, response.results().get(0).inputTokenCount());
        assertEquals("eos_token", response.results().get(0).stopReason());

        JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(httpRequest), true);
        assertEquals(
            URI.create(CloudRegion.DALLAS.getMlEndpoint()
                .concat("/ml/v1/deployments/my-deployment-id/text/generation?version=%s".formatted(API_VERSION))),
            httpRequest.getValue().uri()
        );
    }

    @Test
    void test_generate_streaming() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
            .build();

        TextGenerationParameters parameters = TextGenerationParameters.builder()
            .modelId("model-id")
            .projectId("project-id")
            .spaceId("space-id")
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/generation_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withRequestBody(equalToJson(
                """
                      {
                      "input": "how far is paris from bangalore:",
                      "parameters": {
                            "project_id" : "project-id",
                            "space_id" : "space-id",
                            "model_id" : "model-id",
                            "time_limit": 10000
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


        CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
        deploymentService.generateStreaming("how far is paris from bangalore:", parameters,
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
    void test_generate_streaming_prompt_template() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
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
                            },
                            "time_limit": 10000
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


        CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
        deploymentService.generateStreaming("how far is paris from bangalore:", parameters,
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
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
            .logResponses(true)
            .build();


        deploymentService.generateStreaming("how far is paris from bangalore:", parameters,
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
    void test_chat() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .deployment("my-deployment-id")
            .build();

        ChatParameters parameters = ChatParameters.builder()
            .modelId("model-id")
            .projectId("project-id")
            .spaceId("space-id")
            .build();

        ChatRequest EXPECTED_BODY = ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hello")))
            .parameters(parameters)
            .timeLimit(10000l)
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
        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var response = deploymentService.chat(List.of(UserMessage.text("Hello")), parameters);
        assertEquals("cmpl-15475d0dea9b4429a55843c77997f8a9", response.getId());
        assertEquals("ibm/granite-3-2b-instruct", response.getModelId());
        assertEquals("2023-07-21T16:52:32.190Z", response.getCreatedAt());
        assertEquals(1689958352, response.getCreated());
        assertEquals(27, response.getUsage().getCompletionTokens());
        assertEquals(186, response.getUsage().getPromptTokens());
        assertEquals(213, response.getUsage().getTotalTokens());
        assertEquals(1, response.getChoices().size());
        assertEquals("The 2020 World Series was played at the Globe Life Field in Arlington, Texas.\n",
            response.getChoices().get(0).message().content());

        JSONAssert.assertEquals(toJson(EXPECTED_BODY), bodyPublisherToString(httpRequest), true);
        assertEquals(
            URI.create(CloudRegion.DALLAS.getMlEndpoint()
                .concat("/ml/v1/deployments/my-deployment-id/text/chat?version=%s".formatted(API_VERSION))),
            httpRequest.getValue().uri()
        );

        deploymentService.chat(List.of(UserMessage.text("Hello")));
        response = deploymentService.chat(List.of(UserMessage.text("Hello")), parameters);
        assertEquals("cmpl-15475d0dea9b4429a55843c77997f8a9", response.getId());
    }

    @Test
    void test_chat_streaming() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
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
                    "time_limit": 10000,
                    "temperature": 0,
                    "context": "test"
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
        };

        deploymentService.chatStreaming(messages, chatParameters, chatHandler);
        ChatResponse response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        assertNotNull(response);
        assertNotNull(response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals("stop", response.getChoices().get(0).finishReason());
        assertEquals(0, response.getChoices().get(0).index());
        assertEquals("Ciao", response.getChoices().get(0).message().content());
        assertEquals("Ciao", response.toText());
        assertNotNull(response.getCreated());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getId());
        assertNotNull(response.getModel());
        assertNotNull(response.getModelId());
        assertNotNull(response.getModelVersion());
        assertNotNull(response.getObject());
        assertNotNull(response.getUsage());
        assertNotNull(response.getUsage().getCompletionTokens());
        assertNotNull(response.getUsage().getPromptTokens());
        assertNotNull(response.getUsage().getTotalTokens());

        deploymentService = DeploymentService.builder()
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
            .logResponses(true)
            .build();

        chatParameters = ChatParameters.builder()
            .modelId("model-id")
            .spaceId("space-id")
            .projectId("project-id")
            .maxCompletionTokens(0)
            .temperature(0.0)
            .build();

        deploymentService.chatStreaming(messages, chatParameters, chatHandler);
        response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        assertNotNull(response);
        Thread.sleep(50);
    }

    @Test
    void test_forecast() throws Exception {

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
            .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .deployment("my-deployment-id")
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
            .futureData(
                ForecastData.create()
                    .add("date", "2021-01-01T00:00:00")
                    .add("ID1", "D1")
                    .add("TARGET1", 5)
            )
            .build();

        wireMock.stubFor(post("/ml/v1/deployments/my-deployment-id/time_series/forecast?version=2025-04-23")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
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

        var result = deploymentService.forecast(inputSchema, data, parameters);
        JSONAssert.assertEquals(EXPECTED, toJson(result), true);
    }

    @Test
    void test_exception() throws Exception {

        when(mockHttpClient.send(any(), any()))
            .thenThrow(new IOException("IOException"))
            .thenThrow(new InterruptedException("InterruptedException"));

        DeploymentService deploymentService = DeploymentService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .deployment("my-deployment-id")
            .build();

        var ex = assertThrows(RuntimeException.class, () -> deploymentService.generate("test"));
        assertEquals(ex.getCause().getMessage(), "IOException");
        ex = assertThrows(RuntimeException.class, () -> deploymentService.chat(UserMessage.text("test")));
        assertEquals(ex.getCause().getMessage(), "InterruptedException");
        ex = assertThrows(RuntimeException.class,
            () -> deploymentService.forecast(InputSchema.builder().timestampColumn("test").build(), ForecastData.create()));
        assertEquals(ex.getCause().getMessage(), "InterruptedException");
    }
}
