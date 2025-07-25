/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoice;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.StreamingToolFetcher;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

/**
 * Service class to interact with IBM watsonx.ai Text Chat APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ChatService chatService = ChatService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .authenticationProvider(authProvider)
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-3-8b-instruct")
 *     .build();
 *
 * ChatResponse response = chatService.chat(
 *     SystemMessage.of("You are a helpful assistant."),
 *     UserMessage.text("Tell me a joke.")
 * );
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-chat" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class ChatService extends WatsonxService {

    protected ChatService(Builder builder) {
        super(builder);
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages), null, null);
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */

    public ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, null, null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(messages, tools, null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools), null);
    }

    /**
     * Sends a chat request to the model using the provided messages, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, and customize
     * the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters parameters to customize the output generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters) {
        return chat(messages, null, parameters);
    }

    /**
     * Sends a chat request to the model using the provided messages, tools, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models, and customize the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @param parameters parameters to customize the output generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, List<Tool> tools, ChatParameters parameters) {

        if (isNull(messages) || messages.isEmpty())
            throw new IllegalArgumentException("The list of messages can not be null or empty");

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
        var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        var chatRequest = ChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/chat?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(chatRequest)))
                .timeout(Duration.ofMillis(timeout))
                .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ChatResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatHandler handler) {
        return chatStreaming(messages, null, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatHandler handler) {
        return chatStreaming(messages, tools, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatParameters parameters,
        ChatHandler handler) {
        return chatStreaming(messages, null, parameters, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages, tools, and parameters.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, List<Tool> tools,
        ChatParameters parameters, ChatHandler handler) {

        requireNonNull(handler, "The chatHandler parameter can not be null");

        if (isNull(messages) || messages.isEmpty())
            throw new IllegalArgumentException("The list of messages can not be null or empty");

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());

        if (isNull(parameters.getModelId()) && isNull(this.modelId))
            throw new NullPointerException("The modelId must be provided");

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
        var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        var chatRequest = ChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/chat_stream?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(chatRequest)))
                .timeout(Duration.ofMillis(timeout))
                .build();

        var subscriber = new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private String finishReason;
            private String role;
            private String refusal;
            private boolean success = true;
            private final StringBuilder stringBuilder = new StringBuilder();
            private final List<StreamingToolFetcher> tools = new ArrayList<>();
            private final ChatResponse chatResponse = new ChatResponse();

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {

                try {

                    if (isNull(partialMessage) || partialMessage.isBlank() || !partialMessage.startsWith("data: "))
                        return;

                    var messageData = partialMessage.split("data: ")[1];
                    var chunk = Json.fromJson(messageData, PartialChatResponse.class);

                    // Last message get the "usage" values
                    if (chunk.choices().size() == 0) {
                        chatResponse.setUsage(chunk.usage());
                        return;
                    }

                    var message = chunk.choices().get(0);

                    if (isNull(chatResponse.getCreated()) && nonNull(chunk.created()))
                        chatResponse.setCreated(chunk.created());

                    if (isNull(chatResponse.getCreatedAt()) && nonNull(chunk.createdAt()))
                        chatResponse.setCreatedAt(chunk.createdAt());

                    if (isNull(chatResponse.getId()) && nonNull(chunk.id()))
                        chatResponse.setId(chunk.id());

                    if (isNull(chatResponse.getModelId()) && nonNull(chunk.modelId()))
                        chatResponse.setModelId(chunk.modelId());

                    if (isNull(chatResponse.getObject()) && nonNull(chunk.object()))
                        chatResponse.setObject(chunk.object());

                    if (isNull(chatResponse.getModelVersion()) && nonNull(chunk.modelVersion()))
                        chatResponse.setModelVersion(chunk.modelVersion());

                    if (isNull(chatResponse.getModel()) && nonNull(chunk.model()))
                        chatResponse.setModel(chunk.model());

                    if (isNull(finishReason) && nonNull(message.finishReason()))
                        finishReason = message.finishReason();

                    if (isNull(role) && nonNull(message.delta().role()))
                        role = message.delta().role();

                    if (isNull(refusal) && nonNull(message.delta().refusal()))
                        refusal = message.delta().refusal();

                    if (message.delta().toolCalls() != null) {

                        StreamingToolFetcher toolFetcher;

                        // Watsonx doesn't return "tool_calls" when the tool-choice is set to REQUIRED.
                        if (nonNull(chatRequest.getToolChoiceOption())
                            && chatRequest.getToolChoiceOption().equals(ToolChoice.REQUIRED.type()))
                            finishReason = "tool_calls";

                        // During streaming there is only one element in the tool_calls,
                        // but the "index" field can be used to understand how many tools need to be
                        // executed.
                        var deltaTool = message.delta().toolCalls().get(0);
                        var index = deltaTool.index();

                        // Check if there is an incomplete version of the TextChatToolCall object.
                        if ((index + 1) > tools.size()) {
                            // First occurrence of the object, create it.
                            toolFetcher = new StreamingToolFetcher(index);
                            tools.add(toolFetcher);
                        } else {
                            // Incomplete version is present, complete it.
                            toolFetcher = tools.get(index);
                        }

                        toolFetcher.setId(deltaTool.id());
                        toolFetcher.setType(deltaTool.type());

                        if (deltaTool.function() != null) {
                            toolFetcher.setName(deltaTool.function().name());
                            toolFetcher.appendArguments(deltaTool.function().arguments());
                        }
                    }

                    if (message.delta().content() != null) {

                        String token = message.delta().content();

                        if (token.isEmpty())
                            return;

                        stringBuilder.append(token);
                        handler.onPartialResponse(token, chunk);
                    }

                } catch (RuntimeException e) {

                    success = false;
                    onError(e);

                } finally {

                    if (success)
                        subscription.request(1);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                handler.onError(throwable);
            }

            @Override
            public void onComplete() {
                try {

                    List<ToolCall> toolCalls = null;
                    String content = stringBuilder.toString();

                    if (nonNull(finishReason) && finishReason.equals("tool_calls")) {
                        content = null;
                        toolCalls = tools.stream()
                            .map(StreamingToolFetcher::build)
                            .toList();
                    }

                    var resultMessage = new ResultMessage(role, content, refusal, toolCalls);
                    chatResponse.setChoices(List.of(new ResultChoice(0, resultMessage, finishReason)));
                    handler.onCompleteResponse(chatResponse);

                } catch (RuntimeException e) {
                    handler.onError(e);
                }
            }
        };

        return asyncHttpClient
            .send(httpRequest,
                responseInfo -> logResponses
                    ? BodySubscribers
                        .fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
                    : BodySubscribers.fromLineSubscriber(subscriber)
            ).thenApply(response -> null);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ChatService chatService = ChatService.builder()
     *     .url("https://...") // or use CloudRegion
     *     .authenticationProvider(authProvider)
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-3-8b-instruct")
     *     .build();
     *
     * ChatResponse response = chatService.chat(
     *     SystemMessage.of("You are a helpful assistant."),
     *     UserMessage.text("Tell me a joke.")
     * );
     * }</pre>
     *
     * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-chat" target="_blank"> official documentation</a>.
     *
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {

        /**
         * Builds a {@link ChatService} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatService}
         */
        public ChatService build() {
            return new ChatService(this);
        }
    }
}