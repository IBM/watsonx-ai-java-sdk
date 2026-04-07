/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.CryptoService;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with IBM watsonx.ai Text Chat APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ChatService chatService = ChatService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("project-id")
 *     .modelId("ibm/granite-4-h-small")
 *     .build();
 *
 * ChatResponse response = chatService.chat(
 *     SystemMessage.of("You are a helpful assistant"),
 *     UserMessage.text("Tell me a joke")
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ChatService extends CryptoService implements ChatProvider {
    public static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatRestClient client;
    private final MessageInterceptor messageInterceptor;
    private final ToolInterceptor toolInterceptor;
    private final ChatProvider chatProvider;
    private final ChatParameters defaultParameters;
    private final List<Tool> defaultTools;

    private ChatService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
        defaultTools = builder.defaultTools;

        var defaultParametersBuilder = nonNull(builder.defaultParameters)
            ? builder.defaultParameters.toBuilder()
                .timeLimit(nonNull(builder.defaultParameters.timeLimit()) ? Duration.ofMillis(builder.defaultParameters.timeLimit()) : timeout)
            : ChatParameters.builder()
                .timeLimit(timeout);

        defaultParameters = defaultParametersBuilder.modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .build();

        client = ChatRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .authenticator(builder.authenticator())
            .build();

        if (nonNull(messageInterceptor) || nonNull(toolInterceptor)) {
            chatProvider = new Builder()
                .authenticator(builder.authenticator())
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .modelId(modelId)
                .projectId(projectId)
                .spaceId(spaceId)
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
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        if (nonNull(chatRequest.deploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

        var textChatRequest = ChatUtility.buildTextChatRequest(chatRequest, defaultParameters);
        var extractionTags = nonNull(chatRequest.thinking()) ? chatRequest.thinking().extractionTags() : null;
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var chatResponse = client.chat(transactionId, textChatRequest);

        if (nonNull(messageInterceptor)) {
            var newChoices = messageInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        if (nonNull(toolInterceptor)) {
            var newChoices = toolInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        var response = chatResponse.toBuilder();

        // For certain models, watsonx.ai does not return FinishReason.TOOL_CHOICE when ToolChoiceOption.REQUIRED is set
        if (ToolChoiceOption.REQUIRED.value().equals(textChatRequest.toolChoiceOption()))
            response.choices(
                chatResponse.choices().stream()
                    .map(resultChoice -> resultChoice.withFinishReason(FinishReason.TOOL_CALLS))
                    .toList()
            );

        return response.extractionTags(extractionTags).build();
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        if (nonNull(chatRequest.deploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

        var textChatRequest = ChatUtility.buildTextChatRequest(chatRequest, defaultParameters);
        var extractionTags = nonNull(chatRequest.thinking()) ? chatRequest.thinking().extractionTags() : null;
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var context = ChatClientContext.builder()
            .chatProvider(chatProvider)
            .chatRequest(chatRequest)
            .toolInterceptor(toolInterceptor)
            .extractionTags(extractionTags)
            .build();

        return client.chatStreaming(transactionId, textChatRequest, context, handler);
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
        return chat(messages, parameters, null);
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
                .tools(isNull(tools) ? defaultTools : tools)
                .build()
        );
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message Message to send.
     * @param handler a {@link ChatHandler} implementation
     */
    public CompletableFuture<ChatResponse> chatStreaming(String message, ChatHandler handler) {
        return chatStreaming(List.of(UserMessage.text(message)), handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a {@link ChatHandler} implementation
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ChatHandler handler) {
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
     * @param handler a {@link ChatHandler} implementation
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatHandler handler) {
        return chatStreaming(messages, null, tools, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, ChatHandler handler) {
        return chatStreaming(messages, parameters, null, handler);
    }

    /**
     * Sends a chat request to the model using the provided message.
     *
     * @param message Message to send.
     * @param handler a consumer that receives partial text responses
     */
    public CompletableFuture<ChatResponse> chatStreaming(String message, Consumer<String> handler) {
        return chatStreaming(List.of(UserMessage.text(message)), handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a consumer that receives partial text responses
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, Consumer<String> handler) {
        return chatStreaming(messages, ChatParameters.builder().build(), handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a consumer that receives partial text responses
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, Consumer<String> handler) {
        return chatStreaming(messages, parameters, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param handler a consumer that receives partial text responses
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, List<Tool> tools, Consumer<String> handler) {
        return chatStreaming(messages, null, tools, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param tools the list of tools that the model may use
     * @param handler a consumer that receives partial text responses
     */
    public CompletableFuture<ChatResponse> chatStreaming(
        List<ChatMessage> messages, ChatParameters parameters, List<Tool> tools, Consumer<String> handler) {
        return chatStreaming(messages, parameters, tools, new ChatHandler() {
            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                handler.accept(partialResponse);
            }
        });
    }

    /**
     * Sends a streaming chat request using the provided messages.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation
     */
    public CompletableFuture<ChatResponse> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, List<Tool> tools,
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
     * ChatService chatService = ChatService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("project-id")
     *     .modelId("ibm/granite-4-h-small")
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
    public final static class Builder extends CryptoService.Builder<Builder> {
        private MessageInterceptor messageInterceptor;
        private ToolInterceptor toolInterceptor;
        private ChatParameters defaultParameters;
        private List<Tool> defaultTools;

        private Builder() {}

        /**
         * Sets the default {@link ChatParameters} that will be applied to all chat requests when no specific parameters are provided.
         * <p>
         * These default values serve as fallbacks for any parameter not explicitly set. When parameters are provided in the chat method call, they
         * will take precedence over these default parameters.
         *
         * @param parameters the default chat parameters to use
         */
        public Builder parameters(ChatParameters parameters) {
            this.defaultParameters = parameters;
            return this;
        }

        /**
         * Registers a {@link MessageInterceptor} used to modify or sanitize the assistant's textual content before it is returned to the caller.
         * <p>
         * This interceptor is invoked on the final aggregated content (non-streaming responses only), allowing adjustments such as rewriting,
         * filtering, or normalization.
         * <p>
         * <b>Example:</b>
         *
         * <pre>{@code
         * ChatService.builder()
         *     .messageInterceptor((request, content) -> content.replace("error", "issue"));
         * }</pre>
         *
         * @param messageInterceptor the interceptor to apply
         */
        public Builder messageInterceptor(MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
            return this;
        }

        /**
         * Registers a {@link ToolInterceptor} to modify or normalize function call arguments before tool execution.
         * <p>
         * This interceptor is applied to every function call appearing in the assistant message. It is invoked both for standard responses and for
         * aggregated tool-call data in streaming mode.
         * <p>
         * <b>Example:</b>
         *
         * <pre>{@code
         * ChatService.builder()
         *     .toolInterceptor((request, fc) -> {
         *         var args = fc.arguments();
         *         return (nonNull(args) && args.startsWith("\""))
         *             ? fc.withArguments(Json.fromJson(args, String.class))
         *             : fc;
         *     });
         * }</pre>
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
         * <p>
         * These tools will be used in all chat requests unless explicitly overridden by passing tools directly to the chat methods. If tools are
         * provided in the chat method call, they will take precedence over these default tools.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(Tool... tools) {
            return tools(Arrays.asList(tools));
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         * <p>
         * These tools will be used in all chat requests unless explicitly overridden by passing tools directly to the chat methods. If tools are
         * provided in the chat method call, they will take precedence over these default tools.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(List<Tool> tools) {
            this.defaultTools = tools;
            return this;
        }

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