/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.Image;
import com.ibm.watsonx.ai.chat.model.Image.Detail;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.JsonSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.EnumSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.IntegerSchema;
import com.ibm.watsonx.ai.chat.model.JsonSchema.StringSchema;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.Thinking;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.chat.model.UserContent;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.VideoContent;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class ChatServiceTest extends AbstractWatsonxTest {

    @Test
    void try_all_chat_parameters() {

        withWatsonxServiceMock(() -> {

            var chatParameters = ChatParameters.builder()
                .frequencyPenalty(2.0)
                .logitBias(Map.of("test", -10))
                .logprobs(true)
                .maxCompletionTokens(0)
                .modelId("my-super-model")
                .n(10)
                .presencePenalty(1.0)
                .projectId("project-id")
                .withJsonResponse()
                .seed(10)
                .spaceId("space-id")
                .stop(List.of("stop"))
                .temperature(1.0)
                .timeLimit(Duration.ofSeconds(60))
                .toolChoice("my-tool")
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .topLogprobs(10)
                .topP(1.2)
                .withJsonSchemaResponse("test", Map.of(), false)
                .build();

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .logRequests(true)
                .logResponses(true)
                // These values will be overridden by the chat parameters
                .modelId("my-default-model")
                .projectId("default-project-id")
                .spaceId("default-space-id")
                // ------------------------------------------------------
                .timeout(Duration.ofSeconds(60))
                .baseUrl(URI.create("http://my-cloud-instance.com"))
                .version(("1988-03-23"))
                .build();

            var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

            when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of(
                    "Content-Type", List.of("application/json"),
                    "Accept", List.of("application/json")),
                (k, v) -> true));
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

            var chatResponse = chatService.chat(messages, chatParameters);
            assertEquals("chatcmpl-43962cc06e5346ccbd653a04a48e4b5b", chatResponse.getId());
            assertEquals("my-super-model", chatResponse.getModelId());
            assertEquals("my-super-model-model", chatResponse.getModel());
            assertNotNull(chatResponse.getChoices());
            assertEquals(1, chatResponse.getChoices().size());
            assertEquals(0, chatResponse.getChoices().get(0).getIndex());
            assertEquals(0, chatResponse.getChoices().get(0).getIndex());
            assertEquals("assistant", chatResponse.getChoices().get(0).getMessage().role());
            assertEquals("Hello!!!", chatResponse.getChoices().get(0).getMessage().content());
            assertEquals("stop", chatResponse.getChoices().get(0).getFinishReason());
            assertEquals(1749288614, chatResponse.getCreated());
            assertEquals("2025-06-07T09:30:15.122Z", chatResponse.getCreatedAt());
            assertNotNull(chatResponse.getUsage());
            assertEquals(37, chatResponse.getUsage().getCompletionTokens());
            assertEquals(66, chatResponse.getUsage().getPromptTokens());
            assertEquals(103, chatResponse.getUsage().getTotalTokens());

            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals("http://my-cloud-instance.com/ml/v1/text/chat?version=1988-03-23", actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody = Json.toJson(
                TextChatRequest.builder()
                    .modelId("my-super-model")
                    .projectId("project-id")
                    .spaceId("space-id")
                    .messages(messages)
                    .parameters(chatParameters)
                    .timeLimit(chatParameters.getTimeLimit())
                    .build());

            assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void try_default_chat_parameters() throws Exception {

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("my-default-model")
                .projectId("default-project-id")
                .spaceId("default-space-id")
                .baseUrl(CloudRegion.FRANKFURT)
                .build();

            var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

            when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
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

            chatService.chat(messages);
            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals("https://eu-de.ml.cloud.ibm.com/ml/v1/text/chat?version=%s".formatted(API_VERSION),
                actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody = Json.toJson(
                TextChatRequest.builder()
                    .modelId("my-default-model")
                    .projectId("default-project-id")
                    .spaceId("default-space-id")
                    .messages(messages)
                    .timeLimit(60000L)
                    .build());

            assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void text_chat() throws Exception {

        final String REQUEST = """
            {
              "model_id": "meta-llama/llama-3-8b-instruct",
              "space_id": "space_id",
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant."
                },
                {
                  "role": "user",
                  "content": [{
                    "type": "text",
                    "text": "Who won the world series in 2020?"
                  }]
                },
                {
                  "role": "assistant",
                  "content": "The Los Angeles Dodgers won the World Series in 2020."
                },
                {
                  "role": "user",
                  "content": [{
                    "type": "text",
                    "text": "Where was it played?"
                  }]
                }
              ],
              "max_completion_tokens": 100,
              "temperature": 0,
              "time_limit": 1000
            }""";

        final String RESPONSE =
            """
                {
                  "id": "cmpl-15475d0dea9b4429a55843c77997f8a9",
                  "model_id": "meta-llama/llama-3-8b-instruct",
                  "created": 1689958352,
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "The 2020 World Series was played at Globe Life Field in Arlington, Texas,\\nwhich is the home stadium of the Texas Rangers.\\nHowever, the series was played with no fans in attendance due to the COVID-19 pandemic.\\n"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "completion_tokens": 47,
                    "prompt_tokens": 59,
                    "total_tokens": 106
                  }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.LONDON)
                .build();

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant."),
                UserMessage.text("Who won the world series in 2020?"),
                AssistantMessage.text("The Los Angeles Dodgers won the World Series in 2020."),
                UserMessage.text("Where was it played?"));

            var parameters = ChatParameters.builder()
                .timeLimit(Duration.ofMillis(1000))
                .maxCompletionTokens(100)
                .temperature(0.0)
                .spaceId("space_id")
                .build();

            var chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void tool_call() throws Exception {

        final String REQUEST = """
            {
              "model_id": "meta-llama/llama-3-8b-instruct",
              "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages": [
                {
                  "role": "user",
                  "content": [{
                    "type": "text",
                    "text": "What is the weather like in Boston today?"
                  }]
                }
              ],
              "tools": [
                {
                  "type": "function",
                  "function": {
                    "name": "get_current_weather",
                    "parameters": {
                      "type": "object",
                      "properties": {
                        "location": {
                          "description": "The city, e.g. San Francisco, CA",
                          "type": "string"
                        },
                        "unit": {
                          "enum": [
                            "celsius",
                            "fahrenheit"
                          ]
                        }
                      },
                      "required": [
                        "location"
                      ]
                    }
                  }
                }
              ],
              "tool_choice": {
                "type": "function",
                "function": {
                  "name": "get_current_weather"
                }
              }
            }""";

        final String RESPONSE = """
              {
                "id": "cmpl-15475d0dea9b4429a55843c77997f8a9",
                "model_id": "meta-llama/llama-3-8b-instruct",
                "created": 1689958352,
                "created_at": "2023-07-21T16:52:32.190Z",
                "choices": [
                  {
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "tool_calls": [
                        {
                          "id": "chatcmpl-tool-ef093f0cbbff4c6a973aa0873f73fc99",
                          "type": "function",
                          "function": {
                            "name": "get_current_weather",
                            "arguments": "{\\n  \\"location\\": \\"Boston, MA\\",\\n  \\"unit\\": \\"fahrenheit\\"\\n}\\n"
                          }
                        }
                      ]
                    },
                    "finish_reason": "stop"
                  }
                ],
                "usage": {
                  "completion_tokens": 18,
                  "prompt_tokens": 19,
                  "total_tokens": 37
                }
            }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var parameters = ChatParameters.builder()
                .toolChoice("get_current_weather")
                .build();

            var chatResponse = chatService.chat(
                ChatRequest.builder()
                    .messages(UserMessage.text("What is the weather like in Boston today?"))
                    .tools(Tool.of(
                        "get_current_weather",
                        JsonSchema.builder()
                            .addProperty("location", StringSchema.of("The city, e.g. San Francisco, CA"))
                            .addProperty("unit", EnumSchema.of("celsius", "fahrenheit"))
                            .required("location")))
                    .parameters(parameters)
                    .build()
            );

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
            assertEquals("ChatUsage [completionTokens=18, promptTokens=19, totalTokens=37]",
                chatResponse.getUsage().toString());
        });
    }

    @Test
    void text_mode() throws Exception {

        final String REQUEST = """
            {
              "model_id": "meta-llama/llama-3-8b-instruct",
              "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "response_format": {
                "type": "text"
              },
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant designed to output JSON."
                },
                {
                  "role": "user",
                  "content": [{
                    "type": "text",
                    "text": "Who won the world series in 2020?"
                  }]
                }
              ]
            }""";

        final String RESPONSE =
            """
                {
                  "id": "cmpl-09945b25c805491fb49e15439b8e5d84",
                  "model_id": "meta-llama/llama-3-8b-instruct",
                  "created": 1689958352,
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "[\\"The Los Angeles Dodgers won the World Series in 2020. They defeated the Tampa Bay Rays in six games.\\"]"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "completion_tokens": 35,
                    "prompt_tokens": 20,
                    "total_tokens": 55
                  }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant designed to output JSON."),
                UserMessage.text("Who won the world series in 2020?"));

            var parameters = ChatParameters.builder().withTextResponse().build();
            var chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void json_mode() throws Exception {

        final String REQUEST = """
            {
              "model_id": "meta-llama/llama-3-8b-instruct",
              "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "response_format": {
                "type": "json_object"
              },
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant designed to output JSON."
                },
                {
                  "role": "user",
                  "content": [{
                    "type": "text",
                    "text": "Who won the world series in 2020?"
                  }]
                }
              ]
            }""";

        final String RESPONSE =
            """
                {
                  "id": "cmpl-09945b25c805491fb49e15439b8e5d84",
                  "model_id": "meta-llama/llama-3-8b-instruct",
                  "created": 1689958352,
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "[\\"The Los Angeles Dodgers won the World Series in 2020. They defeated the Tampa Bay Rays in six games.\\"]"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "completion_tokens": 35,
                    "prompt_tokens": 20,
                    "total_tokens": 55
                  }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant designed to output JSON."),
                UserMessage.text("Who won the world series in 2020?"));

            var parameters = ChatParameters.builder().withJsonResponse().build();
            var chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    record LLMResponse(String name, List<Province> provinces) {}
    record Province(String name, Population population) {}
    record Population(int value, String density) {}

    @Test
    void json_schema() throws Exception {

        final String REQUEST = """
              {
                "model_id": "meta-llama/llama-3-8b-instruct",
                "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                        "name": "test",
                        "schema": {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type": "string"
                                },
                                "provinces": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "properties": {
                                            "name": {
                                                "type": "string"
                                            },
                                            "population": {
                                                "type": "object",
                                                "properties": {
                                                    "value": {
                                                        "type": "number"
                                                    },
                                                    "density": {
                                                        "enum": [
                                                            "LOW",
                                                            "MEDIUM",
                                                            "HIGH"
                                                        ]
                                                    }
                                                },
                                                "required": [
                                                    "value",
                                                    "density"
                                                ]
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "strict": true
                    }
                },
                "messages": [
                    {
                        "role": "system",
                        "content": "Given a region return the information"
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "Campania"
                            }
                        ]
                    }
                ]
            }""";

        final String RESPONSE =
            """
                {
                  "id": "cmpl-09945b25c805491fb49e15439b8e5d84",
                  "model_id": "meta-llama/llama-3-8b-instruct",
                  "created": 1689958352,
                  "created_at": "2023-07-21T16:52:32.190Z",
                  "choices": [
                      {
                          "index": 0,
                          "message": {
                              "role": "assistant",
                              "content": "{\\"name\\": \\"Campania\\", \\n\\"provinces\\": [\\n{\\n\\"name\\": \\"Caserta\\",\\n\\"population\\":  \\t{\\n\\"density\\":  \\t    \\t\\"LOW\\",\\n\\"value\\": 924414\\n}\\n},\\n{\\n\\"name\\": \\"Benevento\\",\\n\\"population\\":  {\\n\\"density\\":  \\"LOW\\",\\n\\"value\\": 283393\\n}\\n},\\n{\\n\\"name\\": \\"Napoli\\",\\n\\"population\\":  {\\n\\"density\\":  \\"HIGH\\",\\n\\"value\\": 3116402\\n}\\n},\\n{\\n\\"name\\": \\"Avellino\\",\\n\\"population\\":  {\\n\\"density\\":  \\"LOW\\",\\n\\"value\\": 423536\\n}\\n},\\n{\\n\\"name\\": \\"Salerno\\",\\n\\"population\\":  {\\n\\"density\\":  \\"MEDIUM\\",\\n\\"value\\": 1108369\\n}\\n}\\n]\\n}"
                          },
                          "finish_reason": "stop"
                      }
                  ],
                  "usage": {
                    "completion_tokens": 35,
                    "prompt_tokens": 20,
                    "total_tokens": 55
                  }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .logRequests(true)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("Given a region return the information"),
                UserMessage.text("Campania"));

            var jsonSchema = JsonSchema.builder()
                .addStringProperty("name")
                .addArrayProperty("provinces",
                    JsonSchema.builder()
                        .addStringProperty("name")
                        .addObjectProperty("population",
                            JsonSchema.builder()
                                .addNumberProperty("value")
                                .addEnumProperty("density", "LOW", "MEDIUM", "HIGH")
                                .required("value", "density")
                                .build()
                        )
                ).build();

            var parameters = ChatParameters.builder()
                .withJsonSchemaResponse("test", jsonSchema, true)
                .build();

            var chatResponse = chatService.chat(messages, parameters);

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), true);

            var response = chatResponse.toObject(LLMResponse.class);
            assertEquals("Campania", response.name());
            assertEquals(5, response.provinces.size());
            assertEquals(FinishReason.STOP, chatResponse.finishReason());
            assertEquals(new Province("Caserta", new Population(924414, "LOW")), response.provinces.get(0));
            assertEquals(new Province("Benevento", new Population(283393, "LOW")), response.provinces.get(1));
            assertEquals(new Province("Napoli", new Population(3116402, "HIGH")), response.provinces.get(2));
            assertEquals(new Province("Avellino", new Population(423536, "LOW")), response.provinces.get(3));
            assertEquals(new Province("Salerno", new Population(1108369, "MEDIUM")), response.provinces.get(4));

            parameters = ChatParameters.builder()
                .withJsonSchemaResponse(jsonSchema)
                .build();

            chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), true);
        });
    }

    @Test
    void test_tool_calls() throws Exception {

        final String REQUEST = """
            {
              "model_id" : "ibm/granite-3-8b-instruct",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "2 + 2"
                } ]
              } ],
              "tools" : [ {
                "type" : "function",
                "function" : {
                  "name" : "sum",
                  "description" : "Execute the sum of two numbers",
                  "parameters" : {
                    "type" : "object",
                    "properties" : {
                      "second_number" : {
                        "type" : "integer"
                      },
                      "first_number" : {
                        "type" : "integer"
                      }
                    },
                    "required" : [ "first_number", "second_number" ]
                  }
                }
              } ]
            }""";

        final String RESPONSE = """
            {
              "id" : "chatcmpl-344f198bce61aabb4bf2c318d05b3e80",
              "object" : "chat.completion",
              "model_id" : "ibm/granite-3-8b-instruct",
              "model" : "ibm/granite-3-8b-instruct",
              "choices" : [ {
                "index" : 0,
                "message" : {
                  "role" : "assistant",
                  "tool_calls" : [ {
                    "id" : "chatcmpl-tool-95b123bc5f214e7ebaf5fc7e111410a4",
                    "type" : "function",
                    "function" : {
                      "name" : "sum",
                      "arguments" : "{\\"first_number\\": 2, \\"second_number\\": 2}"
                    }
                  } ]
                },
                "finish_reason" : "tool_calls"
              } ],
              "created" : 1749320949,
              "model_version" : "1.1.0",
              "created_at" : "2025-06-07T18:29:09.521Z",
              "usage" : {
                "completion_tokens" : 31,
                "prompt_tokens" : 239,
                "total_tokens" : 270
              }
            }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(UserMessage.text("2 + 2"));
            var tools = Tool.of(
                "sum",
                "Execute the sum of two numbers",
                JsonSchema.builder()
                    .addProperty("first_number", IntegerSchema.of())
                    .addProperty("second_number", IntegerSchema.of())
                    .required(List.of("first_number", "second_number")));

            var chatResponse = chatService.chat(messages, List.of(tools));

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);

            var ex = assertThrows(RuntimeException.class, () -> chatResponse.extractContent());
            assertEquals("The response is of the type \"tool_calls\" and contains no text", ex.getMessage());
        });
    }

    @Test
    void test_tool_call_without_parameters() throws Exception {

        final String REQUEST = """
            {
              "model_id" : "ibm/granite-3-8b-instruct",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "What time is it?"
                } ]
              } ],
              "tools" : [ {
                "type" : "function",
                "function" : {
                  "name" : "get_time",
                  "description" : "Get the current time"
                }
              } ],
              "time_limit": 60000
            }""";

        final String RESPONSE = """
            {
              "id" : "chatcmpl-344f198bce61aabb4bf2c318d05b3e80",
              "object" : "chat.completion",
              "model_id" : "ibm/granite-3-8b-instruct",
              "model" : "ibm/granite-3-8b-instruct",
              "choices" : [ {
                "index" : 0,
                "message" : {
                  "role" : "assistant",
                  "tool_calls" : [ {
                    "id" : "chatcmpl-tool-95b123bc5f214e7ebaf5fc7e111410a4",
                    "type" : "function",
                    "function" : {
                      "name" : "get_time",
                      "arguments" : "{}"
                    }
                  } ]
                },
                "finish_reason" : "tool_calls"
              } ],
              "created" : 1749320949,
              "model_version" : "1.1.0",
              "created_at" : "2025-06-07T18:29:09.521Z",
              "usage" : {
                "completion_tokens" : 31,
                "prompt_tokens" : 239,
                "total_tokens" : 270
              }
            }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .logRequests(true)
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(UserMessage.text("What time is it?"));
            var tools = Tool.of("get_time", "Get the current time");

            var chatResponse = chatService.chat(messages, List.of(tools));

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), true);

            var ex = assertThrows(RuntimeException.class, () -> chatResponse.extractContent());
            assertEquals("The response is of the type \"tool_calls\" and contains no text", ex.getMessage());
        });
    }

    @Test
    void test_tool_calls_with_result() throws Exception {

        final String REQUEST = """
                 {
              "model_id" : "ibm/granite-3-8b-instruct",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "2 + 2"
                } ]
              }, {
                "role" : "assistant",
                "tool_calls" : [ {
                  "id" : "id",
                  "type" : "function",
                  "function" : {
                    "name" : "sum",
                    "arguments" : "{\\"first_number\\": 2, \\"second_number\\": 2}"
                  }
                } ]
              }, {
                "role" : "tool",
                "content" : "The result is 4",
                "tool_call_id" : "id"
              } ],
              "tools" : [ {
                "type" : "function",
                "function" : {
                  "name" : "sum",
                  "description" : "Execute the sum of two numbers",
                  "parameters" : {
                    "type" : "object",
                    "properties" : {
                      "first_number" : {
                        "type" : "integer"
                      },
                      "second_number" : {
                        "type" : "integer"
                      }
                    },
                    "required" : [ "first_number", "second_number" ]
                  }
                }
              } ]
            }""";

        final String RESPONSE =
            """
                {
                  "id" : "chatcmpl-d7f9fe711e8684a6bc58d2c4a88ff1bb",
                  "object" : "chat.completion",
                  "model_id" : "ibm/granite-3-8b-instruct",
                  "model" : "ibm/granite-3-8b-instruct",
                  "choices" : [ {
                    "index" : 0,
                    "message" : {
                      "role" : "assistant",
                      "content" : "The sum of 2 and 2 is 4."
                    },
                    "finish_reason" : "stop"
                  } ],
                  "created" : 1749322325,
                  "model_version" : "1.1.0",
                  "created_at" : "2025-06-07T18:52:05.718Z",
                  "usage" : {
                    "completion_tokens" : 13,
                    "prompt_tokens" : 254,
                    "total_tokens" : 267
                  }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.LONDON)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                UserMessage.text("2 + 2"),
                AssistantMessage.tools(ToolCall.of("id", "sum", "{\"first_number\": 2, \"second_number\": 2}")),
                ToolMessage.of("The result is 4", "id")
            );

            var tools = Tool.of(
                "sum",
                "Execute the sum of two numbers",
                JsonSchema.builder()
                    .addProperty("first_number", IntegerSchema.of())
                    .addProperty("second_number", IntegerSchema.of())
                    .required(List.of("first_number", "second_number"))
                    .build());

            var chatResponse = chatService.chat(messages, tools);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);

            tools = Tool.of(
                "sum",
                "Execute the sum of two numbers",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "first_number", Map.of(
                            "type", "integer"
                        ),
                        "second_number", Map.of(
                            "type", "integer"
                        )
                    ),
                    "required", List.of("first_number", "second_number")
                ));

            chatResponse = chatService.chat(messages, tools);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void test_text_content_user_message() throws Exception {

        final String REQUEST = """
            {
              "model_id" : "ibm/granite-3-8b-instruct",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "2 + 2"
                } ],
                "name" : "Alan"
              } ],
              "time_limit" : 60000
            }""";

        final String RESPONSE = """
                    {
              "id" : "chatcmpl-57aeefde11734f097c5b2ddefa88e10f",
              "object" : "chat.completion",
              "model_id" : "ibm/granite-3-8b-instruct",
              "model" : "ibm/granite-3-8b-instruct",
              "choices" : [ {
                "index" : 0,
                "message" : {
                  "role" : "assistant",
                  "content" : "2 + 2 equals 4."
                },
                "finish_reason" : "stop"
              } ],
              "created" : 1749327352,
              "model_version" : "1.1.0",
              "created_at" : "2025-06-07T20:15:52.598Z",
              "usage" : {
                "completion_tokens" : 9,
                "prompt_tokens" : 63,
                "total_tokens" : 72
              }
            }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.TOKYO)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(UserMessage.of("Alan", List.<UserContent>of(TextContent.of("2 + 2"))));
            var chatResponse = chatService.chat(messages);

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void test_image_content_user_message() throws Exception {

        var bytes = getClass().getClassLoader().getResourceAsStream("IBM.svg").readAllBytes();
        var file = new File(getClass().getClassLoader().getResource("IBM.svg").toURI());

        final String REQUEST = """
            {
              "model_id" : "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Tell me more about this image"
                }, {
                  "type" : "image_url",
                  "image_url" : {
                    "url" : "data:image/svg+xml;base64,%s",
                    "detail" : "auto"
                  }
                } ]
              } ],
              "time_limit" : 60000
            }""".formatted(Base64.getEncoder().encodeToString(bytes));

        final String RESPONSE =
            """
                  {
                      "id": "chatcmpl-016961bf40e3faa36420cf41ee60761d",
                      "object": "chat.completion",
                      "model_id": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                      "model": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                      "choices": [
                          {
                              "index": 0,
                              "message": {
                                  "role": "assistant",
                                  "content": "The image is the IBM logo"
                              },
                              "finish_reason": "stop"
                          }
                      ],
                      "created": 1749330987,
                      "model_version": "4.0.0",
                      "created_at": "2025-06-07T21:16:30.676Z",
                      "usage": {
                          "completion_tokens": 263,
                          "prompt_tokens": 2552,
                          "total_tokens": 2815
                      }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.TORONTO)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var message = UserMessage.of(
                TextContent.of("Tell me more about this image"),
                ImageContent.of("image/svg+xml", Base64.getEncoder().encodeToString(bytes))
            );

            var chatResponse = chatService.chat(message);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
            assertEquals(
                ImageContent.of("image/svg+xml", Base64.getEncoder().encodeToString(bytes)),
                assertDoesNotThrow(() -> ImageContent.from(file)));
        });
    }

    @Test
    void test_video_content_user_message() throws Exception {

        final String REQUEST = """
            {
              "model_id" : "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
              "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Tell me more about this video"
                }, {
                  "type" : "video_url",
                  "video_url" : {
                    "url" : "data:video/mp4;base64,ABC"
                  }
                } ]
              } ],
              "time_limit" : 60000
            }""";

        final String RESPONSE =
            """
                  {
                      "id": "chatcmpl-016961bf40e3faa36420cf41ee60761d",
                      "object": "chat.completion",
                      "model_id": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                      "model": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                      "choices": [
                          {
                              "index": 0,
                              "message": {
                                  "role": "assistant",
                                  "content": "Alan Wake videogame"
                              },
                              "finish_reason": "stop"
                          }
                      ],
                      "created": 1749330987,
                      "model_version": "4.0.0",
                      "created_at": "2025-06-07T21:16:30.676Z",
                      "usage": {
                          "completion_tokens": 263,
                          "prompt_tokens": 2552,
                          "total_tokens": 2815
                      }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var message = UserMessage.of(List.of(
                TextContent.of("Tell me more about this video"),
                VideoContent.of("video/mp4", "ABC")
            ));

            var chatResponse = chatService.chat(message);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void test_control_message() throws Exception {

        final String REQUEST = """
              {
                "model_id": "ibm/granite-3-3-8b-instruct",
                "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                "messages": [
                    {
                        "role": "control",
                        "content": "thinking"
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "What is the result of 1 + 1"
                            }
                        ]
                    }
                ]
            }""";

        final String RESPONSE =
            """
                {
                    "id": "chatcmpl-326dc89051c826dd8d3c690a2d716e77",
                    "object": "chat.completion",
                    "model_id": "ibm/granite-3-3-8b-instruct",
                    "model": "ibm/granite-3-3-8b-instruct",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "<think>Think</think><response>Result</response>"
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "created": 1749323488,
                    "model_version": "3.3.0",
                    "created_at": "2025-06-07T19:11:29.419Z",
                    "usage": {
                        "completion_tokens": 162,
                        "prompt_tokens": 198,
                        "total_tokens": 360
                    }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.SYDNEY)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                ControlMessage.of("thinking"),
                UserMessage.text("What is the result of 1 + 1")
            );

            var chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(
                    ChatParameters.builder()
                        .transactionId("my-transaction-id")
                        .build())
                .thinking(ExtractionTags.of("think", "response"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);

            var thinking = chatResponse.extractThinking();
            var response = chatResponse.extractContent();
            assertEquals("Think", thinking);
            assertEquals("Result", response);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");

            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Result", assistantMessage.content());
            assertEquals("Think", assistantMessage.thinking());
        });
    }

    @Test
    void test_control_message_with_single_tag() throws Exception {

        final String REQUEST = """
              {
                "model_id": "ibm/granite-3-3-8b-instruct",
                "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                "messages": [
                    {
                        "role": "control",
                        "content": "thinking"
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "What is the result of 1 + 1"
                            }
                        ]
                    }
                ]
            }""";

        final String RESPONSE =
            """
                {
                    "id": "chatcmpl-326dc89051c826dd8d3c690a2d716e77",
                    "object": "chat.completion",
                    "model_id": "ibm/granite-3-3-8b-instruct",
                    "model": "ibm/granite-3-3-8b-instruct",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "<think>Think</think>Result"
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "created": 1749323488,
                    "model_version": "3.3.0",
                    "created_at": "2025-06-07T19:11:29.419Z",
                    "usage": {
                        "completion_tokens": 162,
                        "prompt_tokens": 198,
                        "total_tokens": 360
                    }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.SYDNEY)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                ControlMessage.of("thinking"),
                UserMessage.text("What is the result of 1 + 1")
            );

            var chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(
                    ChatParameters.builder()
                        .transactionId("my-transaction-id")
                        .build())
                .thinking(ExtractionTags.of("think"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);

            var thinking = chatResponse.extractThinking();
            var response = chatResponse.extractContent();
            assertEquals("Think", thinking);
            assertEquals("Result", response);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }


    @Test
    void chat_streaming_test() throws Exception {

        final String VERSION = "2020-03-15";
        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson("""
                {
                    "model_id": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                    "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
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
                    "time_limit": 60000,
                    "temperature": 0
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(5, 200)
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


        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .version(VERSION)
            .build();

        var messages = List.<ChatMessage>of(
            SystemMessage.of("You are an expert translator and you give the translation of single words"),
            UserMessage.text("Translate \"Hello\" in Italian")
        );

        var chatParameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .temperature(0.0)
            .transactionId("my-transaction-id")
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        chatService.chatStreaming(messages, chatParameters, new ChatHandler() {

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
                fail("Unexpected partial tool call");
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                fail("Unexpected complete tool call");
            }
        });

        ChatResponse response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
        assertNotNull(response);
        assertNotNull(response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
        assertEquals(0, response.getChoices().get(0).getIndex());
        assertEquals("Ciao", response.getChoices().get(0).getMessage().content());
        assertEquals("Ciao", response.extractContent());
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
    }

    @Test
    void chat_streaming_tool_test() throws Exception {

        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withRequestBody(equalToJson("""
                {
                  "model_id" : "ibm/granite-3-3-8b-instruct",
                  "project_id" : "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                  "messages" : [ {
                    "role" : "user",
                    "content" : [ {
                      "type" : "text",
                      "text" : "Tell me the result of (2 + 2) - 2"
                    } ]
                  } ],
                  "tools" : [ {
                    "type" : "function",
                    "function" : {
                      "name" : "sum",
                      "description" : "execute the sum of two number",
                      "parameters" : {
                        "type" : "object",
                        "properties" : {
                          "firstNumber" : {
                            "type" : "integer"
                          },
                          "secondNumber" : {
                            "type" : "integer"
                          }
                        }
                      }
                    }
                  }, {
                    "type" : "function",
                    "function" : {
                      "name" : "subtraction",
                      "description" : "execute the subtraction of two number",
                      "parameters" : {
                        "type" : "object",
                        "properties" : {
                          "firstNumber" : {
                            "type" : "integer"
                          },
                          "secondNumber" : {
                            "type" : "integer"
                          }
                        }
                      }
                    }
                  } ],
                  "time_limit" : 60000
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(7, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.150Z","system":{"warnings":[{"message":"The value of 'max_completion_tokens' for this model was set to value 1024","id":"unspecified_max_completion_tokens","additional_properties":{"limit":0,"new_value":1024,"parameter":"max_completion_tokens","value":0}}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":null,"delta":{"content":""}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.151Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"id":"chatcmpl-tool-af37032523934f019aa7258469580a7a","type":"function","function":{"name":"sum","arguments":""}}]}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.348Z"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"function":{"name":"","arguments":"{\\"firstNumber\\": 2, \\"secondNumber\\": 2}"}}]}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.357Z"}

                        id: 5
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"id":"chatcmpl-tool-f762db03c60f441dba57bab09552bb7b","type":"function","function":{"name":"subtraction","arguments":""}}]}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.555Z"}

                        id: 6
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"finish_reason":"tool_calls","delta":{"tool_calls":[{"index":1,"function":{"name":"","arguments":"{\\"firstNumber\\": 2, \\"secondNumber\\": 2}"}}]}}],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.564Z"}

                        id: 7
                        event: message
                        data: {"id":"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602","object":"chat.completion.chunk","model_id":"ibm/granite-3-3-8b-instruct","model":"ibm/granite-3-3-8b-instruct","choices":[],"created":1749764735,"model_version":"3.3.0","created_at":"2025-06-12T21:45:35.565Z","usage":{"completion_tokens":49,"prompt_tokens":319,"total_tokens":368}}
                        """)));


        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("ibm/granite-3-3-8b-instruct")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .logResponses(true)
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .build();


        var messages = List.<ChatMessage>of(
            UserMessage.text("Tell me the result of (2 + 2) - 2")
        );

        var tools = List.of(
            Tool.of(
                "sum",
                "execute the sum of two number",
                JsonSchema.builder()
                    .addProperty("firstNumber", IntegerSchema.of())
                    .addProperty("secondNumber", IntegerSchema.of())
                    .build()
            ),
            Tool.of(
                "subtraction",
                "execute the subtraction of two number",
                JsonSchema.builder()
                    .addProperty("firstNumber", IntegerSchema.of())
                    .addProperty("secondNumber", IntegerSchema.of())
                    .build()
            )
        );

        List<PartialToolCall> toolFetchers = new ArrayList<>();
        List<CompletedToolCall> toolCalls = new ArrayList<>();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        chatService.chatStreaming(messages, tools, new ChatHandler() {

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
        assertNotNull(response);
        assertEquals(1749764735, response.getCreated());
        assertEquals("2025-06-12T21:45:35.150Z", response.getCreatedAt());
        assertEquals("chatcmpl-cc34b5ea3120fa9e07b18c5125d66602", response.getId());
        assertEquals("ibm/granite-3-3-8b-instruct", response.getModel());
        assertEquals("ibm/granite-3-3-8b-instruct", response.getModelId());
        assertEquals("3.3.0", response.getModelVersion());
        assertEquals("chat.completion.chunk", response.getObject());
        assertNotNull(response.getUsage());
        assertEquals(49, response.getUsage().getCompletionTokens());
        assertEquals(319, response.getUsage().getPromptTokens());
        assertEquals(368, response.getUsage().getTotalTokens());
        assertNotNull(response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals(0, response.getChoices().get(0).getIndex());
        assertEquals("tool_calls", response.getChoices().get(0).getFinishReason());
        assertNotNull(response.getChoices().get(0).getMessage());
        assertNull(response.getChoices().get(0).getMessage().content());
        assertNull(response.getChoices().get(0).getMessage().refusal());
        assertEquals("assistant", response.getChoices().get(0).getMessage().role());
        assertEquals(2, response.getChoices().get(0).getMessage().toolCalls().size());
        assertEquals("chatcmpl-tool-af37032523934f019aa7258469580a7a",
            response.getChoices().get(0).getMessage().toolCalls().get(0).id());
        assertEquals(0, response.getChoices().get(0).getMessage().toolCalls().get(0).index());
        assertEquals("function", response.getChoices().get(0).getMessage().toolCalls().get(0).type());
        assertEquals("sum", response.getChoices().get(0).getMessage().toolCalls().get(0).function().name());
        assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
            response.getChoices().get(0).getMessage().toolCalls().get(0).function().arguments());
        assertEquals("chatcmpl-tool-f762db03c60f441dba57bab09552bb7b",
            response.getChoices().get(0).getMessage().toolCalls().get(1).id());
        assertEquals(1, response.getChoices().get(0).getMessage().toolCalls().get(1).index());
        assertEquals("function", response.getChoices().get(0).getMessage().toolCalls().get(1).type());
        assertEquals("subtraction", response.getChoices().get(0).getMessage().toolCalls().get(1).function().name());
        assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
            response.getChoices().get(0).getMessage().toolCalls().get(1).function().arguments());


        assertEquals(2, toolFetchers.size());
        assertEquals(
            "{\"completion_id\":\"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602\",\"index\":0,\"id\":\"chatcmpl-tool-af37032523934f019aa7258469580a7a\",\"name\":\"sum\",\"arguments\":\"{\\\"firstNumber\\\": 2, \\\"secondNumber\\\": 2}\"}",
            toJson(toolFetchers.get(0)));
        assertEquals(
            "{\"completion_id\":\"chatcmpl-cc34b5ea3120fa9e07b18c5125d66602\",\"index\":1,\"id\":\"chatcmpl-tool-f762db03c60f441dba57bab09552bb7b\",\"name\":\"subtraction\",\"arguments\":\"{\\\"firstNumber\\\": 2, \\\"secondNumber\\\": 2}\"}",
            toJson(toolFetchers.get(1)));

        assertEquals(2, toolCalls.size());

        assertEquals("chatcmpl-cc34b5ea3120fa9e07b18c5125d66602", toolCalls.get(0).completionId());
        assertEquals(new ToolCall(
            0,
            "chatcmpl-tool-af37032523934f019aa7258469580a7a",
            "function",
            new FunctionCall("sum", "{\"firstNumber\": 2, \"secondNumber\": 2}")
        ), toolCalls.get(0).toolCall());

        assertEquals("chatcmpl-cc34b5ea3120fa9e07b18c5125d66602", toolCalls.get(1).completionId());
        assertEquals(new ToolCall(
            1,
            "chatcmpl-tool-f762db03c60f441dba57bab09552bb7b",
            "function",
            new FunctionCall("subtraction", "{\"firstNumber\": 2, \"secondNumber\": 2}")
        ), toolCalls.get(1).toolCall());
    }

    @Test
    void chat_streaming_tool_choice_required_test() throws Exception {

        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson("""
                  {
                    "model_id": "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
                    "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                    "messages": [
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Tell me the result of (2 + 2) - 2"
                                }
                            ]
                        }
                    ],
                    "tools": [
                        {
                            "type": "function",
                            "function": {
                                "name": "sum",
                                "description": "execute the sum of two number",
                                "parameters": {
                                    "type": "object",
                                    "properties": {
                                        "firstNumber": {
                                            "type": "integer"
                                        },
                                        "secondNumber": {
                                            "type": "integer"
                                        }
                                    }
                                }
                            }
                        },
                        {
                            "type": "function",
                            "function": {
                                "name": "subtraction",
                                "description": "execute the subtraction of two number",
                                "parameters": {
                                    "type": "object",
                                    "properties": {
                                        "firstNumber": {
                                            "type": "integer"
                                        },
                                        "secondNumber": {
                                            "type": "integer"
                                        }
                                    }
                                }
                            }
                        }
                    ],
                    "tool_choice_option": "required",
                    "time_limit": 60000
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(30, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.126Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"},{"message":"The value of 'max_completion_tokens' for this model was set to value 1024","id":"unspecified_max_completion_tokens","additional_properties":{"limit":0,"new_value":1024,"parameter":"max_completion_tokens","value":0}}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"sum","arguments":"{\\""}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.227Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"first"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.237Z"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"Number"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.249Z"}

                        id: 5
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"\\":"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.259Z"}

                        id: 6
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":" "}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.270Z"}

                        id: 7
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"2"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.282Z"}

                        id: 8
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":","}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.293Z"}

                        id: 9
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":" \\""}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.304Z"}

                        id: 10
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"second"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.315Z"}

                        id: 11
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"Number"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.326Z"}

                        id: 12
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"\\":"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.337Z"}

                        id: 13
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":" "}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.349Z"}

                        id: 14
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"2"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.360Z"}

                        id: 15
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":0,"type":"function","function":{"name":"","arguments":"}"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.371Z"}

                        id: 16
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"subtraction","arguments":"{\\""}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.494Z"}

                        id: 17
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"first"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.505Z"}

                        id: 18
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"Number"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.516Z"}

                        id: 19
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"\\":"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.527Z"}

                        id: 20
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":" "}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.538Z"}

                        id: 21
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"4"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.549Z"}

                        id: 22
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":","}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.560Z"}

                        id: 23
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":" \\""}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.572Z"}

                        id: 24
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"second"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.583Z"}

                        id: 25
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"Number"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.594Z"}

                        id: 26
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"\\":"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.605Z"}

                        id: 27
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":" "}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.616Z"}

                        id: 28
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"2"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.627Z"}

                        id: 29
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"tool_calls":[{"index":1,"type":"function","function":{"name":"","arguments":"}"}}]}}],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.638Z"}

                        id: 30
                        event: message
                        data: {"id":"chatcmpl-75021362a9edcdacca7976b97cc20f0d","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[],"created":1749766697,"model_version":"4.0.0","created_at":"2025-06-12T22:18:18.661Z","usage":{"completion_tokens":49,"prompt_tokens":374,"total_tokens":423}}
                        """)));

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .build();


        var messages = List.<ChatMessage>of(
            UserMessage.text("Tell me the result of (2 + 2) - 2")
        );

        var tools = List.of(
            Tool.of(
                "sum",
                "execute the sum of two number",
                JsonSchema.builder()
                    .addProperty("firstNumber", IntegerSchema.of())
                    .addProperty("secondNumber", IntegerSchema.of())
                    .build()
            ),
            Tool.of(
                "subtraction",
                "execute the subtraction of two number",
                JsonSchema.builder()
                    .addProperty("firstNumber", IntegerSchema.of())
                    .addProperty("secondNumber", IntegerSchema.of())
                    .build()
            )
        );

        List<PartialToolCall> toolFetchers = new ArrayList<>();
        List<CompletedToolCall> toolCalls = new ArrayList<>();

        var chatParameters = ChatParameters.builder()
            .transactionId("my-transaction-id")
            .toolChoiceOption(ToolChoiceOption.REQUIRED)
            .build();

        var chatRequest = ChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(chatParameters)
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        chatService.chatStreaming(chatRequest, new ChatHandler() {

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
        assertNotNull(response);
        assertEquals(1749766697, response.getCreated());
        assertEquals("2025-06-12T22:18:18.126Z", response.getCreatedAt());
        assertEquals("chatcmpl-75021362a9edcdacca7976b97cc20f0d", response.getId());
        assertEquals("meta-llama/llama-4-maverick-17b-128e-instruct-fp8", response.getModel());
        assertEquals("meta-llama/llama-4-maverick-17b-128e-instruct-fp8", response.getModelId());
        assertEquals("4.0.0", response.getModelVersion());
        assertEquals("chat.completion.chunk", response.getObject());
        assertNotNull(response.getUsage());
        assertEquals(49, response.getUsage().getCompletionTokens());
        assertEquals(374, response.getUsage().getPromptTokens());
        assertEquals(423, response.getUsage().getTotalTokens());
        assertNotNull(response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals(0, response.getChoices().get(0).getIndex());
        assertEquals("tool_calls", response.getChoices().get(0).getFinishReason());
        assertNotNull(response.getChoices().get(0).getMessage());
        assertNull(response.getChoices().get(0).getMessage().content());
        assertNull(response.getChoices().get(0).getMessage().refusal());
        assertEquals("assistant", response.getChoices().get(0).getMessage().role());
        assertEquals(2, response.getChoices().get(0).getMessage().toolCalls().size());
        // If tool_choice_option is "required" watsonx doesn't return the id, so it will be autogenerated.
        assertNotNull(response.getChoices().get(0).getMessage().toolCalls().get(0).id());
        assertEquals(0, response.getChoices().get(0).getMessage().toolCalls().get(0).index());
        assertEquals("function", response.getChoices().get(0).getMessage().toolCalls().get(0).type());
        assertEquals("sum", response.getChoices().get(0).getMessage().toolCalls().get(0).function().name());
        assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
            response.getChoices().get(0).getMessage().toolCalls().get(0).function().arguments());
        // If tool_choice_option is "required" watsonx doesn't return the id, so it will be autogenerated.
        assertNotNull(response.getChoices().get(0).getMessage().toolCalls().get(1).id());
        assertEquals(1, response.getChoices().get(0).getMessage().toolCalls().get(1).index());
        assertEquals("function", response.getChoices().get(0).getMessage().toolCalls().get(1).type());
        assertEquals("subtraction", response.getChoices().get(0).getMessage().toolCalls().get(1).function().name());
        assertEquals("{\"firstNumber\": 4, \"secondNumber\": 2}",
            response.getChoices().get(0).getMessage().toolCalls().get(1).function().arguments());

        assertEquals(28, toolFetchers.size());
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "{\"")),
            toJson(toolFetchers.get(0)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "first")),
            toJson(toolFetchers.get(1)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "Number")),
            toJson(toolFetchers.get(2)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "\":")),
            toJson(toolFetchers.get(3)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", " ")),
            toJson(toolFetchers.get(4)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "2")),
            toJson(toolFetchers.get(5)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", ",")),
            toJson(toolFetchers.get(6)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", " \"")),
            toJson(toolFetchers.get(7)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "second")),
            toJson(toolFetchers.get(8)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "Number")),
            toJson(toolFetchers.get(9)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "\":")),
            toJson(toolFetchers.get(10)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", " ")),
            toJson(toolFetchers.get(11)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "2")),
            toJson(toolFetchers.get(12)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 0, null, "sum", "}")),
            toJson(toolFetchers.get(13)),
            true
        );


        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "{\"")),
            toJson(toolFetchers.get(14)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "first")),
            toJson(toolFetchers.get(15)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "Number")),
            toJson(toolFetchers.get(16)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "\":")),
            toJson(toolFetchers.get(17)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", " ")),
            toJson(toolFetchers.get(18)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "4")),
            toJson(toolFetchers.get(19)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", ",")),
            toJson(toolFetchers.get(20)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", " \"")),
            toJson(toolFetchers.get(21)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "second")),
            toJson(toolFetchers.get(22)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "Number")),
            toJson(toolFetchers.get(23)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "\":")),
            toJson(toolFetchers.get(24)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", " ")),
            toJson(toolFetchers.get(25)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "2")),
            toJson(toolFetchers.get(26)),
            true
        );
        JSONAssert.assertEquals(
            toJson(new PartialToolCall("chatcmpl-75021362a9edcdacca7976b97cc20f0d", 1, null, "subtraction", "}")),
            toJson(toolFetchers.get(27)),
            true
        );

        assertEquals(2, toolCalls.size());
        assertEquals(0, toolCalls.get(0).toolCall().index());
        assertNotNull(toolCalls.get(0).toolCall().id());
        assertEquals("function", toolCalls.get(0).toolCall().type());
        assertEquals(new FunctionCall("sum", "{\"firstNumber\": 2, \"secondNumber\": 2}"), toolCalls.get(0).toolCall().function());

        assertEquals(1, toolCalls.get(1).toolCall().index());
        assertNotNull(toolCalls.get(1).toolCall().id());
        assertEquals("function", toolCalls.get(1).toolCall().type());
        assertEquals(new FunctionCall("subtraction", "{\"firstNumber\": 4, \"secondNumber\": 2}"), toolCalls.get(1).toolCall().function());
    }

    @Test
    void chat_streaming_on_error() throws Exception {

        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(4, 200)
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
                        data: {"id"}

                        id: 4
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"iao"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}
                        """)));

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .logResponses(true)
            .build();

        AtomicInteger counter = new AtomicInteger();
        CompletableFuture<ChatResponse> result = new CompletableFuture<>();

        var mockChatHandler = mock(ChatHandler.class);
        doAnswer(invocation -> {
            String partialResponse = invocation.getArgument(0);
            assertNotNull(partialResponse);
            counter.incrementAndGet();
            return null;
        }).when(mockChatHandler).onPartialResponse(anyString(), any());

        doAnswer(invocation -> {
            ChatResponse completeResponse = invocation.getArgument(0);
            result.complete(completeResponse);
            return null;
        }).when(mockChatHandler).onCompleteResponse(any());

        doAnswer(invocation -> {
            Throwable error = invocation.getArgument(0);
            assertInstanceOf(JsonParseException.class, error.getCause());
            counter.incrementAndGet();
            return null;
        }).when(mockChatHandler).onError(any());

        InOrder inOrder = inOrder(mockChatHandler);

        chatService.chatStreaming(List.of(UserMessage.text("Hello")), mockChatHandler);

        var response = result.get(3, TimeUnit.SECONDS);

        inOrder.verify(mockChatHandler).onPartialResponse(eq("C"), any());
        inOrder.verify(mockChatHandler).onError(any(RuntimeException.class));
        inOrder.verify(mockChatHandler).onPartialResponse(eq("iao"), any());
        inOrder.verify(mockChatHandler).onCompleteResponse(any());

        assertEquals(3, counter.get());
        assertEquals("Ciao", response.extractContent());

    }

    @Test
    void chat_streaming_on_complete_exception() throws Exception {

        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}
                        """)));

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .logResponses(true)
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        chatService.chatStreaming(List.of(UserMessage.text("Hello")), new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                throw new RuntimeException("Error in onComplete handler");
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
        });

        var ex = assertThrows(ExecutionException.class, () -> result.get(3, TimeUnit.SECONDS));
        assertEquals("Error in onComplete handler", ex.getCause().getMessage());
    }

    @Test
    void chat_tool_choice_option_required() throws Exception {

        // Watsonx doesn't return "tool_calls" when the tool-choice-option is set to REQUIRED.
        // In this case, the SDK forces the finish response.

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                    "model": "model",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "",
                                "tool_calls": [
                                    {
                                        "id": "chatcmpl-tool-6e63f95869944f03a86fdab6189ba0b5",
                                        "type": "function",
                                        "function": {
                                            "name": "getWeather",
                                            "arguments": "{\\"city\\": \\"Munich\\"}"
                                        }
                                    }
                                ]
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "created": 1755501551,
                    "model_version": "3.2.0",
                    "created_at": "2025-08-18T07:19:13.136Z",
                    "usage": {
                        "completion_tokens": 23,
                        "prompt_tokens": 309,
                        "total_tokens": 332
                    }
                }""");

        mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("project-id")
                .baseUrl(CloudRegion.FRANKFURT)
                .build();

            var parameters = ChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .build();

            var response = chatService.chat(List.of(UserMessage.text("Show me the weather in Munich")), parameters);
            assertEquals("tool_calls", response.finishReason().value());
        });
    }

    @Test
    void parameters_test() {

        var chatHandler = new ChatHandler() {
            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {}

            @Override
            public void onError(Throwable error) {}

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {}

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {}
        };

        var ex = assertThrows(NullPointerException.class, () -> AssistantMessage.text(null));
        assertEquals("Either content or toolCalls must be specified", ex.getMessage());

        var image = Image.of("jpeg", "mock");
        assertEquals("auto", image.detail());
        assertNotNull(image.url());

        image = Image.of("jpeg", "mock", Detail.HIGH);
        assertEquals("high", image.detail());
        assertNotNull(image.url());

        var imageContent = ImageContent.of("jpeg", "mock", Detail.LOW);
        assertEquals("image_url", imageContent.type());
        assertEquals("low", imageContent.imageUrl().detail());
        assertEquals("data:jpeg;base64,mock", imageContent.imageUrl().url());

        ex = assertThrows(NullPointerException.class, () -> ChatService.builder()
            .projectId("test")
            .authenticationProvider(mockAuthenticationProvider)
            .baseUrl(URI.create("http://localhost:8080"))
            .build());
        assertEquals("The modelId must be provided", ex.getMessage());


        ex = assertThrows(NullPointerException.class, () -> ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("test")
            .baseUrl(URI.create("http://localhost:8080"))
            .build());
        assertEquals("Either projectId or spaceId must be provided", ex.getMessage());

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("model-id")
                .projectId("project-id")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var chatParameters = ChatParameters.builder().build();

            var ex2 = assertThrows(NullPointerException.class, () -> chatService.chat(null, chatParameters));
            assertEquals("messages cannot be null", ex2.getMessage());
            ex2 =
                assertThrows(NullPointerException.class, () -> chatService.chatStreaming(null, chatParameters, chatHandler));
            assertEquals("messages cannot be null", ex2.getMessage());
        });
    }

    @Test
    void test_chat_streaming_model_not_supported_exception() throws Exception {

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "errors": [
                            {
                                "code": "model_not_supported",
                                "message": "Model 'doesn't exist' is not supported",
                                "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat-stream"
                            }
                        ],
                        "trace": "245ff8904f4aaf0fdaedc0bf05e2e45b",
                        "status_code": 404
                    }""")));

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("notExist")
            .projectId("project-id")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .build();

        CompletableFuture<ChatResponse> result = new CompletableFuture<>();
        ChatRequest chatRequest = ChatRequest.builder()
            .messages(UserMessage.text("fail"))
            .build();

        chatService.chatStreaming(chatRequest, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                fail("Should not be called");
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                fail("Should not be called");
            }

            @Override
            public void onError(Throwable error) {
                result.completeExceptionally(error);
            }
        });

        var ex = assertThrows(ExecutionException.class, () -> result.get(3, TimeUnit.SECONDS));
        assertTrue(WatsonxException.class.isInstance(ex.getCause()));
        WatsonxException e = (WatsonxException) ex.getCause();
        assertTrue(e.details().isPresent());
        assertEquals("model_not_supported", e.details().get().errors().get(0).code());
    }

    @Test
    void test_chat_streaming_error_event() {

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(2, 10)
                .withBody(
                    """
                        id: 1
                        event: error
                        data: error

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"Hello"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}
                        """)));

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var chatService = ChatService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .build();

        CompletableFuture<Throwable> futureEx = new CompletableFuture<>();
        CompletableFuture<String> futureMessage = new CompletableFuture<>();
        chatService.chatStreaming("Hello", new ChatHandler() {
            private StringBuilder builder = new StringBuilder();

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                builder.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureMessage.complete(builder.toString());
            }

            @Override
            public void onError(Throwable error) {
                futureEx.completeExceptionally(error);
            }
        });

        try {
            futureEx.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            var ex = assertInstanceOf(RuntimeException.class, e.getCause());
            assertEquals("error", ex.getMessage());
        }

        var message = assertDoesNotThrow(() -> futureMessage.get(3, TimeUnit.SECONDS));
        assertEquals("Hello", message);
    }

    @Test
    void test_chat_streaming_non_blocking_io_thread() {

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .withRequestBody(equalToJson(
                """
                    {
                      "model_id": "model-id",
                      "project_id": "project-id",
                      "messages": [
                        {
                          "role": "user",
                          "content": [{
                            "type": "text",
                            "text": "Ciao"
                          }]
                        }
                      ],
                      "time_limit": 60000
                    }"""
            ))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(3, 510)
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
                        """)));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .withRequestBody(equalToJson(
                """
                    {
                      "model_id": "model-id",
                      "project_id": "project-id",
                      "messages": [
                        {
                          "role": "user",
                          "content": [{
                            "type": "text",
                            "text": "Hello"
                          }]
                        }
                      ],
                      "time_limit": 60000
                    }"""
            ))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(3, 10)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"He"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.542Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"llo"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}
                        """)));

        List<String> listResult = Collections.synchronizedList(new ArrayList<>());

        try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {

            var executor = Executors.newCachedThreadPool();

            mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(Executors.newSingleThreadExecutor());
            when(mockAuthenticationProvider.asyncToken()).thenReturn(CompletableFuture.completedFuture("my-token"));

            var chatService = ChatService.builder()
                .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("project-id")
                .modelId("model-id")
                .build();

            List<CompletableFuture<Boolean>> futures = Collections.synchronizedList(new ArrayList<>());

            var chatHandler = new ChatHandler() {
                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    var nextTimeout = partialResponse.equals("C") ? Duration.ofMillis(500) : null;
                    if (nonNull(nextTimeout))
                        futures.add(CompletableFuture.supplyAsync(
                            () -> listResult.add(partialResponse),
                            CompletableFuture.delayedExecutor(nextTimeout.toMillis(), TimeUnit.MILLISECONDS, executor)));
                    else
                        futures.add(CompletableFuture.completedFuture(listResult.add(partialResponse)));
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {}

                @Override
                public void onError(Throwable error) {
                    fail(error);
                }
            };


            CompletableFuture.allOf(
                chatService.chatStreaming(List.of(UserMessage.text("Ciao")), chatHandler),
                chatService.chatStreaming(List.of(UserMessage.text("Hello")), chatHandler)).join();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            assertEquals(List.of("He", "llo", "iao", "C"), listResult);
        }
    }

    @Test
    void test_sync_interceptors() {

        List<WatsonxError.Error> errors =
            List.of(new WatsonxError.Error("authentication_token_expired", "Failed to authenticate the request due to an expired token", ""));
        WatsonxError detail = new WatsonxError(401, "57ed27e71f8e27dc25f00800a80b9529", errors);
        when(mockAuthenticationProvider.token())
            .thenThrow(new WatsonxException("Failed to authenticate the request due to an expired token", 401, detail))
            .thenReturn("my-super-token");

        wireMock.stubFor(post("/ml/v1/text/chat?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("SecondAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("SecondAttempt")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("ThirdAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("ThirdAttempt")
            .willReturn(aResponse().withStatus(504))
            .willSetStateTo("FourthAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("FourthAttempt")
            .willReturn(aResponse().withStatus(520))
            .willSetStateTo("FinalAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("FinalAttempt")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                            {
                              "id": "cmpl-15475d0dea9b4429a55843c77997f8a9",
                              "model_id": "meta-llama/llama-3-8b-instruct",
                              "created": 1689958352,
                              "created_at": "2023-07-21T16:52:32.190Z",
                              "choices": [
                                {
                                  "index": 0,
                                  "message": {
                                    "role": "assistant",
                                    "content": "The 2020 World Series was played at Globe Life Field in Arlington, Texas,\\nwhich is the home stadium of the Texas Rangers.\\nHowever, the series was played with no fans in attendance due to the COVID-19 pandemic.\\n"
                                  },
                                  "finish_reason": "stop"
                                }
                              ],
                              "usage": {
                                "completion_tokens": 47,
                                "prompt_tokens": 59,
                                "total_tokens": 106
                              }
                            }
                        """)));

        var chatService = ChatService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("model-id")
            .projectId("project-id")
            .logRequests(true)
            .logResponses(true)
            .build();

        chatService.chat("Hello");

        int count = wireMock.findAll(postRequestedFor(urlMatching("/ml/v1/text/chat.*"))).size();
        assertEquals(5, count);
    }

    @Test
    void test_async_interceptors() {

        List<WatsonxError.Error> errors =
            List.of(new WatsonxError.Error("authentication_token_expired", "Failed to authenticate the request due to an expired token", ""));
        WatsonxError detail = new WatsonxError(401, "57ed27e71f8e27dc25f00800a80b9529", errors);

        String FIRST_CALL = """
            {
              "model_id" : "model-id",
              "project_id" : "project-id",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Hello 1"
                } ]
              } ],
              "time_limit" : 60000
            }""";

        String SECOND_CALL = """
            {
              "model_id" : "model-id",
              "project_id" : "project-id",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Hello 2"
                } ]
              } ],
              "time_limit" : 60000
            }""";

        when(mockAuthenticationProvider.asyncToken())
            .thenReturn(failedFuture(new WatsonxException("Failed to authenticate the request due to an expired token", 401, detail)))
            .thenReturn(completedFuture("my-super-token"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs(Scenario.STARTED)
            .withRequestBody(equalToJson(FIRST_CALL))
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("SecondAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("SecondAttempt")
            .withRequestBody(equalToJson(FIRST_CALL))
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("ThirdAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("ThirdAttempt")
            .withRequestBody(equalToJson(FIRST_CALL))
            .willReturn(aResponse().withStatus(504))
            .willSetStateTo("FourthAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("FourthAttempt")
            .withRequestBody(equalToJson(FIRST_CALL))
            .willReturn(aResponse().withStatus(520))
            .willSetStateTo("FinalAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario")
            .whenScenarioStateIs("FinalAttempt")
            .withRequestBody(equalToJson(FIRST_CALL))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(3, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"He"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.542Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"llo"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}
                        """)));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario2")
            .whenScenarioStateIs(Scenario.STARTED)
            .withRequestBody(equalToJson(SECOND_CALL))
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("SecondAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario2")
            .whenScenarioStateIs("SecondAttempt")
            .withRequestBody(equalToJson(SECOND_CALL))
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("ThirdAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario2")
            .whenScenarioStateIs("ThirdAttempt")
            .withRequestBody(equalToJson(SECOND_CALL))
            .willReturn(aResponse().withStatus(504))
            .willSetStateTo("FourthAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario2")
            .whenScenarioStateIs("FourthAttempt")
            .withRequestBody(equalToJson(SECOND_CALL))
            .willReturn(aResponse().withStatus(520))
            .willSetStateTo("FinalAttempt"));

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=" + API_VERSION)
            .inScenario("RetryScenario2")
            .whenScenarioStateIs("FinalAttempt")
            .withRequestBody(equalToJson(SECOND_CALL))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(3, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}

                        id: 2
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"He"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.542Z"}

                        id: 3
                        event: message
                        data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"content":"llo"}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.552Z"}

                        id: 4
                        event: close
                        data: {}
                        """)));

        var chatService = ChatService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .authenticationProvider(mockAuthenticationProvider)
            .modelId("model-id")
            .projectId("project-id")
            .build();

        assertDoesNotThrow(() -> CompletableFuture.allOf(
            chatService.chatStreaming("Hello 1", mock(ChatHandler.class)),
            chatService.chatStreaming("Hello 2", mock(ChatHandler.class))
        ).get(3, TimeUnit.SECONDS));
        int count = wireMock.findAll(postRequestedFor(urlMatching("/ml/v1/text/chat_stream.*"))).size();
        assertEquals(10, count);
    }

    @Test
    void test_executor() throws Exception {

        String BODY = new String(ClassLoader.getSystemResourceAsStream("granite_thinking_streaming_response.txt").readAllBytes());

        wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(5, 200)
                .withBody(BODY)));

        List<String> threadNames = new ArrayList<>();

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-token"));

        Executor ioExecutor = Executors.newSingleThreadExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "my-thread"));

        try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {
            mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);
            var chatService = ChatService.builder()
                .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("project-id")
                .modelId("model-id")
                .build();

            var chatRequest = ChatRequest.builder()
                .messages(ControlMessage.of("thinking"), UserMessage.text("Hello"))
                .thinking(Thinking.of(ExtractionTags.of("think", "response")))
                .build();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            chatService.chatStreaming(chatRequest, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    assertEquals("my-thread", Thread.currentThread().getName());
                    assertNotNull(partialResponse);
                    assertNotNull(partialChatResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    assertEquals("my-thread", Thread.currentThread().getName());
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    assertEquals("my-thread", Thread.currentThread().getName());
                    assertInstanceOf(JsonParseException.class, error.getCause());
                }

                @Override
                public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
                    assertEquals("my-thread", Thread.currentThread().getName());
                    assertNotNull(partialThinking);
                    assertNotNull(partialChatResponse);
                }
            });

            result.get(3, TimeUnit.SECONDS);
            assertEquals(1, threadNames.size());
            assertEquals("my-thread", threadNames.get(0));
        }
    }

    @Test
    void test_exception() throws Exception {

        when(mockHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("project-id")
                .modelId("model-id")
                .build();

            assertThrows(RuntimeException.class, () -> chatService.chat(UserMessage.text("test")), "IOException");
        });
    }

    @Test
    void test_builder_with_api_key() {

        withWatsonxServiceMock(() -> {

            var chatServiceBuilder = ChatService.builder()
                .apiKey("my-api-key");

            assertNotNull(chatServiceBuilder.getAuthenticationProvider());
            var spyAuthenticator = spy(chatServiceBuilder.getAuthenticationProvider());

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn(
                """
                    {
                        "access_token": "my-super-token",
                        "refresh_token": "not_supported",
                        "ims_user_id": 14364907,
                        "token_type": "Bearer",
                        "expires_in": 3600,
                        "expiration": 1757106813,
                        "scope": "ibm openid"
                    }""");

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));
            spyAuthenticator.token();
            assertEquals("grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=my-api-key", bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void test_serialize_assistant_message() {

        // The thinking field must not be serialized.
        AssistantMessage assistantMessage = new AssistantMessage("content", "thinking", "name", "refusal", null);
        String json = Json.prettyPrint(assistantMessage);
        JSONAssert.assertEquals("""
                {
                    "role": "assistant",
                    "content": "content",
                    "name": "name",
                    "refusal": "refusal"
                }
            """, json, true);
    }
}
