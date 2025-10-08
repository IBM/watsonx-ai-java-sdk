/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.JsonSchema;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_DEPLOYMENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class DeploymentServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String DEPLOYMENT_ID = System.getenv("WATSONX_DEPLOYMENT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final AuthenticationProvider authentication = IAMAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    @Test
    @EnabledIfEnvironmentVariable(named = "WATSONX_SPACE_ID", matches = ".+")
    void test_find_by_id() {

        String SPACE_ID = System.getenv("WATSONX_SPACE_ID");

        var deploymentService = DeploymentService.builder()
            .baseUrl(URL)
            .authenticationProvider(authentication)
            .logRequests(true)
            .logResponses(true)
            .build();

        var response = deploymentService.findById(FindByIdRequest.builder()
            .deploymentId(DEPLOYMENT_ID)
            .spaceId(SPACE_ID)
            .build());

        assertNotNull(response);
    }

    @Nested
    class Chat {

        @Test
        void test_chat() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Hello!"))
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var text = chatResponse.extractContent();

            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());
        }

        @Test
        void test_chat_messages() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(
                    SystemMessage.of("You are an helpful assistant"),
                    UserMessage.text("Hello, my name is Andrea"),
                    AssistantMessage.text("Hello Andrea, nice to meet you"),
                    UserMessage.text("What is my name?")
                )
                .parameters(
                    ChatParameters.builder()
                        .temperature(0.0)
                        .build()
                ).build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var text = chatResponse.extractContent();

            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());
            assertTrue(text.contains("Andrea"));
        }

        @Test
        void test_chat_json() {

            record Poem(String content, String topic) {}

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .withJsonResponse()
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("""
                    Create a poem about dog, max 3 lines
                    Answer using the following json structure:
                    {
                        "content": <poem content>
                        "topic": <poem topic>
                    }"""))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var poem = chatResponse.toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        void test_chat_json_schema() {

            record Poem(String content, String topic) {}

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .withJsonSchemaResponse(
                    JsonSchema.builder()
                        .addStringProperty("content")
                        .addEnumProperty("topic", "dog", "cat")
                        .required("content", "topic")
                        .build())
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Create a poem about dog, max 3 lines"))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var poem = chatResponse.toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        @Disabled
        void test_chat_thinking() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(
                    ControlMessage.of("thinking"),
                    UserMessage.text("Why the sky is blue?"))
                .thinking(ExtractionTags.of("think", "response"))
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var text = chatResponse.getChoices().get(0).getMessage().content();

            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());
            assertTrue(text.contains("<think>") && text.contains("</think>"));
            assertTrue(text.contains("<response>") && text.contains("</response>"));

            var thinkingMessage = chatResponse.extractThinking();
            assertNotNull(thinkingMessage);
            assertFalse(thinkingMessage.isBlank());

            var contentMessage = chatResponse.extractContent();
            assertNotNull(contentMessage);
            assertFalse(contentMessage.isBlank());

            var assistantMessage = chatResponse.toAssistantMessage();
            assertFalse(assistantMessage.content().contains("<think>") && assistantMessage.content().contains("</think>"));
            assertFalse(assistantMessage.content().contains("<response>") && assistantMessage.content().contains("</response>"));
        }

        @Test
        @Disabled("deployment doesn't allow image function")
        void test_chat_image() throws Exception {

            var image = getClass().getClassLoader().getResource("alien.jpg");

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.of(
                    TextContent.of("Give a short description of the image"),
                    ImageContent.from(Paths.get(image.toURI()))
                )).build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            var text = chatResponse.extractContent();
            assertNotNull(text);
            assertFalse(text.isBlank());
        }

        @Test
        @Disabled
        void test_chat_tool() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Send an email to a@a.it with subject \"Test\" and body \"Hello\""))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.builder()
                        .addStringProperty("to")
                        .addStringProperty("subject")
                        .addStringProperty("body")
                        .required("to", "body")))
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(chatRequest));
            assertNotNull(chatResponse);
            var tools = chatResponse.toAssistantMessage().toolCalls();
            assertNotNull(tools);
            assertEquals(1, tools.size());
            assertNotNull(tools.get(0).id());
            assertEquals("send_email", tools.get(0).function().name());
            JSONAssert.assertEquals(
                "{\"to\": \"a@a.it\", \"subject\": \"Test\", \"body\": \"Hello\"}",
                tools.get(0).function().arguments(),
                true);
        }

        @Test
        @Disabled
        void test_chat_tool_choice_option() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatParameters parameters = ChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .build();

            ChatRequest request = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Hello!"))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.builder()
                        .addStringProperty("to")
                        .addStringProperty("subject")
                        .addStringProperty("body")
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> deploymentService.chat(request));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content() == null || assistantMessage.content().isBlank());
            assertNotNull(assistantMessage.toolCalls());
            assertEquals(1, assistantMessage.toolCalls().size());
        }
    }

    @Nested
    class ChatStreaming {
        @Test
        void test_chat_streaming() throws Exception {

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var futures = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    chatService.chatStreaming(createChatRequest(), createChatHandler(future));
                    return future;
                }).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(10, TimeUnit.SECONDS);

            for (Future<String> future : futures) {
                var result = assertDoesNotThrow(() -> future.get());
                assertEquals("0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20", result);
            }
        }

        @Test
        void test_chat_streaming_json() {

            record Poem(String content, String topic) {}

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .withJsonResponse()
                .build();

            ChatRequest request = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("""
                    Create a poem about dog, max 3 lines
                    Answer using the following json structure:
                    {
                        "content": <poem content>
                        "topic": <poem topic>
                    }"""))
                .parameters(parameters)
                .build();

            CompletableFuture<ChatResponse> future = new CompletableFuture<>();
            chatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var chatResponse = assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            var poem = chatResponse.toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        void test_chat_streaming_json_schema() {

            record Poem(String content, String topic) {}

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .withJsonSchemaResponse(
                    JsonSchema.builder()
                        .addStringProperty("content")
                        .addEnumProperty("topic", "dog", "cat")
                        .required("content", "topic")
                        .build())
                .build();

            ChatRequest request = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Create a poem about dog, max 3 lines"))
                .parameters(parameters)
                .build();

            CompletableFuture<ChatResponse> future = new CompletableFuture<>();
            chatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var chatResponse = assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            var poem = chatResponse.toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        @Disabled
        void test_chat_streaming_thinking() {

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .maxCompletionTokens(0)
                .build();

            ChatRequest request = ChatRequest.builder()
                .messages(
                    ControlMessage.of("thinking"),
                    UserMessage.text("Why the sky is blue?"))
                .parameters(parameters)
                .thinking(ExtractionTags.of("think", "response"))
                .build();

            CompletableFuture<String> futureThinking = new CompletableFuture<>();
            CompletableFuture<String> futureContent = new CompletableFuture<>();
            CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
            CompletableFuture<Throwable> futureError = new CompletableFuture<>();
            chatService.chatStreaming(request, new ChatHandler() {
                private StringBuilder thinking = new StringBuilder();
                private StringBuilder response = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    response.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    futureChatResponse.complete(completeResponse);
                    futureThinking.complete(thinking.toString());
                    futureContent.complete(response.toString());
                }

                @Override
                public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
                    thinking.append(partialThinking);
                }

                @Override
                public void onError(Throwable error) {
                    futureError.completeExceptionally(error);
                }

            });

            var chatResponse = assertDoesNotThrow(() -> futureChatResponse.get(10, TimeUnit.SECONDS));
            var text = chatResponse.getChoices().get(0).getMessage().content();
            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());
            assertTrue(text.contains("<think>") && text.contains("</think>"));
            assertTrue(text.contains("<response>") && text.contains("</response>"));

            var thinkingMessage = chatResponse.extractThinking();
            assertNotNull(thinkingMessage);
            assertFalse(thinkingMessage.isBlank());

            var contentMessage = chatResponse.extractContent();
            assertNotNull(contentMessage);
            assertFalse(contentMessage.isBlank());

            var thinking = assertDoesNotThrow(() -> futureThinking.get(3, TimeUnit.SECONDS));
            assertEquals(thinkingMessage, thinking);

            var content = assertDoesNotThrow(() -> futureContent.get(3, TimeUnit.SECONDS));
            assertEquals(contentMessage, content);
        }

        @Test
        @Disabled
        void test_chat_streaming_image() throws Exception {

            var image = getClass().getClassLoader().getResource("alien.jpg");

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .timeLimit(Duration.ofSeconds(30))
                .build();

            ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.of(
                    TextContent.of("Give a short description of the image"),
                    ImageContent.from(Paths.get(image.toURI()))
                ))
                .parameters(parameters)
                .build();

            CompletableFuture<String> partialResponseFuture = new CompletableFuture<>();
            CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();
            chatService.chatStreaming(request, new ChatHandler() {
                StringBuilder builder = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    builder.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    chatResponseFuture.complete(completeResponse);
                    partialResponseFuture.complete(builder.toString());
                }

                @Override
                public void onError(Throwable error) {}

            });

            var chatResponse = assertDoesNotThrow(() -> chatResponseFuture.get(10, TimeUnit.SECONDS));
            var partialResponse = assertDoesNotThrow(() -> partialResponseFuture.get(10, TimeUnit.SECONDS));
            assertNotNull(chatResponse.extractContent());
            assertFalse(chatResponse.extractContent().isBlank());
            assertNotNull(partialResponse);
            assertFalse(partialResponse.isBlank());
            assertEquals(chatResponse.extractContent(), partialResponse);
        }

        @Test
        @Disabled
        void test_chat_streaming_tool() {

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.text("Send an email to a@a.it with subject \"Test\" and body \"Hello\""))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.builder()
                        .addStringProperty("to")
                        .addStringProperty("subject")
                        .addStringProperty("body")
                        .required("to", "body")))
                .build();

            CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();
            CompletableFuture<CompletedToolCall> toolCallFuture = new CompletableFuture<>();
            CompletableFuture<ToolCall> fromPartialToolCallFuture = new CompletableFuture<>();
            CompletableFuture<Throwable> throwableFuture = new CompletableFuture<>();
            chatService.chatStreaming(request, new ChatHandler() {
                Map<String, String> cachePartialToolCall = new HashMap<>();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    throwableFuture.completeExceptionally(new RuntimeException("Unexpected partial response"));
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    chatResponseFuture.complete(completeResponse);
                    fromPartialToolCallFuture.complete(new ToolCall(
                        Integer.parseInt(cachePartialToolCall.get("index")),
                        cachePartialToolCall.get("id"),
                        "",
                        new FunctionCall(cachePartialToolCall.get("name"), cachePartialToolCall.get("arguments"))));
                }

                @Override
                public void onError(Throwable error) {
                    throwableFuture.completeExceptionally(new RuntimeException("Unexpected error"));
                }

                @Override
                public void onPartialToolCall(PartialToolCall partialToolCall) {
                    cachePartialToolCall.computeIfAbsent("id", k -> partialToolCall.id());
                    cachePartialToolCall.computeIfAbsent("name", k -> partialToolCall.name());
                    cachePartialToolCall.computeIfAbsent("completionId", k -> partialToolCall.completionId());
                    cachePartialToolCall.computeIfAbsent("index", k -> partialToolCall.index() + "");
                    if (cachePartialToolCall.containsKey("arguments")) {
                        var arguments = cachePartialToolCall.get("arguments") + partialToolCall.arguments();
                        cachePartialToolCall.put("arguments", arguments);
                    } else {
                        if (nonNull(partialToolCall.arguments()))
                            cachePartialToolCall.put("arguments", partialToolCall.arguments());
                    }
                }

                @Override
                public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                    toolCallFuture.complete(completeToolCall);
                }
            });

            var chatResponse = assertDoesNotThrow(() -> chatResponseFuture.get(3, TimeUnit.SECONDS));
            var toolCall = assertDoesNotThrow(() -> toolCallFuture.get(3, TimeUnit.SECONDS));
            var fromPartialTool = assertDoesNotThrow(() -> fromPartialToolCallFuture.get(3, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> throwableFuture.get(1, TimeUnit.SECONDS));
            assertEquals(toolCall.completionId(), chatResponse.getId());
            assertEquals(toolCall.toolCall(), fromPartialTool);
            assertEquals(toolCall.toolCall(), chatResponse.toAssistantMessage().toolCalls().get(0));
            assertNotNull(chatResponse.toAssistantMessage().toolCalls().get(0).id());
            assertEquals("send_email", chatResponse.toAssistantMessage().toolCalls().get(0).function().name());
            JSONAssert.assertEquals(
                "{\"to\": \"a@a.it\", \"subject\": \"Test\", \"body\": \"Hello\"}",
                chatResponse.toAssistantMessage().toolCalls().get(0).function().arguments(),
                true);
        }

        @Test
        void test_chat_streaming_shared_handler() throws Exception {

            var chatService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            Map<String, CompletableFuture<String>> results = new HashMap<>();
            Set<String> ids = new HashSet<>();
            CountDownLatch latch = new CountDownLatch(10);

            var sharedHandler = new ChatHandler() {
                Map<String, StringBuilder> cache = new HashMap<>();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    cache.computeIfAbsent(partialChatResponse.id(), k -> new StringBuilder()).append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    ids.add(completeResponse.getId());
                    var result = cache.remove(completeResponse.getId()).toString();
                    results.put(completeResponse.getId(), CompletableFuture.completedFuture(result));
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            };
            var futures = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> chatService.chatStreaming(createChatRequest(), sharedHandler))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(10, TimeUnit.SECONDS);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertEquals(10, ids.size());
            for (var future : results.values())
                assertEquals("0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20", future.get());
        }

        @Test
        @Disabled
        void test_chat_streaming_tool_choice_option() {

            var deploymentService = DeploymentService.builder()
                .baseUrl(URL)
                .authenticationProvider(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ChatParameters parameters = ChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .build();

            ChatRequest request = ChatRequest.builder()
                .deploymentId(DEPLOYMENT_ID)
                .messages(UserMessage.text("Hello!"))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.builder()
                        .addStringProperty("to")
                        .addStringProperty("subject")
                        .addStringProperty("body")
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            CompletableFuture<ChatResponse> future = new CompletableFuture<>();
            assertDoesNotThrow(() -> deploymentService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            }));

            var chatResponse = assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content() == null || assistantMessage.content().isBlank());
            assertNotNull(assistantMessage.toolCalls());
            assertEquals(1, assistantMessage.toolCalls().size());
        }

        private ChatHandler createChatHandler(CompletableFuture<String> future) {
            return new ChatHandler() {
                StringBuilder builder = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    builder.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete(builder.toString());
                }

                @Override
                public void onError(Throwable error) {
                    future.completeExceptionally(error);
                }
            };
        }

        private ChatRequest createChatRequest() {

            var parameters = ChatParameters.builder()
                .temperature(0.0)
                .build();

            return ChatRequest.builder()
                .messages(
                    SystemMessage.of("""
                        You are an helpful assistant, your task is return number starting from 0 to 20.
                        Return the number in the following format:

                        1, 2, 3, ...

                        Return only the list of number without any other text."""),
                    UserMessage.text("Count")
                )
                .deploymentId(DEPLOYMENT_ID)
                .parameters(parameters)
                .build();
        }
    }
}
