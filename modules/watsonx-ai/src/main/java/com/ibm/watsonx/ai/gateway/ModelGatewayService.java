/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
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
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with the IBM watsonx.ai Model Gateway.
 * <p>
 * The Model Gateway provides an OpenAI-compatible API that routes chat requests to external foundation models (for example OpenAI GPT or Anthropic
 * Claude) through a unified interface. It allows you to switch between multiple model providers without changing your application code, and to apply
 * access policies, rate limits, and caching rules configured by your platform administrator.
 * <p>
 * Unlike {@link ChatService}, which targets IBM-hosted models directly, {@code ModelGatewayService} forwards requests to third-party models via the
 * gateway endpoint. The gateway returns a {@link GatewayChatResponse} that extends the standard chat response with gateway-specific fields such as
 * {@code serviceTier}, {@code systemFingerprint}, and {@code cached}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayService modelGatewayService = ModelGatewayService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .modelId("gpt-4o")
 *     .build();
 *
 * GatewayChatResponse response = modelGatewayService.chat(
 *     SystemMessage.of("You are a helpful assistant"),
 *     UserMessage.text("Tell me a joke")
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 * <p>
 * Gateway-specific generation knobs (reasoning effort, service tier, caching, modalities) are available through {@link ModelGatewayParameters}, which
 * extends the shared {@link BaseChatParameters}.
 *
 * @see GatewayChatProvider
 * @see GatewayChatResponse
 * @see ModelGatewayParameters
 * @see Authenticator
 */
public class ModelGatewayService extends WatsonxService implements GatewayChatProvider {
    private final ModelGatewayRestClient client;
    private final MessageInterceptor messageInterceptor;
    private final ToolInterceptor toolInterceptor;
    private final GatewayChatProvider chatProvider;
    private final BaseChatParameters defaultParameters;
    private final List<Tool> defaultTools;
    private final String modelId;

    private ModelGatewayService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        modelId = requireNonNull(builder.modelId, "The modelId must be provided");
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
        defaultTools = builder.defaultTools;
        defaultParameters = builder.defaultParameters;

        client = ModelGatewayRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();

        if (nonNull(messageInterceptor) || nonNull(toolInterceptor)) {
            chatProvider = new Builder()
                .modelId(modelId)
                .authenticator(builder.authenticator())
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(timeout)
                .version(version)
                .parameters(defaultParameters)
                .httpClient(httpClient)
                .verifySsl(verifySsl)
                .build();
        } else
            chatProvider = null;
    }

    @Override
    public GatewayChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        var gatewayRequest = GatewayUtility.buildGatewayRequest(chatRequest, defaultParameters, modelId, this.timeout.toMillis());
        var extractionTags = nonNull(chatRequest.thinking()) ? chatRequest.thinking().extractionTags() : null;
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;

        var perRequestTimeLimit = nonNull(chatRequest.parameters()) ? chatRequest.parameters().timeLimit() : null;
        var defaultTimeLimit = nonNull(defaultParameters) ? defaultParameters.timeLimit() : null;
        var timeoutMillis = getOrDefault(perRequestTimeLimit, getOrDefault(defaultTimeLimit, this.timeout.toMillis()));

        var chatResponse = client.chat(transactionId, Duration.ofMillis(timeoutMillis), gatewayRequest);

        if (nonNull(messageInterceptor)) {
            var newChoices = messageInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = (GatewayChatResponse) chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        if (nonNull(toolInterceptor)) {
            var newChoices = toolInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = (GatewayChatResponse) chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        return chatResponse.toBuilder().extractionTags(extractionTags).build();
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        var gatewayRequest = GatewayUtility.buildGatewayRequest(chatRequest, defaultParameters, modelId, this.timeout.toMillis(), true);
        var extractionTags = nonNull(chatRequest.thinking()) ? chatRequest.thinking().extractionTags() : null;
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var context = ChatClientContext.builder()
            .chatProvider(chatProvider)
            .chatRequest(chatRequest)
            .toolInterceptor(toolInterceptor)
            .extractionTags(extractionTags)
            .build();

        return client.chatStreaming(transactionId, gatewayRequest, context, handler);
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message the message to send
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(String message) {
        return chat(UserMessage.text(message));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, (ModelGatewayParameters) null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools));
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(messages, null, tools);
    }

    /**
     * Sends a chat request to the model using the provided messages and gateway parameters.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters gateway parameters to customize the output generation
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(List<ChatMessage> messages, ModelGatewayParameters parameters) {
        return chat(messages, parameters, null);
    }

    /**
     * Sends a chat request to the model using the provided messages, parameters, and tools.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters gateway parameters to customize the output generation
     * @param tools list of tools the model may call during generation
     * @return a {@link GatewayChatResponse} containing the model's reply
     */
    public GatewayChatResponse chat(List<ChatMessage> messages, ModelGatewayParameters parameters, List<Tool> tools) {
        return chat(
            ChatRequest.builder()
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
        return chatStreaming(messages, (ModelGatewayParameters) null, handler);
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
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayParameters parameters, ChatHandler handler) {
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
        return chatStreaming(messages, (ModelGatewayParameters) null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages and gateway parameters, delegating to a simple text consumer.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters gateway parameters for the chat invocation
     * @param handler a consumer that receives partial text responses
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayParameters parameters, Consumer<String> handler) {
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
        List<ChatMessage> messages, ModelGatewayParameters parameters, List<Tool> tools, Consumer<String> handler) {
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
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ModelGatewayParameters parameters, List<Tool> tools,
        ChatHandler handler) {
        var chatRequest = ChatRequest.builder()
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
     * ModelGatewayService modelGatewayService = ModelGatewayService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .modelId("gpt-4o")
     *     .build();
     *
     * GatewayChatResponse response = modelGatewayService.chat(
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
     * Builder class for constructing {@link ModelGatewayService} instances with configurable parameters.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {
        private String modelId;
        private MessageInterceptor messageInterceptor;
        private ToolInterceptor toolInterceptor;
        private BaseChatParameters defaultParameters;
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
         * Sets the default parameters applied to all chat requests when no per-request parameters are provided.
         * <p>
         * Accepts any {@link BaseChatParameters} - pass {@link ModelGatewayParameters} to configure gateway-only knobs, or generic
         * {@link com.ibm.watsonx.ai.chat.model.ChatParameters} when routing through the common {@link com.ibm.watsonx.ai.chat.ChatProvider}
         * interface. Per-request parameters take precedence over these defaults.
         *
         * @param parameters the default parameters to use
         */
        public Builder parameters(BaseChatParameters parameters) {
            this.defaultParameters = parameters;
            return this;
        }

        /**
         * Registers a {@link MessageInterceptor} used to modify or sanitize the assistant's textual content before it is returned to the caller.
         *
         * @param messageInterceptor the interceptor to apply
         */
        public Builder messageInterceptor(MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
            return this;
        }

        /**
         * Registers a {@link ToolInterceptor} to modify or normalize function call arguments before tool execution.
         *
         * @param toolInterceptor the interceptor to apply
         */
        public Builder toolInterceptor(ToolInterceptor toolInterceptor) {
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
         * Builds a {@link ModelGatewayService} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayService}
         */
        public ModelGatewayService build() {
            return new ModelGatewayService(this);
        }
    }
}
