package com.ibm.watsonx.runtime.chat;

import static com.ibm.watsonx.runtime.utils.Utils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
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
import com.ibm.watsonx.runtime.chat.model.ChatRequest;
import com.ibm.watsonx.runtime.chat.model.ControlMessage;
import com.ibm.watsonx.runtime.chat.model.ImageContent;
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
    assertEquals(37, chatResponse.usage().getCompletionTokens());
    assertEquals(66, chatResponse.usage().getPromptTokens());
    assertEquals(103, chatResponse.usage().getTotalTokens());

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
    System.out.println(chatResponse.usage());
    assertEquals("ChatUsage [completionTokens=18, promptTokens=19, totalTokens=37]", chatResponse.usage().toString());
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

    var chatResponse = chatService.chat(messages, tools);
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
}
