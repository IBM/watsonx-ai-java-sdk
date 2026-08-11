/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.skyscreamer.jsonassert.JSONAssert;
import com.google.common.collect.Sets;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatResponse;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class ModelGatewayChatServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String URL = System.getenv("WATSONX_URL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    @Nested
    class Chat {

        @Test
        void should_return_valid_chat_response_when_chat_is_invoked() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .apiKey(API_KEY)
                .modelId("claude-sonnet-5")
                .logRequests(true)
                .logResponses(true)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat("Hello!"));
            var text = chatResponse.toAssistantMessage().content();

            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());

            assertNotNull(chatResponse.finishReason());
            assertNotNull(chatResponse.choices());
            assertNotNull(chatResponse.choices().get(0).finishReason());
            assertNotNull(chatResponse.choices().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message());
            assertNotNull(chatResponse.choices().get(0).message().content());
            assertNotNull(chatResponse.choices().get(0).message().role());
            assertNull(chatResponse.choices().get(0).message().refusal());
            assertNull(chatResponse.choices().get(0).message().toolCalls());
            assertNotNull(chatResponse.created());
            assertNotNull(chatResponse.id());
            assertNotNull(chatResponse.model());
            assertNotNull(chatResponse.object());
            assertNotNull(chatResponse.usage());
            assertNotNull(chatResponse.usage().completionTokens());
            assertNotNull(chatResponse.usage().promptTokens());
            assertNotNull(chatResponse.usage().totalTokens());
            assertNull(chatResponse.modelId());
            assertNull(chatResponse.modelVersion());
            assertNull(chatResponse.createdAt());
        }

        @Test
        void should_return_answer_containing_user_name_when_chat_messages_are_sent() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(
                    SystemMessage.of("You are an helpful assistant"),
                    UserMessage.text("Hello, my name is Andrea"),
                    AssistantMessage.text("Hello Andrea, nice to meet you"),
                    UserMessage.text("What is my name?")
                ).build();


            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var text = chatResponse.toAssistantMessage().content();

            assertNotNull(chatResponse);
            assertNotNull(text);
            assertFalse(text.isBlank());
            assertTrue(text.contains("Andrea"));
        }

        @Test
        void should_return_valid_poem_json_response() {

            record Poem(String content, String topic) {}

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .responseAsJson()
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("""
                    Create a poem about dog, max 3 lines
                    Answer using the following json structure:
                    {
                        "content": <poem content>
                        "topic": <poem topic>
                    }
                    Only return the JSON. Do not write anything else or add 'json'"""))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var poem = chatResponse.toAssistantMessage().toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        void should_return_valid_poem_json_schema() {

            record Poem(String content, String topic) {}

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .responseAsJsonSchema(
                    "poem",
                    JsonSchema.object()
                        .property("content", JsonSchema.string())
                        .property("topic", JsonSchema.enumeration("dog", "cat"))
                        .required("content", "topic")
                        .additionalProperties(false)
                        .build(),
                    true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Create a poem about dog, max 3 lines"))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var poem = chatResponse.toAssistantMessage().toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        void should_return_description_when_image_is_sent_in_chat() throws Exception {

            var image = getClass().getClassLoader().getResource("alien.jpg");

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .timeLimit(Duration.ofSeconds(30))
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.image(
                    "Give a short description of the image",
                    Paths.get(image.toURI())
                ))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var text = chatResponse.toAssistantMessage().content();
            assertNotNull(text);
            assertFalse(text.isBlank());
        }

        @Test
        void should_call_tool_and_return_valid_tool_response_when_chat_contains_tool_message() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Send an email to a@a.it with subject \"Test\" and body \"Hello\""))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
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

            assertNotNull(chatResponse.finishReason());
            assertNotNull(chatResponse.choices());
            assertNotNull(chatResponse.choices().get(0).finishReason());
            assertNotNull(chatResponse.choices().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message());
            assertNotNull(chatResponse.choices().get(0).message().content());
            assertNotNull(chatResponse.choices().get(0).message().role());
            assertNull(chatResponse.choices().get(0).message().refusal());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0));
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).id());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).type());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).function());
            assertNotNull(chatResponse.created());
            assertNotNull(chatResponse.id());
            assertNotNull(chatResponse.model());
            assertNotNull(chatResponse.object());
            assertNotNull(chatResponse.usage());
            assertNotNull(chatResponse.usage().completionTokens());
            assertNotNull(chatResponse.usage().promptTokens());
            assertNotNull(chatResponse.usage().totalTokens());

            // The OpenAI-compatible gateway does not return the watsonx-native fields modelId, modelVersion, or createdAt.
            assertNull(chatResponse.modelId());
            assertNull(chatResponse.modelVersion());
            assertNull(chatResponse.createdAt());
        }

        @Test
        void should_call_tool_without_parameters_when_chat_contains_tool_message() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("What time is it?"))
                .tools(Tool.of("get_time", "Get the current time"))
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            assertNotNull(chatResponse);

            var tools = chatResponse.toAssistantMessage().toolCalls();
            assertNotNull(tools);
            assertEquals(1, tools.size());
            assertNotNull(tools.get(0).id());
            assertEquals("get_time", tools.get(0).function().name());
            assertEquals("{}", tools.get(0).function().arguments());
        }

        @Test
        void should_force_tool_execution_when_tool_choice_option_is_set_to_required() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatParameters parameters = ModelGatewayChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Hello!"))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content() == null || assistantMessage.content().isBlank());
            assertNotNull(assistantMessage.toolCalls());
            assertEquals(1, assistantMessage.toolCalls().size());
            assertEquals(chatResponse.choices().get(0).finishReason(), FinishReason.TOOL_CALLS.value());
        }

        @Test
        void should_not_force_tool_execution_when_tool_choice_option_is_set_to_none() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatParameters parameters = ModelGatewayChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.NONE)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Send an email to a@a.it with subject \"a\" and body \"b\""))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(nonNull(assistantMessage.content()) || !assistantMessage.content().isBlank());
            assertNull(assistantMessage.toolCalls());
        }

        @Test
        void should_throw_exception_when_api_key_is_invalid() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .apiKey("invalid_api_key")
                .logRequests(true)
                .logResponses(true)
                .build();


            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Hello!"))
                .build();

            var ex = assertThrows(WatsonxException.class, () -> modelGatewayChatService.chat(request));
            assertTrue(ex.getMessage().contains("Provided API key could not be found."));
        }

        @Test
        void should_force_tool_call_when_tool_choice_option_is_set() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatParameters parameters = ModelGatewayChatParameters.builder()
                .toolChoice("send_email")
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Hello!"))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            var chatResponse = assertDoesNotThrow(() -> modelGatewayChatService.chat(request));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content() == null || assistantMessage.content().isBlank());
            assertNotNull(assistantMessage.toolCalls());
            assertEquals(1, assistantMessage.toolCalls().size());
        }

        @Test
        void should_manage_multiple_choices() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(ModelGatewayChatParameters.builder().n(2).build())
                .build();

            var chatResponse = modelGatewayChatService.chat("Tell me a joke");
            var assistantMessages = chatResponse.toAssistantMessages();
            assertEquals(2, assistantMessages.size());

            assistantMessages.forEach(assistantMessage -> {
                assertNotNull(assistantMessage.content());
                assertFalse(assistantMessage.hasToolCalls());
            });
        }

        @Test
        void should_manage_multiple_choices_with_tool() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(ModelGatewayChatParameters.builder().n(2).build())
                .build();

            var messages = List.<ChatMessage>of(UserMessage.text("What time is it?"));
            var tools = List.of(Tool.of("get_current_time"));
            var chatResponse = modelGatewayChatService.chat(messages, tools);
            var assistantMessages = chatResponse.toAssistantMessages();

            assertEquals(2, assistantMessages.size());
            assistantMessages.forEach(assistantMessage -> {
                assertTrue(assistantMessage.hasToolCalls());
            });
        }

        @Test
        void should_handle_parallel_tool_calls() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-4o")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(
                    ModelGatewayChatParameters.builder()
                        .parallelToolCalls(true)
                        .build()
                )
                .tools(Tool.of(
                    "get_current_time",
                    "Get the current time",
                    JsonSchema.object()
                        .property("country", JsonSchema.string("Name of the country"))
                        .required("country")
                ))
                .build();

            var assistantMessage = modelGatewayChatService.chat("Could you give me the time in Germany, Italy, and Japan?").toAssistantMessage();
            assertNotNull(assistantMessage.content());
            assertEquals(3, assistantMessage.toolCalls().size());
        }
    }

    @Nested
    class ChatStreaming {

        @Test
        void should_return_valid_chat_response_when_chat_streaming_is_invoked() throws Exception {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var chatRequest = ModelGatewayChatRequest.builder()
                .messages(
                    SystemMessage.of("""
                        You are an helpful assistant, your task is return number starting from 0 to 20.
                        Return the number in the following format:

                        1, 2, 3, ...

                        Return only the list of number without any other text."""),
                    UserMessage.text("Count")
                ).build();

            var futures = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    modelGatewayChatService.chatStreaming(chatRequest, new ChatHandler() {
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
                    });
                    return future;
                }).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(1, TimeUnit.MINUTES);

            for (Future<String> future : futures) {
                var result = assertDoesNotThrow(() -> future.get());
                assertEquals("0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20", result);
            }
        }

        @Test
        void should_return_valid_poem_json_response() {

            record Poem(String content, String topic) {}

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .responseAsJson()
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("""
                    Create a poem about dog, max 3 lines
                    Answer using the following json structure:
                    {
                        "content": <poem content>
                        "topic": <poem topic>
                    }
                    Only return the JSON. Do not write anything else or add 'json'"""))
                .parameters(parameters)
                .build();

            CompletableFuture<ModelGatewayChatResponse> future = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var chatResponse = assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            var poem = chatResponse.toAssistantMessage().toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));
        }

        @Test
        void should_return_valid_poem_json_schema() {

            record Poem(String content, String topic) {}

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .responseAsJsonSchema(
                    "poem",
                    JsonSchema.object()
                        .property("content", JsonSchema.string())
                        .property("topic", JsonSchema.enumeration("dog", "cat"))
                        .required("content", "topic")
                        .additionalProperties(false)
                        .build(),
                    true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Create a poem about dog, max 3 lines"))
                .parameters(parameters)
                .build();

            CompletableFuture<ModelGatewayChatResponse> future = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var chatResponse = assertDoesNotThrow(() -> future.get(1, TimeUnit.MINUTES));
            var poem = chatResponse.toAssistantMessage().toObject(Poem.class);

            assertNotNull(chatResponse);
            assertNotNull(poem);
            assertFalse(poem.content().isBlank());
            assertTrue(poem.topic.equalsIgnoreCase("dog"));

            assertNotNull(chatResponse.finishReason());
            assertNotNull(chatResponse.choices());
            assertNotNull(chatResponse.choices().get(0).finishReason());
            assertNotNull(chatResponse.choices().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message());
            assertNotNull(chatResponse.choices().get(0).message().content());
            assertNotNull(chatResponse.choices().get(0).message().role());
            assertNull(chatResponse.choices().get(0).message().refusal());
            assertNull(chatResponse.choices().get(0).message().toolCalls());
            assertNotNull(chatResponse.created());
            assertNotNull(chatResponse.id());
            assertNotNull(chatResponse.model());
            assertNotNull(chatResponse.object());
            assertNotNull(chatResponse.usage());
            assertNotNull(chatResponse.usage().completionTokens());
            assertNotNull(chatResponse.usage().promptTokens());
            assertNotNull(chatResponse.usage().totalTokens());

            // The OpenAI-compatible gateway does not return the watsonx-native fields modelId, modelVersion, or createdAt.
            assertNull(chatResponse.modelId());
            assertNull(chatResponse.modelVersion());
            assertNull(chatResponse.createdAt());

            // Gateway-only fields: for claude-sonnet-5 the stream reports serviceTier and systemFingerprint as null and cached as false.
            assertNull(chatResponse.serviceTier());
            assertNull(chatResponse.systemFingerprint());
            assertFalse(chatResponse.cached());
        }

        @Test
        void should_return_description_when_image_is_sent_in_chat() throws Exception {

            var image = getClass().getClassLoader().getResource("alien.jpg");

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            var parameters = ModelGatewayChatParameters.builder()
                .timeLimit(Duration.ofSeconds(30))
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.of(
                    TextContent.of("Give a short description of the image"),
                    ImageContent.from(Paths.get(image.toURI()))
                ))
                .parameters(parameters)
                .build();

            CompletableFuture<String> partialResponseFuture = new CompletableFuture<>();
            CompletableFuture<ModelGatewayChatResponse> chatResponseFuture = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {
                StringBuilder builder = new StringBuilder();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    builder.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    chatResponseFuture.complete((ModelGatewayChatResponse) completeResponse);
                    partialResponseFuture.complete(builder.toString());
                }

                @Override
                public void onError(Throwable error) {}

            });

            var chatResponse = assertDoesNotThrow(() -> chatResponseFuture.get(10, TimeUnit.SECONDS));
            var partialResponse = assertDoesNotThrow(() -> partialResponseFuture.get(10, TimeUnit.SECONDS));
            assertNotNull(chatResponse.toAssistantMessage().content());
            assertFalse(chatResponse.toAssistantMessage().content().isBlank());
            assertNotNull(partialResponse);
            assertFalse(partialResponse.isBlank());
            assertEquals(chatResponse.toAssistantMessage().content(), partialResponse);
        }

        @Test
        void should_call_tool_and_return_valid_tool_response_when_chat_contains_tool_message() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(30))
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Send an email to a@a.it with subject \"Test\" and body \"Hello\""))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .build();

            CompletableFuture<ModelGatewayChatResponse> chatResponseFuture = new CompletableFuture<>();
            CompletableFuture<CompletedToolCall> toolCallFuture = new CompletableFuture<>();
            CompletableFuture<ToolCall> fromPartialToolCallFuture = new CompletableFuture<>();
            CompletableFuture<Throwable> throwableFuture = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {
                Map<String, String> cachePartialToolCall = new HashMap<>();

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    throwableFuture.completeExceptionally(new RuntimeException("Unexpected partial response"));
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    chatResponseFuture.complete((ModelGatewayChatResponse) completeResponse);
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
                    cachePartialToolCall.computeIfAbsent("index", k -> partialToolCall.toolIndex() + "");
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

            var chatResponse = assertDoesNotThrow(() -> chatResponseFuture.get(60, TimeUnit.SECONDS));
            var toolCall = assertDoesNotThrow(() -> toolCallFuture.get(3, TimeUnit.SECONDS));
            var fromPartialTool = assertDoesNotThrow(() -> fromPartialToolCallFuture.get(3, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> throwableFuture.get(1, TimeUnit.SECONDS));
            assertEquals(toolCall.completionId(), chatResponse.id());
            assertEquals(toolCall.toolCall(), fromPartialTool);
            assertEquals(toolCall.toolCall(), chatResponse.toAssistantMessage().toolCalls().get(0));
            assertNotNull(chatResponse.toAssistantMessage().toolCalls().get(0).id());
            assertEquals("send_email", chatResponse.toAssistantMessage().toolCalls().get(0).function().name());
            JSONAssert.assertEquals(
                "{\"to\": \"a@a.it\", \"subject\": \"Test\", \"body\": \"Hello\"}",
                chatResponse.toAssistantMessage().toolCalls().get(0).function().arguments(),
                true);

            assertNotNull(chatResponse.finishReason());
            assertNotNull(chatResponse.choices());
            assertNotNull(chatResponse.choices().get(0).finishReason());
            assertNotNull(chatResponse.choices().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message());
            assertNull(chatResponse.choices().get(0).message().content());
            assertNotNull(chatResponse.choices().get(0).message().role());
            assertNull(chatResponse.choices().get(0).message().refusal());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0));
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).id());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).index());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).type());
            assertNotNull(chatResponse.choices().get(0).message().toolCalls().get(0).function());
            assertNotNull(chatResponse.created());
            assertNotNull(chatResponse.id());
            assertNotNull(chatResponse.model());
            assertNotNull(chatResponse.object());
            assertNotNull(chatResponse.usage());
            assertNotNull(chatResponse.usage().completionTokens());
            assertNotNull(chatResponse.usage().promptTokens());
            assertNotNull(chatResponse.usage().totalTokens());

            // The OpenAI-compatible gateway does not return the watsonx-native fields modelId, modelVersion, or createdAt.
            assertNull(chatResponse.modelId());
            assertNull(chatResponse.modelVersion());
            assertNull(chatResponse.createdAt());

            // Gateway-only fields: for claude-sonnet-5 the stream reports serviceTier and systemFingerprint as null and cached as false.
            assertNull(chatResponse.serviceTier());
            assertNull(chatResponse.systemFingerprint());
            assertFalse(chatResponse.cached());
        }

        @Test
        void should_call_tool_without_parameters_when_chat_contains_tool_message() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)

                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("What time is it?"))
                .tools(Tool.of("get_time", "Get the current time"))
                .build();

            CompletableFuture<ModelGatewayChatResponse> future = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            });

            var chatResponse = assertDoesNotThrow(() -> future.get(60, TimeUnit.SECONDS));
            assertNotNull(chatResponse);

            var tools = chatResponse.toAssistantMessage().toolCalls();
            assertNotNull(tools);
            assertEquals(1, tools.size());
            assertNotNull(tools.get(0).id());
            assertEquals("get_time", tools.get(0).function().name());
            assertEquals("{}", tools.get(0).function().arguments());
        }

        @Test
        void should_handle_multiple_streaming_responses_correctly_when_shared_handler_is_used() throws Exception {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            AtomicInteger completions = new AtomicInteger();
            List<String> contents = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(3);

            var sharedHandler = new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    completions.incrementAndGet();
                    contents.add(completeResponse.toAssistantMessage().content());
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            };

            var chatRequest = ModelGatewayChatRequest.builder()
                .messages(
                    SystemMessage.of("""
                        You are an helpful assistant, your task is return number starting from 0 to 20.
                        Return the number in the following format:

                        1, 2, 3, ...

                        Return only the list of number without any other text."""),
                    UserMessage.text("Count")
                ).build();

            var futures = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> modelGatewayChatService.chatStreaming(chatRequest, sharedHandler))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(1, TimeUnit.MINUTES);

            assertTrue(latch.await(1, TimeUnit.MINUTES));
            assertEquals(3, completions.get());
            assertEquals(3, contents.size());
            contents.forEach(c -> assertEquals("0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20", c));
        }

        @Test
        void should_throw_exception_when_api_key_is_invalid() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .apiKey("invalid_api_key")
                .logRequests(true)
                .logResponses(true)
                .build();


            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Hello!"))
                .build();

            CompletableFuture<Throwable> future = new CompletableFuture<>();
            modelGatewayChatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {}

                @Override
                public void onError(Throwable error) {
                    future.completeExceptionally(error);
                }
            });

            var ex = assertThrows(ExecutionException.class, () -> future.get(60, TimeUnit.SECONDS));
            var wex = assertInstanceOf(WatsonxException.class, ex.getCause());
            assertTrue(wex.getMessage().contains("Provided API key could not be found."));
        }

        @Test
        void should_force_tool_execution_when_tool_choice_option_is_set_to_required() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .build();

            ModelGatewayChatParameters parameters = ModelGatewayChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .build();

            ModelGatewayChatRequest request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text("Hello!"))
                .tools(Tool.of("send_email", "Send an email",
                    JsonSchema.object()
                        .property("to", JsonSchema.string())
                        .property("subject", JsonSchema.string())
                        .property("body", JsonSchema.string())
                        .required("to", "body")))
                .parameters(parameters)
                .build();

            CompletableFuture<ModelGatewayChatResponse> future = new CompletableFuture<>();
            CompletableFuture<CompletedToolCall> futureToolCall = new CompletableFuture<>();

            assertDoesNotThrow(() -> modelGatewayChatService.chatStreaming(request, new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    future.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                    futureToolCall.complete(completeToolCall);
                }

                @Override
                public void onError(Throwable error) {}
            }));

            var chatResponse = assertDoesNotThrow(() -> future.get(30, TimeUnit.SECONDS));
            var completedToolCall = assertDoesNotThrow(() -> futureToolCall.get(10, TimeUnit.SECONDS));
            var assistantMessage = chatResponse.toAssistantMessage();
            assertTrue(assistantMessage.content() == null || assistantMessage.content().isBlank());
            assertNotNull(assistantMessage.toolCalls());
            assertNotNull(completedToolCall);
            assertEquals(chatResponse.choices().get(0).finishReason(), FinishReason.TOOL_CALLS.value());
            assertEquals(1, assistantMessage.toolCalls().size());
        }

        @Test
        void should_handle_streaming_conversation_with_tool_interception() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .apiKey(API_KEY)
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .logRequests(true)
                .logResponses(true)
                .toolInterceptor((ctx, fc) -> {

                    var arguments = fc.arguments();

                    if (!arguments.contains("country")) {
                        int firstBraceIndex = arguments.indexOf('{');
                        int firstQuoteAfterBrace = arguments.indexOf("\\\"", firstBraceIndex + 1);
                        String countryPart = arguments.substring(firstQuoteAfterBrace + 2, arguments.indexOf("\\\"", firstQuoteAfterBrace + 2));
                        arguments = "{ \"country\": \"" + countryPart + "\"}";
                    }

                    return fc.withArguments(arguments);

                }).build();

            var chatRequest = ModelGatewayChatRequest.builder()
                .addMessages(UserMessage.text("What time is it in Italy?"))
                .tools(Tool.of(
                    "get_current_time",
                    "Get the current time",
                    JsonSchema.object()
                        .property("country", JsonSchema.string())
                        .required("country")
                ));

            var firstResponse = new CompletableFuture<ChatResponse>();
            modelGatewayChatService.chatStreaming(chatRequest.build(), new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    firstResponse.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var assistantMessage = assertDoesNotThrow(() -> firstResponse.get(60, TimeUnit.SECONDS)).toAssistantMessage();
            assertTrue(assistantMessage.hasToolCalls(), assistantMessage.content());
            var toolCall = assistantMessage.toolCalls().get(0);
            chatRequest
                .addMessages(
                    assistantMessage,
                    toolCall.processTool((toolName, toolArgs) -> {
                        assertEquals("get_current_time", toolName);
                        assertEquals("Italy", toolArgs.get("country"));
                        return "The current time in Italy is 11:13";
                    }));

            var secondResponse = new CompletableFuture<ChatResponse>();
            modelGatewayChatService.chatStreaming(chatRequest.build(), new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    secondResponse.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });

            var chatResponse = assertDoesNotThrow(() -> secondResponse.get(60, TimeUnit.SECONDS));
            assistantMessage = chatResponse.toAssistantMessage();
            assertFalse(assistantMessage.hasToolCalls(), "Response: " + chatResponse);
            assertTrue(assistantMessage.content().contains("11:13"), assistantMessage.content());

            chatRequest
                .parameters(null)
                .addMessages(assistantMessage, UserMessage.text("And in Germany?"));
            var thirdResponse = new CompletableFuture<ChatResponse>();
            modelGatewayChatService.chatStreaming(chatRequest.build(), new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    thirdResponse.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            });

            assistantMessage = assertDoesNotThrow(() -> thirdResponse.get(60, TimeUnit.SECONDS)).toAssistantMessage();
            assertTrue(assistantMessage.hasToolCalls(), assistantMessage.content());
            toolCall = assistantMessage.toolCalls().get(0);
            chatRequest
                .addMessages(
                    assistantMessage,
                    toolCall.processTool((toolName, toolArgs) -> {
                        assertEquals("get_current_time", toolName);
                        assertEquals("Germany", toolArgs.get("country"));
                        return "The current time in Germany is 11:15";
                    }));

            var fourthResponse = new CompletableFuture<ChatResponse>();
            modelGatewayChatService.chatStreaming(chatRequest.build(), new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    fourthResponse.complete((ModelGatewayChatResponse) completeResponse);
                }

                @Override
                public void onError(Throwable error) {}
            });
            chatResponse = assertDoesNotThrow(() -> fourthResponse.get(60, TimeUnit.SECONDS));
            assistantMessage = chatResponse.toAssistantMessage();
            assertFalse(assistantMessage.hasToolCalls(), "Response: " + chatResponse);
            assertTrue(assistantMessage.content().contains("11:15"), assistantMessage.content());
        }

        @Test
        void should_handle_streaming_conversation_with_multiple_tools_in_interception_tools() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .apiKey(API_KEY)
                .baseUrl(URL)
                .modelId("claude-sonnet-5")
                .logRequests(true)
                .toolInterceptor((ctx, fc) -> {

                    var arguments = fc.arguments();

                    if (!arguments.contains("country")) {
                        int firstBraceIndex = arguments.indexOf('{');
                        int firstQuoteAfterBrace = arguments.indexOf("\\\"", firstBraceIndex + 1);
                        String countryPart = arguments.substring(firstQuoteAfterBrace + 2, arguments.indexOf("\\\"", firstQuoteAfterBrace + 2));
                        arguments = "{ \"country\": \"" + countryPart + "\"}";
                    }


                    return fc.withArguments(arguments);

                }).build();

            var chatRequest = ModelGatewayChatRequest.builder()
                .addMessages(
                    SystemMessage.of("You are an helpful assistant"),
                    UserMessage.text("What time is it in Italy, Germany and Japan?")
                )
                .tools(Tool.of(
                    "get_current_time",
                    "Get the current time",
                    JsonSchema.object()
                        .property("country", JsonSchema.string("Name of the country"))
                        .required("country")
                ));

            AssistantMessage assistantMessage;
            int i = 0;
            do {

                if (i > 5)
                    throw new RuntimeException("Too many attempts");

                var firstResponse = new CompletableFuture<ChatResponse>();
                modelGatewayChatService.chatStreaming(chatRequest.build(), new ChatHandler() {

                    @Override
                    public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        firstResponse.complete((ModelGatewayChatResponse) completeResponse);
                    }

                    @Override
                    public void onError(Throwable error) {}
                });

                assistantMessage = assertDoesNotThrow(() -> firstResponse.get(30, TimeUnit.SECONDS)).toAssistantMessage();
                assertTrue(assistantMessage.hasToolCalls());
                i++;
            } while (assistantMessage.toolCalls().size() == 1 && i < 5);

            var toolCall = assistantMessage.toolCalls().get(0);
            chatRequest.addMessages(
                toolCall.processTool((toolName, toolArgs) -> {
                    assertEquals("get_current_time", toolName);
                    assertEquals("Italy", toolArgs.get("country"));
                    return "The current time in Italy is 11:13";
                }));
            toolCall = assistantMessage.toolCalls().get(1);
            chatRequest.addMessages(
                toolCall.processTool((toolName, toolArgs) -> {
                    assertEquals("get_current_time", toolName);
                    assertEquals("Germany", toolArgs.get("country"));
                    return "The current time in Germany is 11:13";
                }));
            toolCall = assistantMessage.toolCalls().get(2);
            chatRequest.addMessages(
                toolCall.processTool((toolName, toolArgs) -> {
                    assertEquals("get_current_time", toolName);
                    assertEquals("Japan", toolArgs.get("country"));
                    return "The current time in Japan is 23:13";
                }));
        }

        @Test
        void should_manage_multiple_choices() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(ModelGatewayChatParameters.builder().n(2).build())
                .build();

            var chatResponse = modelGatewayChatService.chatStreaming("Tell me a joke", (partialResponse, partialChatResponse) -> {}).join();
            var assistantMessages = chatResponse.toAssistantMessages();
            assertEquals(2, assistantMessages.size());

            assistantMessages.forEach(assistantMessage -> {
                assertNotNull(assistantMessage.content());
                assertFalse(assistantMessage.hasToolCalls());
            });
        }

        @Test
        void should_manage_multiple_choices_with_tool() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-5.6-terra-dzus")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(ModelGatewayChatParameters.builder().n(2).build())
                .build();

            var messages = List.<ChatMessage>of(UserMessage.text("What time is it?"));
            var tools = List.of(Tool.of("get_current_time", "Get current time", JsonSchema.object()));
            var chatResponse = modelGatewayChatService.chatStreaming(messages, tools, (partialResponse, partialChatResponse) -> {}).join();
            var assistantMessages = chatResponse.toAssistantMessages();

            assertEquals(2, assistantMessages.size());
            assistantMessages.forEach(assistantMessage -> {
                assertNull(assistantMessage.content());
                assertTrue(assistantMessage.hasToolCalls());
                assertEquals(1, assistantMessage.toolCalls().size());
                assertEquals("get_current_time", assistantMessage.toolCalls().get(0).function().name());
            });
        }

        @Test
        void should_handle_parallel_tool_calls() {

            var modelGatewayChatService = ModelGatewayChatService.builder()
                .baseUrl(URL)
                .modelId("gpt-4o")
                .authenticator(authentication)
                .logRequests(true)
                .logResponses(true)
                .parameters(
                    ModelGatewayChatParameters.builder()
                        .parallelToolCalls(true)
                        .build()
                )
                .tools(Tool.of(
                    "get_current_time",
                    "Get the current time",
                    JsonSchema.object()
                        .property("country", JsonSchema.string("Name of the country"))
                        .required("country")
                ))
                .build();

            AtomicInteger counter = new AtomicInteger(0);
            Set<String> countries = Collections.synchronizedSet(Sets.newHashSet());
            StringBuilder content = new StringBuilder();
            var chatHandler = new ChatHandler() {

                @Override
                public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                    content.append(partialResponse);
                }

                @Override
                public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                    counter.incrementAndGet();
                    completeToolCall.processTool((toolName, toolArgs) -> countries.add(toolArgs.get("country")));
                }
            };

            var chatResponse = modelGatewayChatService.chatStreaming("Could you give me the time in Germany, Italy, and Japan?", chatHandler).join();
            var assistantMessage = chatResponse.toAssistantMessage();
            assertNotNull(content);
            assertEquals(3, assistantMessage.toolCalls().size());
            assertTrue(countries.contains("Germany") && countries.contains("Italy") && countries.contains("Japan"));
        }
    }
}
