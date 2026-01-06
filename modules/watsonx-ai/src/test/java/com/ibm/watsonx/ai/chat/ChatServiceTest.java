/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.AudioContent;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.chat.model.UserContent;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.VideoContent;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class ChatServiceTest extends AbstractWatsonxTest {

    @Test
    void should_not_add_messages_when_list_is_empty() {

        ChatRequest.Builder chatRequest = ChatRequest.builder()
            .messages(SystemMessage.of("You are an helpful assistant"));

        assertEquals(1, chatRequest.build().messages().size());

        chatRequest.addMessages(List.of());
        assertEquals(1, chatRequest.build().messages().size());
    }

    @Test
    void should_append_user_message_after_system_message() {

        var chatRequest = ChatRequest.builder()
            .messages(SystemMessage.of("You are an helpful assistant"))
            .addMessages(UserMessage.text("How are you?"))
            .build();

        assertEquals(2, chatRequest.messages().size());
        assertInstanceOf(SystemMessage.class, chatRequest.messages().get(0));
        assertInstanceOf(UserMessage.class, chatRequest.messages().get(1));
    }

    @Test
    void should_send_all_chat_parameters_correctly_and_receive_response() {

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
                .responseAsJson()
                .seed(10)
                .spaceId("space-id")
                .stop(List.of("stop"))
                .temperature(1.0)
                .timeLimit(Duration.ofSeconds(60))
                .toolChoice("my-tool")
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .topLogprobs(10)
                .topP(1.2)
                .guidedChoice("guidedChoice")
                .guidedGrammar("guidedGrammar")
                .guidedRegex("guidedRegex")
                .repetitionPenalty(2.0)
                .lengthPenalty(2.0)
                .responseAsJsonSchema("test", Map.of(), false)
                .build();

            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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

            when(mockAuthenticator.token()).thenReturn("my-super-token");
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
            assertEquals("chatcmpl-43962cc06e5346ccbd653a04a48e4b5b", chatResponse.id());
            assertEquals("my-super-model", chatResponse.modelId());
            assertEquals("my-super-model-model", chatResponse.model());
            assertNotNull(chatResponse.choices());
            assertEquals(1, chatResponse.choices().size());
            assertEquals(0, chatResponse.choices().get(0).index());
            assertEquals(0, chatResponse.choices().get(0).index());
            assertEquals("assistant", chatResponse.choices().get(0).message().role());
            assertEquals("Hello!!!", chatResponse.choices().get(0).message().content());
            assertEquals("stop", chatResponse.choices().get(0).finishReason());
            assertEquals(1749288614, chatResponse.created());
            assertEquals("2025-06-07T09:30:15.122Z", chatResponse.createdAt());
            assertNotNull(chatResponse.usage());
            assertEquals(37, chatResponse.usage().completionTokens());
            assertEquals(66, chatResponse.usage().promptTokens());
            assertEquals(103, chatResponse.usage().totalTokens());

            HttpRequest actualRequest = mockHttpRequest.getValue();
            assertEquals("http://my-cloud-instance.com/ml/v1/text/chat?version=1988-03-23", actualRequest.uri().toString());
            assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
            assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
            assertEquals("POST", actualRequest.method());

            String expectedBody = """
                {
                   "model_id":"my-super-model",
                   "space_id":"space-id",
                   "project_id":"project-id",
                   "messages":[
                      {
                         "role":"user",
                         "content":[
                            {
                               "type":"text",
                               "text":"Hello"
                            }
                         ]
                      }
                   ],
                   "tool_choice_option":"required",
                   "tool_choice":{
                      "function":{
                         "name":"my-tool"
                      },
                      "type":"function"
                   },
                   "frequency_penalty":2.0,
                   "logit_bias":{
                      "test":-10
                   },
                   "logprobs":true,
                   "top_logprobs":10,
                   "max_completion_tokens":0,
                   "n":10,
                   "presence_penalty":1.0,
                   "seed":10,
                   "stop":[
                      "stop"
                   ],
                   "temperature":1.0,
                   "top_p":1.2,
                   "time_limit":60000,
                   "response_format":{
                      "json_schema":{
                         "name":"test",
                         "schema":{

                         },
                         "strict":false
                      },
                      "type":"json_schema"
                   },
                   "guided_choice":[
                      "guidedChoice"
                   ],
                   "guided_regex":"guidedRegex",
                   "guided_grammar":"guidedGrammar",
                   "repetition_penalty":2.0,
                   "length_penalty":2.0
                }""";

            JSONAssert.assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_use_default_chat_parameters() throws Exception {

        withWatsonxServiceMock(() -> {

            var tool = Tool.of("get_current_time", "Get the current time");
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("my-default-model")
                .projectId("default-project-id")
                .spaceId("default-space-id")
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
                        .crypto("crypto")
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
                    .crypto("crypto")
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

            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("my-default-model")
                .projectId("default-project-id")
                .spaceId("default-space-id")
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
                        .crypto("crypto")
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

            chatService.chat(
                messages,
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
                    .crypto("crypto_override")
                    .build(),
                List.of(ovverideTool)
            );

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
                    .crypto("crypto_override")
                    .tools(List.of(ovverideTool))
                    .build());

            assertEquals(expectedBody, bodyPublisherToString(mockHttpRequest));
        });
    }

    @Test
    void should_send_text_chat_messages_with_parameters() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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
    void should_call_tool_and_parse_tool_response_correctly() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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
                        JsonSchema.object()
                            .property("location", JsonSchema.string("The city, e.g. San Francisco, CA"))
                            .property("unit", JsonSchema.enumeration("celsius", "fahrenheit"))
                            .required("location")))
                    .parameters(parameters)
                    .build()
            );

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void should_return_text_response_when_response_mode_is_text() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant designed to output JSON."),
                UserMessage.text("Who won the world series in 2020?"));

            var parameters = ChatParameters.builder().responseAsText().build();
            var chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    @Test
    void should_return_json_response_when_response_mode_is_json() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("You are a helpful assistant designed to output JSON."),
                UserMessage.text("Who won the world series in 2020?"));

            var parameters = ChatParameters.builder().responseAsJson().build();
            var chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
        });
    }

    record LLMResponse(String name, List<Province> provinces) {}
    record Province(String name, Population population) {}
    record Population(int value, String density) {}

    @Test
    void should_return_json_schema_response_and_parse_it_correctly() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .logRequests(true)
                .modelId("meta-llama/llama-3-8b-instruct")
                .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
                .baseUrl(CloudRegion.DALLAS)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(
                SystemMessage.of("Given a region return the information"),
                UserMessage.text("Campania"));

            var jsonSchema = JsonSchema.object()
                .property("name", JsonSchema.string())
                .property("provinces", JsonSchema.array()
                    .items(JsonSchema.object()
                        .property("name", JsonSchema.string())
                        .property("population",
                            JsonSchema.object()
                                .property("value", JsonSchema.number())
                                .property("density", JsonSchema.enumeration("LOW", "MEDIUM", "HIGH"))
                                .required("value", "density")
                        ))
                ).build();

            var parameters = ChatParameters.builder()
                .responseAsJsonSchema("test", jsonSchema, true)
                .build();

            var chatResponse = chatService.chat(messages, parameters);

            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), true);

            var response = chatResponse.toAssistantMessage().toObject(LLMResponse.class);
            assertEquals("Campania", response.name());
            assertEquals(5, response.provinces.size());
            assertEquals(FinishReason.STOP, chatResponse.finishReason());
            assertEquals(new Province("Caserta", new Population(924414, "LOW")), response.provinces.get(0));
            assertEquals(new Province("Benevento", new Population(283393, "LOW")), response.provinces.get(1));
            assertEquals(new Province("Napoli", new Population(3116402, "HIGH")), response.provinces.get(2));
            assertEquals(new Province("Avellino", new Population(423536, "LOW")), response.provinces.get(3));
            assertEquals(new Province("Salerno", new Population(1108369, "MEDIUM")), response.provinces.get(4));

            parameters = ChatParameters.builder()
                .responseAsJsonSchema(jsonSchema)
                .build();

            chatResponse = chatService.chat(messages, parameters);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), true);
        });
    }

    @Test
    void should_return_text_result_after_tool_call_with_output() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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
                JsonSchema.object()
                    .property("first_number", JsonSchema.integer())
                    .property("second_number", JsonSchema.integer())
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
    void should_send_text_content_in_user_message() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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
    void should_send_image_content_in_user_message() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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
    void should_send_video_content_in_user_message() throws Exception {

        var bytes = ClassLoader.getSystemResourceAsStream("video.mp4").readAllBytes();
        var file = new File(ClassLoader.getSystemResource("video.mp4").toURI());

        final String REQUEST = """
            {
              "model_id" : "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
              "project_id" : "project-id",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Tell me more about this video"
                }, {
                  "type" : "video_url",
                  "video_url" : {
                    "url" : "data:video/mp4;base64,%s"
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
                                  "content": "This is a test video file."
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

        when(mockAuthenticator.token()).thenReturn("my-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
                .projectId("project-id")
                .baseUrl(CloudRegion.TORONTO)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var message = UserMessage.of(
                TextContent.of("Tell me more about this video"),
                VideoContent.of("video/mp4", Base64.getEncoder().encodeToString(bytes))
            );

            var chatResponse = chatService.chat(message);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
            assertEquals(
                VideoContent.of("video/mp4", Base64.getEncoder().encodeToString(bytes)),
                assertDoesNotThrow(() -> VideoContent.from(file)));
        });
    }

    @Test
    void should_send_audio_content_in_user_message() throws Exception {

        var bytes = ClassLoader.getSystemResourceAsStream("audio.wav").readAllBytes();
        var file = new File(ClassLoader.getSystemResource("audio.wav").toURI());

        final String REQUEST = """
            {
              "model_id" : "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
              "project_id" : "project-id",
              "messages" : [ {
                "role" : "user",
                "content" : [ {
                  "type" : "text",
                  "text" : "Tell me more about this audio"
                }, {
                  "type" : "input_audio",
                  "input_audio" : {
                    "data" : "%s",
                    "format" : "audio/x-wav"
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
                                  "content": "This is a test audio file."
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

        when(mockAuthenticator.token()).thenReturn("my-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
                .projectId("project-id")
                .baseUrl(CloudRegion.TORONTO)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var message = UserMessage.of(
                TextContent.of("Tell me more about this audio"),
                AudioContent.of("audio/x-wav", Base64.getEncoder().encodeToString(bytes))
            );

            var chatResponse = chatService.chat(message);
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
            assertEquals(
                AudioContent.of("audio/x-wav", Base64.getEncoder().encodeToString(bytes)),
                assertDoesNotThrow(() -> AudioContent.from(file)));
        });
    }

    @Test
    void should_send_control_message_and_extract_thinking_and_response() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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

            var thinking = chatResponse.toAssistantMessage().thinking();
            var response = chatResponse.toAssistantMessage().content();
            assertEquals("Think", thinking);
            assertEquals("Result", response);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");

            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Result", assistantMessage.content());
            assertEquals("Think", assistantMessage.thinking());
        });
    }

    @Test
    void should_extract_thinking_and_response_from_control_message_with_single_tag() throws Exception {

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

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
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

            var thinking = chatResponse.toAssistantMessage().thinking();
            var response = chatResponse.toAssistantMessage().content();
            assertEquals("Think", thinking);
            assertEquals("Result", response);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }


    @Test
    void should_retry_sync_interceptors_until_success() {

        List<WatsonxError.Error> errors =
            List.of(new WatsonxError.Error("authentication_token_expired", "Failed to authenticate the request due to an expired token", ""));
        WatsonxError detail = new WatsonxError(401, "57ed27e71f8e27dc25f00800a80b9529", errors);
        when(mockAuthenticator.token())
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
            .authenticator(mockAuthenticator)
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
    void should_throw_runtime_exception_on_io_exception() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .projectId("project-id")
                .modelId("model-id")
                .build();

            assertThrows(RuntimeException.class, () -> chatService.chat(UserMessage.text("test")), "IOException");
        });
    }

    @Test
    void should_build_chat_service_with_api_key() {

        withWatsonxServiceMock(() -> {

            var chatServiceBuilder = ChatService.builder()
                .apiKey("my-api-key");

            assertNotNull(chatServiceBuilder.authenticator());
            var spyAuthenticator = spy(chatServiceBuilder.authenticator());

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
    void should_serialize_assistant_message_without_thinking_field() {

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

    @Test
    void should_send_a_tool_without_parameters() throws Exception {

        final String REQUEST = """
            {
                "model_id": "ibm/granite-4-h-small",
                "project_id": "project-id",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "What time is in Italy?"
                            }
                        ]
                    }
                ],
                "tools": [
                    {
                        "type": "function",
                        "function": {
                            "name": "get_current_time"
                        }
                    }
                ],
                "time_limit": 60000
            }""";

        final String RESPONSE =
            """
                {
                    "id": "chatcmpl-58a80ac1bb874d55a143d489dc5daccf",
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
                                        "id": "chatcmpl-tool-2eb69938d25a4004a9c0d83b665bbd08",
                                        "type": "function",
                                        "function": {
                                            "name": "get_current_time",
                                            "arguments": "null"
                                        }
                                    }
                                ]
                            },
                            "finish_reason": "tool_calls"
                        }
                    ],
                    "created": 1764609585,
                    "model_version": "4.0.0",
                    "created_at": "2025-12-01T17:19:45.682Z",
                    "usage": {
                        "completion_tokens": 17,
                        "prompt_tokens": 157,
                        "total_tokens": 174
                    }
                }""";

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-4-h-small")
                .projectId("project-id")
                .baseUrl(CloudRegion.LONDON)
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var messages = List.<ChatMessage>of(UserMessage.text("What time is in Italy?"));
            var chatResponse = chatService.chat(messages, Tool.of("get_current_time"));
            JSONAssert.assertEquals(REQUEST, bodyPublisherToString(mockHttpRequest), false);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-4-h-small")
                .projectId("project-id")
                .baseUrl(CloudRegion.LONDON)
                .messageInterceptor((ctx, message) -> "New message")
                .toolInterceptor((ctx, fc) -> fc.withArguments(Json.fromJson(fc.arguments(), String.class)))
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var tool = Tool.of("get_current_time", JsonSchema.object().property("country", JsonSchema.string()));
            var messages = List.<ChatMessage>of(UserMessage.text("What time is it in Italy?"));
            var chatResponse = chatService.chat(messages, tool);
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
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-4-h-small")
                .projectId("project-id")
                .baseUrl(CloudRegion.LONDON)
                .messageInterceptor((ctx, message) -> "I don't feel good.")
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = chatService.chat("What time is it in Italy?");
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("I don't feel good.", assistantMessage.content());
            assertFalse(assistantMessage.hasToolCalls());
        });
    }

    @Test
    void should_convert_multiple_choices_to_assistant_messages() throws Exception {

        var RESPONSE = new String(ClassLoader.getSystemResourceAsStream("chat_response_multiple_choices.json").readAllBytes());

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-4-h-small")
                .projectId("project-id")
                .baseUrl("http://localhost")
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = chatService.chat("What is the capital of Italy?");
            var assistantMessages = chatResponse.toAssistantMessages();
            assertEquals("The capital of Italy is Rome. It is also the country's largest city and a significant center of history, art, and culture.",
                assistantMessages.get(0).content());
            assertFalse(assistantMessages.get(0).hasToolCalls());
            assertEquals("The capital of Italy is Rome.", assistantMessages.get(1).content());
            assertFalse(assistantMessages.get(1).hasToolCalls());
        });
    }

    @Test
    void should_apply_message_interceptor_to_multiple_choices() throws Exception {

        var RESPONSE = new String(ClassLoader.getSystemResourceAsStream("chat_response_multiple_choices.json").readAllBytes());

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-4-h-small")
                .projectId("project-id")
                .baseUrl("http://localhost")
                .messageInterceptor((ctx, message) -> message.replace("Rome", "rome"))
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = chatService.chat("What is the capital of Italy?");
            var assistantMessages = chatResponse.toAssistantMessages();
            assertEquals("The capital of Italy is rome. It is also the country's largest city and a significant center of history, art, and culture.",
                assistantMessages.get(0).content());
            assertFalse(assistantMessages.get(0).hasToolCalls());
            assertEquals("The capital of Italy is rome.", assistantMessages.get(1).content());
            assertFalse(assistantMessages.get(1).hasToolCalls());
        });
    }

    @Test
    void should_handle_multiple_choices_with_different_finish_reasons() throws Exception {

        var RESPONSE =
            new String(ClassLoader.getSystemResourceAsStream("chat_response_multiple_choices_different_finish_reasons.json").readAllBytes());

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("project-id")
                .baseUrl("http://localhost")
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = chatService.chat("What is the capital of France?");
            var assistantMessages = chatResponse.toAssistantMessages();

            assertEquals(2, assistantMessages.size());
            assertEquals("Paris is the capital and largest city of France.", assistantMessages.get(0).content());
            assertFalse(assistantMessages.get(0).hasToolCalls());
            assertEquals(
                "Paris, the capital of France, is known for its art, fashion, gastronomy, and culture. The city is home to iconic landmarks such as the Eiffel Tower, the Louvre Museum, Notre-Dame Cathedral",
                assistantMessages.get(1).content());
            assertFalse(assistantMessages.get(1).hasToolCalls());

            assertEquals(2, chatResponse.choices().size());
            assertEquals("stop", chatResponse.choices().get(0).finishReason());
            assertEquals("length", chatResponse.choices().get(1).finishReason());

            assertEquals(FinishReason.STOP, FinishReason.fromValue(chatResponse.choices().get(0).finishReason()));
            assertEquals(FinishReason.LENGTH, FinishReason.fromValue(chatResponse.choices().get(1).finishReason()));
        });
    }

    @Test
    void should_handle_multiple_choices_with_tools() throws Exception {

        var RESPONSE = new String(ClassLoader.getSystemResourceAsStream("chat_response_multiple_choices_with_tools.json").readAllBytes());

        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);

        withWatsonxServiceMock(() -> {
            var chatService = ChatService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-3-8b-instruct")
                .projectId("project-id")
                .baseUrl("http://localhost")
                .toolInterceptor((ctx, fc) -> fc.withArguments(fc.arguments().replace("Paris", "Rome")))
                .build();

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var chatResponse = chatService.chat("What's the weather in Paris?");
            var assistantMessages = chatResponse.toAssistantMessages();

            assertEquals(2, assistantMessages.size());

            assertNull(assistantMessages.get(0).content());
            assertTrue(assistantMessages.get(0).hasToolCalls());
            assertEquals(1, assistantMessages.get(0).toolCalls().size());
            assertEquals("get_weather", assistantMessages.get(0).toolCalls().get(0).function().name());
            assertEquals("{\"location\": \"Rome\", \"unit\": \"celsius\"}",
                assistantMessages.get(0).toolCalls().get(0).function().arguments());
            assertEquals("call_abc123", assistantMessages.get(0).toolCalls().get(0).id());

            assertNull(assistantMessages.get(1).content());
            assertTrue(assistantMessages.get(1).hasToolCalls());
            assertEquals(1, assistantMessages.get(1).toolCalls().size());
            assertEquals("get_weather", assistantMessages.get(1).toolCalls().get(0).function().name());
            assertEquals("{\"location\": \"Rome\", \"unit\": \"fahrenheit\"}",
                assistantMessages.get(1).toolCalls().get(0).function().arguments());
            assertEquals("call_def456", assistantMessages.get(1).toolCalls().get(0).id());

            assertEquals("tool_calls", chatResponse.choices().get(0).finishReason());
            assertEquals("tool_calls", chatResponse.choices().get(1).finishReason());
            assertEquals(FinishReason.TOOL_CALLS, chatResponse.finishReason());
        });
    }
}
