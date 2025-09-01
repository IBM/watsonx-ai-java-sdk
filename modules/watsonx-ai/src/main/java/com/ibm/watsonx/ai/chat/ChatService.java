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
import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoice;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
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
 *     SystemMessage.of("You are a helpful assistant"),
 *     UserMessage.text("Tell me a joke")
 * );
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-chat" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class ChatService extends ModelService implements ChatProvider {

    protected ChatService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {

        List<ChatMessage> messages = chatRequest.getMessages();
        List<Tool> tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        ChatParameters parameters = chatRequest.getParameters();

        if (messages.stream().anyMatch(ControlMessage.class::isInstance)
            && isNull(chatRequest.getExtractionTags()))
            throw new IllegalArgumentException("Extraction tags are required when using control messages");

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
        var spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        var textChatRequest = TextChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest = HttpRequest.newBuilder(URI.create(url.toString() + "%s/chat?version=%s".formatted(ML_API_TEXT_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(timeout));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            var chatResponse = fromJson(httpReponse.body(), ChatResponse.class);

            // Watsonx doesn't return "tool_calls" when the tool-choice-option is set to REQUIRED.
            // Open an issue.
            if (nonNull(parameters.getToolChoiceOption()) && parameters.getToolChoiceOption().equals(ToolChoice.REQUIRED.type())) {
                var assistantMessage = chatResponse.toAssistantMessage();
                if (nonNull(assistantMessage.toolCalls()) && !assistantMessage.toolCalls().isEmpty())
                    chatResponse.getChoices().get(0).setFinishReason("tool_calls");
            }

            chatResponse.setExtractionTags(chatRequest.getExtractionTags());
            return chatResponse;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(handler, "The chatHandler parameter can not be null");

        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = requireNonNullElse(chatRequest.getParameters(), ChatParameters.builder().build());

        if (messages.stream().anyMatch(ControlMessage.class::isInstance)
            && isNull(chatRequest.getExtractionTags()))
            throw new IllegalArgumentException("Extraction tags are required when using control messages");

        var modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
        var projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
        var spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        var textChatRequest = TextChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest = HttpRequest.newBuilder(URI.create(url.toString() + "%s/chat_stream?version=%s".formatted(ML_API_TEXT_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(timeout));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        Map<String, Boolean> toolHasParameters = new HashMap<>();
        if (nonNull(tools))
            tools.stream().forEach(tool -> toolHasParameters.put(tool.function().name(), tool.hasParameters()));

        var subscriber = subscriber(textChatRequest.getToolChoiceOption(), toolHasParameters, chatRequest.getExtractionTags(), handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handlerError(t, handler));
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
     *     SystemMessage.of("You are a helpful assistant"),
     *     UserMessage.text("Tell me a joke")
     * );
     * }</pre>
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
    public static class Builder extends ModelService.Builder<Builder> {

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