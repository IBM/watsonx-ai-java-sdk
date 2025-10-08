/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

/**
 * Service for interacting with IBM watsonx.ai Text Chat APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ChatService chatService = ChatService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
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
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public final class ChatService extends ModelService implements ChatProvider {

    public static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatRestClient client;

    private ChatService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
        client = ChatRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.getAuthenticationProvider())
            .build();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        if (nonNull(chatRequest.getDeploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

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

        var chatResponse = client.chat(parameters.getTransactionId(), textChatRequest);

        // Watsonx doesn't return "tool_calls" when the tool-choice-option is set to REQUIRED.
        // Open an issue.
        if (nonNull(parameters.getToolChoiceOption()) && parameters.getToolChoiceOption().equals(ToolChoiceOption.REQUIRED.type())) {
            var assistantMessage = chatResponse.toAssistantMessage();
            if (nonNull(assistantMessage.toolCalls()) && !assistantMessage.toolCalls().isEmpty())
                chatResponse.getChoices().get(0).setFinishReason("tool_calls");
        }

        chatResponse.setExtractionTags(chatRequest.getExtractionTags());
        return chatResponse;
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        if (nonNull(chatRequest.getDeploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

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

        return client.chatStreaming(parameters.getTransactionId(), chatRequest.getExtractionTags(), textChatRequest, handler);
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message Message to send.
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(String message) {
        return chat(UserMessage.text(message));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */

    public ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, ChatParameters.builder().build());
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
        return chat(messages, Arrays.asList(tools));
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
        return chat(messages, null, tools);
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
        return chat(messages, parameters, List.of());
    }

    /**
     * Sends a chat request to the model using the provided messages, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, and customize
     * the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters parameters to customize the output generation
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters, Tool... tools) {
        return chat(messages, parameters, Arrays.asList(tools));
    }

    /**
     * Sends a chat request to the model using the provided messages, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, and customize
     * the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters parameters to customize the output generation
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters, List<Tool> tools) {
        return chat(
            ChatRequest.builder()
                .messages(messages)
                .parameters(parameters)
                .tools(tools)
                .build()
        );
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message Message to send.
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(String message, ChatHandler handler) {
        return chatStreaming(List.of(UserMessage.text(message)), handler);
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
        return chatStreaming(messages, ChatParameters.builder().build(), handler);
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
        return chatStreaming(messages, ChatParameters.builder().build(), tools, handler);
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
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, ChatHandler handler) {
        return chatStreaming(messages, parameters, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, List<Tool> tools,
        ChatHandler handler) {
        var chatRequest = ChatRequest.builder()
            .messages(messages)
            .parameters(parameters)
            .tools(tools)
            .build();
        return chatStreaming(chatRequest, handler);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ChatService chatService = ChatService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
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
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatService} instances with configurable parameters.
     */
    public static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

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