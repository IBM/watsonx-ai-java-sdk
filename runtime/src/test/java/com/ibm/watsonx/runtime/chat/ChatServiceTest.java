package com.ibm.watsonx.runtime.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.runtime.utils.Utils.bodyPublisherToString;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ibm.watsonx.core.Json;
import com.ibm.watsonx.core.auth.AuthenticationProvider;
import com.ibm.watsonx.core.chat.JsonSchema;
import com.ibm.watsonx.core.chat.JsonSchema.EnumSchema;
import com.ibm.watsonx.core.chat.JsonSchema.IntegerSchema;
import com.ibm.watsonx.core.chat.JsonSchema.StringSchema;
import com.ibm.watsonx.runtime.CloudRegion;
import com.ibm.watsonx.runtime.chat.model.AssistantMessage;
import com.ibm.watsonx.runtime.chat.model.ChatMessage;
import com.ibm.watsonx.runtime.chat.model.ChatParameters;
import com.ibm.watsonx.runtime.chat.model.ChatParameters.ResponseFormat;
import com.ibm.watsonx.runtime.chat.model.ChatParameters.ToolChoice;
import com.ibm.watsonx.runtime.chat.model.ControlMessage;
import com.ibm.watsonx.runtime.chat.model.Image;
import com.ibm.watsonx.runtime.chat.model.Image.Detail;
import com.ibm.watsonx.runtime.chat.model.ImageContent;
import com.ibm.watsonx.runtime.chat.model.PartialChatResponse;
import com.ibm.watsonx.runtime.chat.model.SystemMessage;
import com.ibm.watsonx.runtime.chat.model.TextContent;
import com.ibm.watsonx.runtime.chat.model.Tool;
import com.ibm.watsonx.runtime.chat.model.ToolCall;
import com.ibm.watsonx.runtime.chat.model.ToolMessage;
import com.ibm.watsonx.runtime.chat.model.UserContent;
import com.ibm.watsonx.runtime.chat.model.UserMessage;
import com.ibm.watsonx.runtime.chat.model.VideoContent;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class ChatServiceTest {

  @Mock
  HttpClient mockHttpClient;

  @Mock
  AuthenticationProvider mockAuthenticationProvider;

  @Mock
  HttpResponse<String> mockHttpResponse;

  @Test
  void try_all_chat_parameters() throws Exception {

    var chatParameters = ChatParameters.builder()
      .frequencyPenalty(2.0)
      .logitBias(Map.of("test", -10))
      .logprobs(true)
      .maxCompletionTokens(0)
      .modelId("my-super-model")
      .n(10)
      .presencePenalty(1.0)
      .projectId("project-id")
      .responseFormat(ResponseFormat.JSON)
      .seed(10)
      .spaceId("space-id")
      .stop(List.of("stop"))
      .temperature(1.0)
      .timeLimit(Duration.ofSeconds(60))
      .toolChoice("my-tool")
      .toolChoiceOption(ToolChoice.REQUIRED)
      .topLogprobs(10)
      .topP(1.2)
      .build();

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .logRequests(true)
      .logResponses(true)
      // These values will be overridden by the chat parameters
      .modelId("my-default-model")
      .projectId("default-project-id")
      .spaceId("default-space-id")
      // ------------------------------------------------------
      .timeout(Duration.ofSeconds(60))
      .url("http://my-cloud-instance.com")
      .version(("1988-03-23"))
      .build();

    var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
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

    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var chatResponse = chatService.chat(messages, chatParameters);
    assertEquals("chatcmpl-43962cc06e5346ccbd653a04a48e4b5b", chatResponse.getId());
    assertEquals("my-super-model", chatResponse.getModelId());
    assertEquals("my-super-model-model", chatResponse.getModel());
    assertNotNull(chatResponse.getChoices());
    assertEquals(1, chatResponse.getChoices().size());
    assertEquals(0, chatResponse.getChoices().get(0).index());
    assertEquals(0, chatResponse.getChoices().get(0).index());
    assertEquals("assistant", chatResponse.getChoices().get(0).message().role());
    assertEquals("Hello!!!", chatResponse.getChoices().get(0).message().content());
    assertEquals("stop", chatResponse.getChoices().get(0).finishReason());
    assertEquals(1749288614, chatResponse.getCreated());
    assertEquals("2025-06-07T09:30:15.122Z", chatResponse.getCreatedAt());
    assertNotNull(chatResponse.getUsage());
    assertEquals(37, chatResponse.getUsage().getCompletionTokens());
    assertEquals(66, chatResponse.getUsage().getPromptTokens());
    assertEquals(103, chatResponse.getUsage().getTotalTokens());

    HttpRequest actualRequest = captor.getValue();
    assertEquals("http://my-cloud-instance.com/ml/v1/text/chat?version=1988-03-23", actualRequest.uri().toString());
    assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
    assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
    assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
    assertEquals("POST", actualRequest.method());

    String expectedBody = Json.toJson(
      ChatRequest.builder()
        .modelId("my-super-model")
        .projectId("project-id")
        .spaceId("space-id")
        .messages(messages)
        .parameters(chatParameters)
        .build());

    assertEquals(expectedBody, bodyPublisherToString(captor));
  }

  @Test
  void try_default_chat_parameters() throws Exception {

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("my-default-model")
      .projectId("default-project-id")
      .spaceId("default-space-id")
      .url(CloudRegion.FRANKFURT)
      .build();

    var messages = List.<ChatMessage>of(UserMessage.text("Hello"));

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
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

    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    chatService.chat(messages);
    HttpRequest actualRequest = captor.getValue();
    assertEquals("https://eu-de.ml.cloud.ibm.com/ml/v1/text/chat?version=2025-04-23",
      actualRequest.uri().toString());
    assertEquals("Bearer my-super-token", actualRequest.headers().firstValue("Authorization").orElse(""));
    assertEquals("application/json", actualRequest.headers().firstValue("Accept").orElse(""));
    assertEquals("application/json", actualRequest.headers().firstValue("Content-Type").orElse(""));
    assertEquals("POST", actualRequest.method());

    String expectedBody = Json.toJson(
      ChatRequest.builder()
        .modelId("my-default-model")
        .projectId("default-project-id")
        .spaceId("default-space-id")
        .messages(messages)
        .timeLimit(10000L)
        .build());

    assertEquals(expectedBody, bodyPublisherToString(captor));
  }

  @Test
  void text_chat() throws Exception {

    final String REQUEST = """
      {
        "model_id": "meta-llama/llama-3-8b-instruct",
        "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
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

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("meta-llama/llama-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.LONDON)
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
      .build();

    var chatResponse = chatService.chat(messages, parameters);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("meta-llama/llama-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.DALLAS)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(
      UserMessage.text("What is the weather like in Boston today?"));

    var tools = List.of(
      Tool.of(
        "get_current_weather",
        JsonSchema.builder()
          .addProperty("location", StringSchema.of("The city, e.g. San Francisco, CA"))
          .addProperty("unit", EnumSchema.of("celsius", "fahrenheit"))
          .required("location")
          .build()));

    var parameters = ChatParameters.builder()
      .toolChoice("get_current_weather")
      .build();

    var chatResponse = chatService.chat(messages, tools, parameters);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
    System.out.println(chatResponse.getUsage());
    assertEquals("ChatUsage [completionTokens=18, promptTokens=19, totalTokens=37]",
      chatResponse.getUsage().toString());
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("meta-llama/llama-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.DALLAS)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(
      SystemMessage.of("You are a helpful assistant designed to output JSON."),
      UserMessage.text("Who won the world series in 2020?"));

    var parameters = ChatParameters.builder().responseFormat(ResponseFormat.JSON).build();
    var chatResponse = chatService.chat(messages, parameters);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("ibm/granite-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.DALLAS)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(UserMessage.text("2 + 2"));
    var tools = Tool.of(
      "sum",
      "Execute the sum of two numbers",
      JsonSchema.builder()
        .addProperty("first_number", IntegerSchema.of())
        .addProperty("second_number", IntegerSchema.of())
        .required(List.of("first_number", "second_number"))
        .build());

    var chatResponse = chatService.chat(messages, List.of(tools));

    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("ibm/granite-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.LONDON)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(
      UserMessage.text("2 + 2"),
      AssistantMessage.of(ToolCall.of("id", "sum", "{\"first_number\": 2, \"second_number\": 2}")),
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
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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
        "time_limit" : 10000
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("ibm/granite-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.TOKYO)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(UserMessage.of("Alan", List.<UserContent>of(TextContent.of("2 + 2"))));
    var chatResponse = chatService.chat(messages);

    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
  }

  @Test
  void test_image_content_user_message() throws Exception {

    var bytes = getClass().getClassLoader().getResourceAsStream("IBM.svg").readAllBytes();

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
              "url" : "data:image/svg;base64,%s",
              "detail" : "auto"
            }
          } ]
        } ],
        "time_limit" : 10000
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.TORONTO)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var message = UserMessage.of(
      TextContent.of("Tell me more about this image"),
      ImageContent.of("image/svg", Base64.getEncoder().encodeToString(bytes))
    );

    var chatResponse = chatService.chat(message);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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
        "time_limit" : 10000
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.DALLAS)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var message = UserMessage.of(List.of(
      TextContent.of("Tell me more about this video"),
      VideoContent.of("video/mp4", "ABC")
    ));

    var chatResponse = chatService.chat(message);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
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
                        "content": "<think>The problem is straightforward: calculate the sum of 1 and 1. \\n\\nThe arithmetic operation involved here is addition. When you add two 1s together, you simply count up to a total of 2. This is a basic arithmetic fact:\\n\\n\\\\[ 1 + 1 = 2 \\\\]\\n\\nThere are no additional complexities or conditions given, so the result should be clear and unambiguous.</think><response>### Calculation:\\n\\nTo find the result of \\\\( 1 + 1 \\\\), perform the basic addition:\\n\\n\\\\[ 1 + 1 = 2 \\\\]\\n\\n### Conclusion:\\n\\nThe result of \\\\( 1 + 1 \\\\) is \\\\( \\\\boxed{2} \\\\).</response>"
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .modelId("ibm/granite-3-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url(CloudRegion.SYDNEY)
      .build();

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var messages = List.<ChatMessage>of(
      ControlMessage.of("thinking"),
      UserMessage.text("What is the result of 1 + 1")
    );

    var chatResponse = chatService.chat(messages);
    JSONAssert.assertEquals(REQUEST, bodyPublisherToString(captor), false);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(chatResponse), false);
  }

  @Test
  void chat_streaming_test() throws Exception {

    final String VERSION = "2020-03-15";

    WireMockServer wireMock = new WireMockServer();
    configureFor("localhost", 8080);

    wireMock.start();

    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(VERSION))
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
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
            "time_limit": 10000,
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


    when(mockAuthenticationProvider.getTokenAsync()).thenReturn(completedFuture("my-super-token"));

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(HttpClient.newBuilder().executor(Executors.newSingleThreadExecutor()).build())
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url("http://localhost:8080")
      .version(VERSION)
      .build();

    var messages = List.<ChatMessage>of(
      SystemMessage.of("You are an expert translator and you give the translation of single words"),
      UserMessage.text("Translate \"Hello\" in Italian")
    );

    var chatParameters = ChatParameters.builder()
      .maxCompletionTokens(0)
      .temperature(0.0)
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
    });

    ChatResponse response = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
    assertNotNull(response);
    assertNotNull(response.getChoices());
    assertEquals(1, response.getChoices().size());
    assertEquals("stop", response.getChoices().get(0).finishReason());
    assertEquals(0, response.getChoices().get(0).index());
    assertEquals("Ciao", response.getChoices().get(0).message().content());
    assertEquals("Ciao", response.textResponse().orElse(null));
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
    wireMock.stop();
  }

  @Test
  void chat_streaming_tool_test() throws Exception {

    WireMockServer wireMock = new WireMockServer();
    configureFor("localhost", 8080);

    wireMock.start();

    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=2025-04-23")
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
          "time_limit" : 10000
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


    when(mockAuthenticationProvider.getTokenAsync()).thenReturn(completedFuture("my-super-token"));


    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(HttpClient.newBuilder().executor(Executors.newSingleThreadExecutor()).build())
      .modelId("ibm/granite-3-3-8b-instruct")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url("http://localhost:8080")
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
    assertEquals(0, response.getChoices().get(0).index());
    assertEquals("tool_calls", response.getChoices().get(0).finishReason());
    assertNotNull(response.getChoices().get(0).message());
    assertNull(response.getChoices().get(0).message().content());
    assertNull(response.getChoices().get(0).message().refusal());
    assertEquals("assistant", response.getChoices().get(0).message().role());
    assertEquals(2, response.getChoices().get(0).message().toolCalls().size());
    assertEquals("chatcmpl-tool-af37032523934f019aa7258469580a7a",
      response.getChoices().get(0).message().toolCalls().get(0).id());
    assertEquals(0, response.getChoices().get(0).message().toolCalls().get(0).index());
    assertEquals("function", response.getChoices().get(0).message().toolCalls().get(0).type());
    assertEquals("sum", response.getChoices().get(0).message().toolCalls().get(0).function().name());
    assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
      response.getChoices().get(0).message().toolCalls().get(0).function().arguments());
    assertEquals("chatcmpl-tool-f762db03c60f441dba57bab09552bb7b",
      response.getChoices().get(0).message().toolCalls().get(1).id());
    assertEquals(1, response.getChoices().get(0).message().toolCalls().get(1).index());
    assertEquals("function", response.getChoices().get(0).message().toolCalls().get(1).type());
    assertEquals("subtraction", response.getChoices().get(0).message().toolCalls().get(1).function().name());
    assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
      response.getChoices().get(0).message().toolCalls().get(1).function().arguments());
    
    wireMock.stop();
  }

  @Test
  void chat_streaming_tool_choice_required_test() throws Exception {

    WireMockServer wireMock = new WireMockServer();
    configureFor("localhost", 8080);

    wireMock.start();

    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=2025-04-23")
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
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
            "time_limit": 10000
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


    when(mockAuthenticationProvider.getTokenAsync()).thenReturn(completedFuture("my-super-token"));

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(HttpClient.newBuilder().executor(Executors.newSingleThreadExecutor()).build())
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url("http://localhost:8080")
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

    var chatParameters = ChatParameters.builder().toolChoiceOption(ToolChoice.REQUIRED).build();

    CompletableFuture<ChatResponse> result = new CompletableFuture<>();
    chatService.chatStreaming(messages, tools, chatParameters, new ChatHandler() {

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
    assertEquals(0, response.getChoices().get(0).index());
    assertEquals("tool_calls", response.getChoices().get(0).finishReason());
    assertNotNull(response.getChoices().get(0).message());
    assertNull(response.getChoices().get(0).message().content());
    assertNull(response.getChoices().get(0).message().refusal());
    assertEquals("assistant", response.getChoices().get(0).message().role());
    assertEquals(2, response.getChoices().get(0).message().toolCalls().size());
    // If tool_choice_option is "required" watsonx doesn't return the id, so it will be autogenerated.
    assertNotNull(response.getChoices().get(0).message().toolCalls().get(0).id());
    assertEquals(0, response.getChoices().get(0).message().toolCalls().get(0).index());
    assertEquals("function", response.getChoices().get(0).message().toolCalls().get(0).type());
    assertEquals("sum", response.getChoices().get(0).message().toolCalls().get(0).function().name());
    assertEquals("{\"firstNumber\": 2, \"secondNumber\": 2}",
      response.getChoices().get(0).message().toolCalls().get(0).function().arguments());
    // If tool_choice_option is "required" watsonx doesn't return the id, so it will be autogenerated.
    assertNotNull(response.getChoices().get(0).message().toolCalls().get(1).id());
    assertEquals(1, response.getChoices().get(0).message().toolCalls().get(1).index());
    assertEquals("function", response.getChoices().get(0).message().toolCalls().get(1).type());
    assertEquals("subtraction", response.getChoices().get(0).message().toolCalls().get(1).function().name());
    assertEquals("{\"firstNumber\": 4, \"secondNumber\": 2}",
      response.getChoices().get(0).message().toolCalls().get(1).function().arguments());

    wireMock.stop();
  }

  @Test
  void chat_streaming_on_error() throws Exception {

    WireMockServer wireMock = new WireMockServer();
    configureFor("localhost", 8080);

    wireMock.start();

    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=2025-04-23")
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

    when(mockAuthenticationProvider.getTokenAsync()).thenReturn(completedFuture("my-super-token"));

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url("http://localhost:8080")
      .build();

    AtomicInteger counter = new AtomicInteger();
    CompletableFuture<ChatResponse> result = new CompletableFuture<>();
    chatService.chatStreaming(List.of(UserMessage.text("Hello")), new ChatHandler() {

      @Override
      public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
        assertNotNull(partialResponse);
        counter.incrementAndGet();
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

    assertThrows(ExecutionException.class, () -> result.get(3, TimeUnit.SECONDS));
    assertEquals(1, counter.get());

    wireMock.stop();
  }

  @Test
  void chat_streaming_on_complete_exception() throws Exception {

    WireMockServer wireMock = new WireMockServer();
    configureFor("localhost", 8080);

    wireMock.start();

    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=2025-04-23")
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          """
            id: 1
            event: message
            data: {"id":"chatcmpl-5d8c131decbb6978cba5df10267aa3ff","object":"chat.completion.chunk","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8", "model_id":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","model":"meta-llama/llama-4-maverick-17b-128e-instruct-fp8","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1749736055,"model_version":"4.0.0","created_at":"2025-06-12T13:47:35.541Z","system":{"warnings":[{"message":"This model is a Non-IBM Product governed by a third-party license that may impose use restrictions and other obligations. By using this model you agree to its terms as identified in the following URL.","id":"disclaimer_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx"}]}}
            """)));

    when(mockAuthenticationProvider.getTokenAsync()).thenReturn(completedFuture("my-super-token"));

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
      .url("http://localhost:8080")
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
    });

    var ex = assertThrows(ExecutionException.class, () -> result.get(3, TimeUnit.SECONDS));
    assertEquals("Error in onComplete handler", ex.getCause().getMessage());

    wireMock.stop();
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
    };

    var messages = List.<ChatMessage>of(UserMessage.text("test"));

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

    var chatService = ChatService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .url("http://localhost:8080")
      .build();

    ex = assertThrows(NullPointerException.class, () -> chatService.chat(messages));
    assertEquals("The modelId must be provided", ex.getMessage());
    ex = assertThrows(NullPointerException.class, () -> chatService.chatStreaming(messages, chatHandler));
    assertEquals("The modelId must be provided", ex.getMessage());

    var chatParameters = ChatParameters.builder()
      .modelId("test")
      .build();

    ex = assertThrows(NullPointerException.class, () -> chatService.chat(messages, chatParameters));
    assertEquals("Either projectId or spaceId must be provided", ex.getMessage());
    ex = assertThrows(NullPointerException.class, () -> chatService.chatStreaming(messages, chatParameters, chatHandler));
    assertEquals("Either projectId or spaceId must be provided", ex.getMessage());

    var ex2 = assertThrows(IllegalArgumentException.class, () -> chatService.chat(null, chatParameters));
    assertEquals("The list of messages can not be null or empty", ex2.getMessage());
    ex2 = assertThrows(IllegalArgumentException.class, () -> chatService.chatStreaming(null, chatParameters, chatHandler));
    assertEquals("The list of messages can not be null or empty", ex2.getMessage());
  }
}
