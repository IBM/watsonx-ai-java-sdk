/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatProvider;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with the IBM watsonx.ai Model Gateway chat APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayChatService modelGatewayChatService = ModelGatewayChatService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .modelId("gpt-4o")
 *     .build();
 *
 * ModelGatewayChatResponse response = modelGatewayChatService.chat(
 *     SystemMessage.of("You are a helpful assistant"),
 *     UserMessage.text("Tell me a joke")
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 * <p>
 * Gateway-specific generation knobs (reasoning effort, service tier, caching, modalities) are available through {@link ModelGatewayChatParameters},
 * which extends the shared {@link com.ibm.watsonx.ai.chat.model.BaseChatParameters}.
 *
 * @see Authenticator
 */
public class ModelGatewayChatService extends WatsonxService implements ChatProvider<ModelGatewayChatRequest, ModelGatewayChatResponse> {
    private final ModelGatewayChatRestClient client;
    private final MessageInterceptor<ModelGatewayChatRequest> messageInterceptor;
    private final ToolInterceptor<ModelGatewayChatRequest> toolInterceptor;
    private final ChatProvider<ModelGatewayChatRequest, ModelGatewayChatResponse> chatProvider;
    private final ModelGatewayChatParameters defaultParameters;
    private final List<Tool> defaultTools;
    private final String modelId;

    private ModelGatewayChatService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        modelId = requireNonNull(builder.modelId, "The modelId must be provided");
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
        defaultTools = builder.defaultTools;
        defaultParameters = builder.defaultParameters;

        client = ModelGatewayChatRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();

        chatProvider = nonNull(messageInterceptor) || nonNull(toolInterceptor)
            ? builder.copyWithoutInterceptors().parameters(defaultParameters).build()
            : null;
    }

    /**
     * Sends a chat request to the Model Gateway.
     *
     * @param chatRequest the {@link ModelGatewayChatRequest}
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    @Override
    public ModelGatewayChatResponse chat(ModelGatewayChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        var gatewayRequest = ModelGatewayChatUtility.buildGatewayRequest(chatRequest, defaultParameters, modelId, this.timeout.toMillis());
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;

        var chatResponse = client.chat(transactionId, Duration.ofMillis(gatewayRequest.timeLimit()), gatewayRequest);

        if (nonNull(messageInterceptor)) {
            var newChoices = messageInterceptor.intercept(new InterceptorContext<>(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        if (nonNull(toolInterceptor)) {
            var newChoices = toolInterceptor.intercept(new InterceptorContext<>(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        return chatResponse;
    }

    /**
     * Sends a streaming chat request to the Model Gateway.
     *
     * @param chatRequest the {@link ModelGatewayChatRequest}
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     * @return a {@link CompletableFuture} that completes with the final {@link ChatResponse}
     */
    @Override
    public CompletableFuture<ChatResponse> chatStreaming(ModelGatewayChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        var gatewayRequest = ModelGatewayChatUtility.buildGatewayRequest(chatRequest, defaultParameters, modelId, this.timeout.toMillis(), true);
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var context = ChatClientContext.<ModelGatewayChatRequest>builder()
            .chatProvider(chatProvider)
            .chatRequest(chatRequest)
            .toolInterceptor(toolInterceptor)
            .build();

        return client.chatStreaming(transactionId, Duration.ofMillis(gatewayRequest.timeLimit()), gatewayRequest, context, handler);
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message the message to send
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(String message) {
        return chat(UserMessage.text(message));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, (ModelGatewayChatParameters) null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools));
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(messages, null, tools);
    }

    /**
     * Sends a chat request to the model using the provided messages and gateway parameters.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters gateway parameters to customize the output generation
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(List<ChatMessage> messages, ModelGatewayChatParameters parameters) {
        return chat(messages, parameters, null);
    }

    /**
     * Sends a chat request to the model using the provided messages, parameters, and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters gateway parameters to customize the output generation
     * @param tools list of tools the model may call during generation
     * @return a {@link ModelGatewayChatResponse} containing the model's reply
     */
    public ModelGatewayChatResponse chat(List<ChatMessage> messages, ModelGatewayChatParameters parameters, List<Tool> tools) {
        return chat(
            ModelGatewayChatRequest.builder()
                .messages(messages)
                .parameters(parameters)
                .tools(isNull(tools) ? defaultTools : tools)
                .build()
        );
    }

    /**
     * Sends a streaming chat request to the model using the provided message.
     *
     * @param message the message to send
     * @param handler a {@link ChatHandler} implementation
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(String message, ChatHandler handler) {
        return chatStreaming(List.of(UserMessage.text(message)), handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a {@link ChatHandler} implementation
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ChatHandler handler) {
        return chatStreaming(messages, (ModelGatewayChatParameters) null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages and tools.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatHandler handler) {
        return chatStreaming(messages, null, tools, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages and gateway parameters.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters gateway parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayChatParameters parameters, ChatHandler handler) {
        return chatStreaming(messages, parameters, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided message, delegating to a simple text consumer.
     *
     * @param message the message to send
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(String message, Consumer<String> handler) {
        return chatStreaming(List.of(UserMessage.text(message)), handler);
    }

    /**
     * Sends a streaming chat request using the provided messages, delegating to a simple text consumer.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, Consumer<String> handler) {
        return chatStreaming(messages, (ModelGatewayChatParameters) null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages and gateway parameters, delegating to a simple text consumer.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters gateway parameters for the chat invocation
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayChatParameters parameters,
        Consumer<String> handler) {
        return chatStreaming(messages, parameters, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages and tools, delegating to a simple text consumer.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, List<Tool> tools, Consumer<String> handler) {
        return chatStreaming(messages, null, tools, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages, parameters, and tools, delegating to a simple text consumer.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters gateway parameters for the chat invocation
     * @param tools the list of tools that the model may use
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(
        List<ChatMessage> messages, ModelGatewayChatParameters parameters, List<Tool> tools, Consumer<String> handler) {
        return chatStreaming(messages, parameters, tools, new ChatHandler() {
            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                handler.accept(partialResponse);
            }
        });
    }

    /**
     * Sends a streaming chat request using the provided messages, parameters, and tools.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters gateway parameters for the chat invocation
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayChatParameters parameters, List<Tool> tools,
        ChatHandler handler) {
        var chatRequest = ModelGatewayChatRequest.builder()
            .messages(messages)
            .parameters(parameters)
            .tools(isNull(tools) ? defaultTools : tools)
            .build();
        return chatStreaming(chatRequest, handler);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ModelGatewayChatService modelGatewayChatService = ModelGatewayChatService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .modelId("gpt-4o")
     *     .build();
     *
     * ModelGatewayChatResponse response = modelGatewayChatService.chat(
     *     SystemMessage.of("You are a helpful assistant"),
     *     UserMessage.text("Tell me a joke")
     * );
     * }</pre>
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayChatService} instances with configurable parameters.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {
        private String modelId;
        private MessageInterceptor<ModelGatewayChatRequest> messageInterceptor;
        private ToolInterceptor<ModelGatewayChatRequest> toolInterceptor;
        private ModelGatewayChatParameters defaultParameters;
        private List<Tool> defaultTools;

        private Builder() {}

        /**
         * Sets the model identifier to forward requests to (e.g., {@code "gpt-4o"}).
         *
         * @param modelId the model id
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Sets the default {@link ModelGatewayChatParameters} applied to all chat requests when no per-request parameters are provided.
         * <p>
         * These default values serve as fallbacks for any parameter not explicitly set. When parameters are provided in the chat method call, they
         * take precedence over these defaults.
         *
         * @param parameters the default parameters to use
         */
        public Builder parameters(ModelGatewayChatParameters parameters) {
            this.defaultParameters = parameters;
            return this;
        }

        /**
         * Registers a {@link MessageInterceptor} used to modify or sanitize the assistant's textual content before it is returned to the caller.
         *
         * @param messageInterceptor the interceptor to apply
         */
        public Builder messageInterceptor(MessageInterceptor<ModelGatewayChatRequest> messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
            return this;
        }

        /**
         * Registers a {@link ToolInterceptor} to modify or normalize function call arguments before tool execution.
         *
         * @param toolInterceptor the interceptor to apply
         */
        public Builder toolInterceptor(ToolInterceptor<ModelGatewayChatRequest> toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
            return this;
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         *
         * @param executableTools list of {@link ExecutableTool} objects to set as defaults
         */
        public Builder tools(ExecutableTool... executableTools) {
            return tools(Arrays.stream(executableTools).map(ExecutableTool::schema).toList());
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(Tool... tools) {
            return tools(Arrays.asList(tools));
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(List<Tool> tools) {
            this.defaultTools = tools;
            return this;
        }

        /**
         * Returns a copy of this builder without the registered interceptors.
         */
        private Builder copyWithoutInterceptors() {
            return new Builder().copyFrom(this).modelId(modelId).tools(defaultTools);
        }

        /**
         * Builds a {@link ModelGatewayChatService} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayChatService}
         */
        public ModelGatewayChatService build() {
            return new ModelGatewayChatService(this);
        }
    }
}
