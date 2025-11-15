/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static java.util.Objects.isNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.Thinking;
import com.ibm.watsonx.ai.chat.model.ThinkingEffort;
import com.ibm.watsonx.ai.chat.model.UserMessage;

@ExtendWith(MockitoExtension.class)
public class ChatServiceThinkingTest extends AbstractWatsonxTest {

    @Test
    void should_throw_exception_when_thinking_without_extraction_tags() {

        withWatsonxServiceMock(() -> {

            var chatService = ChatService.builder()
                .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("model-id")
                .projectId("project-id")
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .messages(
                    ControlMessage.of("thinking"),
                    UserMessage.text("Why the sky is blue?"))
                .build();

            assertThrows(
                IllegalArgumentException.class,
                () -> chatService.chat(chatRequest),
                "Extraction tags are required when using control messages");

            assertThrows(
                IllegalArgumentException.class,
                () -> chatService.chatStreaming(chatRequest, mock(ChatHandler.class)),
                "Extraction tags are required when using control messages");
        });
    }

    @Nested
    class Chat {

        @Test
        void should_extract_content_and_thinking_with_extraction_tags() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("granite_thinking_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                     {
                        "model_id": "ibm/granite-3-3-8b-instruct",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "Test"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.token()).thenReturn("my-token");

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(ExtractionTags.of("think", "response"))
                .messages(UserMessage.text("Test"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Result", chatResponse.extractContent());
            assertEquals("Think", chatResponse.extractThinking());
            assertEquals("Result", assistantMessage.content());
            assertEquals("Think", assistantMessage.thinking());

            chatRequest = ChatRequest.builder()
                .thinking(Thinking.of(ExtractionTags.of("think", "response")))
                .messages(UserMessage.text("Test"))
                .build();

            chatResponse = chatService.chat(chatRequest);
            assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Result", chatResponse.extractContent());
            assertEquals("Think", chatResponse.extractThinking());
            assertEquals("Result", assistantMessage.content());
            assertEquals("Think", assistantMessage.thinking());
        }

        @Test
        void should_extract_thinking_without_configuration() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "I'm an Aries. Can you tell me my horoscope?"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.token()).thenReturn("my-token");

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(true)
                .messages(UserMessage.text("I'm an Aries. Can you tell me my horoscope?"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Aries Daily Horoscope", chatResponse.extractContent());
            assertEquals("Need to give horoscope for Aries.", chatResponse.extractThinking());
            assertEquals("Aries Daily Horoscope", assistantMessage.content());
            assertEquals("Need to give horoscope for Aries.", assistantMessage.thinking());
        }

        @Test
        void should_handle_various_thinking_efforts_correctly() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_response.txt").readAllBytes());

            Stream.of(ThinkingEffort.LOW, ThinkingEffort.MEDIUM, ThinkingEffort.HIGH)
                .forEach(thinkingEffort -> {
                    wireMock.stubFor(post("/ml/v1/text/chat?version=%s".formatted(API_VERSION))
                        .withHeader("Authorization", equalTo("Bearer my-token"))
                        .withRequestBody(equalToJson("""
                            {
                                "model_id": "openai/gpt-oss-120b",
                                "project_id": "project-id",
                                "messages": [
                                    {
                                        "role": "user",
                                        "content": [
                                            {
                                                "type": "text",
                                                "text": "I'm an Aries. Can you tell me my horoscope?"
                                            }
                                        ]
                                    }
                                ],
                                "time_limit": 60000,
                                "chat_template_kwargs" : {
                                    "thinking": true
                                },
                                "reasoning_effort": "%s"
                            }""".formatted(thinkingEffort.getValue())))
                        .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(BODY)));

                    when(mockAuthenticationProvider.token()).thenReturn("my-token");

                    var chatService = ChatService.builder()
                        .authenticationProvider(mockAuthenticationProvider)
                        .modelId("openai/gpt-oss-120b")
                        .projectId("project-id")
                        .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                        .build();

                    var chatRequest = ChatRequest.builder()
                        .thinking(thinkingEffort)
                        .messages(UserMessage.text("I'm an Aries. Can you tell me my horoscope?"))
                        .build();

                    var chatResponse = chatService.chat(chatRequest);
                    var assistantMessage = chatResponse.toAssistantMessage();
                    assertEquals("Aries Daily Horoscope", chatResponse.extractContent());
                    assertEquals("Need to give horoscope for Aries.", chatResponse.extractThinking());
                    assertEquals("Aries Daily Horoscope", assistantMessage.content());
                    assertEquals("Need to give horoscope for Aries.", assistantMessage.thinking());
                });
        }

        @Test
        void should_not_extract_thinking_when_disabled() throws Exception {

            wireMock.stubFor(post("/ml/v1/text/chat?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "I'm an Aries. Can you tell me my horoscope?"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": false
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("""
                        {
                            "id": "chatcmpl-b41f77704b96499dbafed2aed7374bf0",
                            "object": "chat.completion",
                            "model_id": "openai/gpt-oss-120b-curated",
                            "model": "openai/gpt-oss-120b-curated",
                            "choices": [
                                {
                                    "index": 0,
                                    "message": {
                                        "role": "assistant",
                                        "content": "Aries Daily Horoscope"
                                    },
                                    "finish_reason": "stop"
                                }
                            ],
                            "created": 1760258491,
                            "created_at": "2025-10-12T08:41:34.366Z",
                            "usage": {
                                "completion_tokens": 522,
                                "prompt_tokens": 89,
                                "total_tokens": 611
                            }
                        }""")));

            when(mockAuthenticationProvider.token()).thenReturn("my-token");

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(false)
                .messages(UserMessage.text("I'm an Aries. Can you tell me my horoscope?"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Aries Daily Horoscope", chatResponse.extractContent());
            assertNull(chatResponse.extractThinking());
            assertEquals("Aries Daily Horoscope", assistantMessage.content());
            assertNull(assistantMessage.thinking());
        }

        @Test
        void should_extract_thinking_with_builder() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "I'm an Aries. Can you tell me my horoscope?"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        },
                        "include_reasoning": true,
                        "reasoning_effort": "low"
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.token()).thenReturn("my-token");

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(
                    Thinking.builder()
                        .thinkingEffort(ThinkingEffort.LOW)
                        .includeReasoning(true)
                        .build()
                )
                .messages(UserMessage.text("I'm an Aries. Can you tell me my horoscope?"))
                .build();

            var chatResponse = chatService.chat(chatRequest);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("Aries Daily Horoscope", chatResponse.extractContent());
            assertEquals("Need to give horoscope for Aries.", chatResponse.extractThinking());
            assertEquals("Aries Daily Horoscope", assistantMessage.content());
            assertEquals("Need to give horoscope for Aries.", assistantMessage.thinking());
        }
    }

    @Nested
    class ChatStreaming {

        @Test
        void should_stream_partial_thinking_and_content_correctly() throws Exception {

            var httpPort = wireMock.getPort();
            String BODY = new String(ClassLoader.getSystemResourceAsStream("granite_thinking_streaming_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "ibm/granite-3-3-8b-instruct",
                        "project_id": "project-id",
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
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withChunkedDribbleDelay(159, 200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
                .build();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.text("Translate \"Hello\" in Italian"))
                .thinking(ExtractionTags.of("think", "response"))
                .build();

            StringBuilder thinkingResponse = new StringBuilder();
            StringBuilder response = new StringBuilder();

            chatService.chatStreaming(chatRequest, new ChatHandler() {

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
            });

            var chatResponse = result.get(3, TimeUnit.SECONDS);

            var EXEPECTED_THINKING =
                "The translation of \"Hello\" in Italian is straightforward. \"Hello\" in English directly translates to \"Ciao\" in Italian, which is a common informal greeting. For a more formal context, \"Buongiorno\" can be used, meaning \"Good day.\" However, since the request is for a direct translation of \"Hello,\" \"Ciao\" is the most appropriate response.";

            var EXPECTED_RESPONSE =
                "This is the informal equivalent, widely used in everyday conversation. For a formal greeting, one would say \"Buongiorno,\" but given the direct translation request, \"Ciao\" is the most fitting response.";

            var chatResponseText = chatResponse.getChoices().get(0).getMessage().content();
            assertTrue(chatResponseText.contains("<think>") && chatResponseText.contains("</think>"));
            assertTrue(chatResponseText.contains("<response>") && chatResponseText.contains("</response>"));
            assertTrue(chatResponseText.contains(EXPECTED_RESPONSE));


            assertTrue(chatResponse.extractContent().contains(EXPECTED_RESPONSE));
            assertFalse(chatResponse.extractContent().contains("<response>") && chatResponse.extractContent().contains("</response>"));

            assertTrue(chatResponse.extractThinking().contains(EXEPECTED_THINKING));
            assertFalse(chatResponse.extractThinking().contains("<think>") && chatResponse.extractThinking().contains("</think>"));

            assertEquals(
                EXEPECTED_THINKING,
                thinkingResponse.toString()
            );

            assertTrue(response.toString().contains(EXPECTED_RESPONSE));

            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content().contains(EXPECTED_RESPONSE));
            assertFalse(assistantMessage.content().contains(EXEPECTED_THINKING));
            assertFalse(assistantMessage.content().contains("<response>") && assistantMessage.content().contains("</response>"));

            assertFalse(assistantMessage.thinking().contains(EXPECTED_RESPONSE));
            assertTrue(assistantMessage.thinking().contains(EXEPECTED_THINKING));
            assertFalse(assistantMessage.thinking().contains("<think>") && assistantMessage.thinking().contains("</think>"));
        }

        @Test
        void should_handle_streaming_without_thinking_result() throws Exception {

            wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "ibm/granite-3-3-8b-instruct",
                        "project_id": "project-id",
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
                        "time_limit": 60000,
                         "chat_template_kwargs" : {
                            "thinking": true
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withChunkedDribbleDelay(159, 200)
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
                .modelId("ibm/granite-3-3-8b-instruct")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.text("Translate \"Hello\" in Italian"))
                .thinking(Thinking.of(ExtractionTags.of("think", "response")))
                .build();

            StringBuilder thinkingResponse = new StringBuilder();
            StringBuilder response = new StringBuilder();

            chatService.chatStreaming(chatRequest, new ChatHandler() {

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
            });

            var chatResponse = result.get(3, TimeUnit.SECONDS);

            var EXPECTED_RESPONSE = "Ciao";

            var chatResponseText = chatResponse.getChoices().get(0).getMessage().content();
            assertFalse(chatResponseText.contains("<think>") && chatResponseText.contains("</think>"));
            assertFalse(chatResponseText.contains("<response>") && chatResponseText.contains("</response>"));
            assertTrue(chatResponseText.contains(EXPECTED_RESPONSE));


            assertTrue(chatResponse.extractContent().contains(EXPECTED_RESPONSE));
            assertFalse(chatResponse.extractContent().contains("<response>") && chatResponse.extractContent().contains("</response>"));

            assertNull(chatResponse.extractThinking());
            assertTrue(response.toString().contains(EXPECTED_RESPONSE));

            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content().contains(EXPECTED_RESPONSE));
            assertFalse(assistantMessage.content().contains("<response>") && assistantMessage.content().contains("</response>"));

            assertNull(assistantMessage.thinking());
        }

        @Test
        void should_extract_thinking_without_configuration() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_streaming_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "Translate 'Hello' to Italian"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        }
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-token"));

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(true)
                .messages(UserMessage.text("Translate 'Hello' to Italian"))
                .build();

            StringBuilder thinkingResponse = new StringBuilder();
            StringBuilder response = new StringBuilder();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            chatService.chatStreaming(chatRequest, new ChatHandler() {

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
            });

            var chatResponse = result.get(3, TimeUnit.SECONDS);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", chatResponse.extractContent());
            assertEquals("User wants translation.", chatResponse.extractThinking());
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", assistantMessage.content());
            assertEquals("User wants translation.", assistantMessage.thinking());
            assertEquals(chatResponse.extractThinking(), thinkingResponse.toString());
            assertEquals(chatResponse.extractContent(), response.toString());
        }

        @Test
        void should_handle_thinking_efforts() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_streaming_response.txt").readAllBytes());

            Stream.of(ThinkingEffort.LOW, ThinkingEffort.MEDIUM, ThinkingEffort.HIGH)
                .forEach(thinkingEffort -> {
                    wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                        .withHeader("Authorization", equalTo("Bearer my-token"))
                        .withRequestBody(equalToJson("""
                            {
                                "model_id": "openai/gpt-oss-120b",
                                "project_id": "project-id",
                                "messages": [
                                    {
                                        "role": "user",
                                        "content": [
                                            {
                                                "type": "text",
                                                "text": "Translate 'Hello' to Italian"
                                            }
                                        ]
                                    }
                                ],
                                "time_limit": 60000,
                                "chat_template_kwargs" : {
                                    "thinking": true
                                },
                                "reasoning_effort": "%s"
                            }""".formatted(thinkingEffort.getValue())))
                        .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(BODY)));

                    when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-token"));

                    var chatService = ChatService.builder()
                        .authenticationProvider(mockAuthenticationProvider)
                        .modelId("openai/gpt-oss-120b")
                        .projectId("project-id")
                        .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                        .build();

                    var chatRequest = ChatRequest.builder()
                        .thinking(thinkingEffort)
                        .messages(UserMessage.text("Translate 'Hello' to Italian"))
                        .build();

                    StringBuilder thinkingResponse = new StringBuilder();
                    StringBuilder response = new StringBuilder();

                    CompletableFuture<ChatResponse> result = new CompletableFuture<>();
                    chatService.chatStreaming(chatRequest, new ChatHandler() {

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
                    });

                    var chatResponse = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
                    var assistantMessage = chatResponse.toAssistantMessage();
                    assertEquals("\"Hello\" in Italian is **\"ciao\"**.", chatResponse.extractContent());
                    assertEquals("User wants translation.", chatResponse.extractThinking());
                    assertEquals("\"Hello\" in Italian is **\"ciao\"**.", assistantMessage.content());
                    assertEquals("User wants translation.", assistantMessage.thinking());
                    assertEquals(chatResponse.extractThinking(), thinkingResponse.toString());
                    assertEquals(chatResponse.extractContent(), response.toString());
                });
        }

        @Test
        void should_not_extract_thinking_when_disabled() throws Exception {

            wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "Translate 'Hello' to Italian"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(
                        """
                            id: 1
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.055Z"}

                            id: 2
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.119Z"}

                            id: 3
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"\\""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.124Z"}

                            id: 4
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"Hello"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.129Z"}

                            id: 5
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"\\""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.134Z"}

                            id: 6
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":" in"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.140Z"}

                            id: 7
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":" Italian"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.145Z"}

                            id: 8
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":" is"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.151Z"}

                            id: 9
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":" **"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.156Z"}

                            id: 10
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"\\""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.161Z"}

                            id: 11
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"cia"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.166Z"}

                            id: 12
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"o"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.172Z"}

                            id: 13
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"\\""}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.177Z"}

                            id: 14
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"**"}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.182Z"}

                            id: 15
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[{"index":0,"finish_reason":null,"delta":{"content":"."}}],"created":1760258822,"created_at":"2025-10-12T08:47:02.188Z"}

                            id: 16
                            event: message
                            data: {"id":"chatcmpl-3956a62a1e0446b0a1f4115152baf489","object":"chat.completion.chunk","model_id":"openai/gpt-oss-120b-curated","model":"openai/gpt-oss-120b-curated","choices":[],"created":1760258822,"created_at":"2025-10-12T08:47:02.194Z","usage":{"completion_tokens":27,"prompt_tokens":85,"total_tokens":112}}
                            """)));

            when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-token"));

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .messages(UserMessage.text("Translate 'Hello' to Italian"))
                .build();

            StringBuilder thinkingResponse = new StringBuilder();
            StringBuilder response = new StringBuilder();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            chatService.chatStreaming(chatRequest, new ChatHandler() {

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
            });

            var chatResponse = assertDoesNotThrow(() -> result.get(3, TimeUnit.SECONDS));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", chatResponse.extractContent());
            assertTrue(isNull(chatResponse.extractThinking()) || chatResponse.extractThinking().isBlank());
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", assistantMessage.content());
            assertTrue(isNull(assistantMessage.thinking()) || assistantMessage.thinking().isBlank());
            assertTrue(isNull(thinkingResponse.toString()) || thinkingResponse.toString().isBlank());
            assertEquals(chatResponse.extractContent(), response.toString());
        }

        @Test
        void should_extract_thinking_with_builder() throws Exception {

            String BODY = new String(ClassLoader.getSystemResourceAsStream("gpt_oss_thinking_streaming_response.txt").readAllBytes());

            wireMock.stubFor(post("/ml/v1/text/chat_stream?version=%s".formatted(API_VERSION))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .withRequestBody(equalToJson("""
                    {
                        "model_id": "openai/gpt-oss-120b",
                        "project_id": "project-id",
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "Translate 'Hello' to Italian"
                                    }
                                ]
                            }
                        ],
                        "time_limit": 60000,
                        "chat_template_kwargs" : {
                            "thinking": true
                        },
                        "include_reasoning": true,
                        "reasoning_effort": "low"
                    }"""))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(BODY)));

            when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-token"));

            var chatService = ChatService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .modelId("openai/gpt-oss-120b")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            var chatRequest = ChatRequest.builder()
                .thinking(
                    Thinking.builder()
                        .thinkingEffort(ThinkingEffort.LOW)
                        .includeReasoning(true)
                        .build()
                )
                .messages(UserMessage.text("Translate 'Hello' to Italian"))
                .build();

            StringBuilder thinkingResponse = new StringBuilder();
            StringBuilder response = new StringBuilder();

            CompletableFuture<ChatResponse> result = new CompletableFuture<>();
            chatService.chatStreaming(chatRequest, new ChatHandler() {

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
            });

            var chatResponse = result.get(3, TimeUnit.SECONDS);
            var assistantMessage = chatResponse.toAssistantMessage();
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", chatResponse.extractContent());
            assertEquals("User wants translation.", chatResponse.extractThinking());
            assertEquals("\"Hello\" in Italian is **\"ciao\"**.", assistantMessage.content());
            assertEquals("User wants translation.", assistantMessage.thinking());
            assertEquals(chatResponse.extractThinking(), thinkingResponse.toString());
            assertEquals(chatResponse.extractContent(), response.toString());
        }
    }
}
