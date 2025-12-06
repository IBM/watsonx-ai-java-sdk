/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
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
 *     .modelId("ibm/granite-4-h-small")
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
public class ChatService extends ModelService implements ChatProvider {
    public static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatRestClient client;
    private final MessageInterceptor messageInterceptor;
    private final ToolInterceptor toolInterceptor;
    private final ChatProvider chatProvider;

    private ChatService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticationProvider(), "authenticationProvider cannot be null");
        client = ChatRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.authenticationProvider())
            .build();
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
        if (nonNull(messageInterceptor) || nonNull(toolInterceptor)) {
            chatProvider = new Builder()
                .authenticationProvider(builder.authenticationProvider())
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .modelId(modelId)
                .projectId(projectId)
                .spaceId(spaceId)
                .timeout(timeout)
                .version(version)
                .build();
        } else
            chatProvider = null;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        if (nonNull(chatRequest.deploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;
        var parameters = requireNonNullElse(chatRequest.parameters(), ChatParameters.builder().build());

        if (messages.stream().anyMatch(ControlMessage.class::isInstance)
            && (isNull(chatRequest.thinking()) || isNull(chatRequest.thinking().extractionTags())))
            throw new IllegalArgumentException("Extraction tags are required when using control messages");

        var projectSpace = resolveProjectSpace(parameters);
        var projectId = projectSpace.projectId();
        var spaceId = projectSpace.spaceId();
        var modelId = requireNonNullElse(parameters.modelId(), this.modelId);
        var timeout = requireNonNullElse(parameters.timeLimit(), this.timeout.toMillis());

        Boolean includeReasoning = null;
        String thinkingEffort = null;
        ExtractionTags extractionTags = null;
        Map<String, Object> chatTemplateKwargs = null;
        if (nonNull(chatRequest.thinking())) {
            var thinking = chatRequest.thinking();
            extractionTags = thinking.extractionTags();
            includeReasoning = thinking.includeReasoning();
            thinkingEffort = nonNull(thinking.thinkingEffort()) ? thinking.thinkingEffort().getValue() : null;
            if (nonNull(thinking.enabled()))
                chatTemplateKwargs = Map.of("thinking", thinking.enabled());
        }

        var textChatRequest = TextChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .toolChoiceOption(parameters.toolChoiceOption())
            .toolChoice(parameters.toolChoice())
            .frequencyPenalty(parameters.frequencyPenalty())
            .logitBias(parameters.logitBias())
            .logprobs(parameters.logprobs())
            .topLogprobs(parameters.topLogprobs())
            .maxCompletionTokens(parameters.maxCompletionTokens())
            .n(parameters.n())
            .presencePenalty(parameters.presencePenalty())
            .seed(parameters.seed())
            .stop(parameters.stop())
            .temperature(parameters.temperature())
            .topP(parameters.topP())
            .responseFormat(parameters.responseFormat())
            .jsonSchema(parameters.jsonSchema())
            .context(parameters.context())
            .timeLimit(parameters.timeLimit())
            .guidedChoice(parameters.guidedChoice())
            .guidedRegex(parameters.guidedRegex())
            .guidedGrammar(parameters.guidedGrammar())
            .repetitionPenalty(parameters.repetitionPenalty())
            .lengthPenalty(parameters.lengthPenalty())
            .timeLimit(timeout)
            .includeReasoning(includeReasoning)
            .reasoningEffort(thinkingEffort)
            .chatTemplateKwargs(chatTemplateKwargs)
            .build();

        var chatResponse = client.chat(parameters.transactionId(), textChatRequest);

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

        return chatResponse.toBuilder()
            .extractionTags(extractionTags)
            .build();
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        if (nonNull(chatRequest.deploymentId()))
            logger.info("The deploymentId parameter can not be used with the ChatService. Use the DeploymentService instead");

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;
        var parameters = requireNonNullElse(chatRequest.parameters(), ChatParameters.builder().build());

        if (messages.stream().anyMatch(ControlMessage.class::isInstance)
            && (isNull(chatRequest.thinking()) || isNull(chatRequest.thinking().extractionTags())))
            throw new IllegalArgumentException("Extraction tags are required when using control messages");

        var projectSpace = resolveProjectSpace(parameters);
        var projectId = projectSpace.projectId();
        var spaceId = projectSpace.spaceId();
        var modelId = requireNonNullElse(parameters.modelId(), this.modelId);
        var timeout = requireNonNullElse(parameters.timeLimit(), this.timeout.toMillis());

        Boolean includeReasoning = null;
        String thinkingEffort = null;
        ExtractionTags extractionTags = null;
        Map<String, Object> chatTemplateKwargs = null;
        if (nonNull(chatRequest.thinking())) {
            var thinking = chatRequest.thinking();
            extractionTags = thinking.extractionTags();
            includeReasoning = thinking.includeReasoning();
            thinkingEffort = nonNull(thinking.thinkingEffort()) ? thinking.thinkingEffort().getValue() : null;
            if (nonNull(thinking.enabled()))
                chatTemplateKwargs = Map.of("thinking", thinking.enabled());
        }

        var textChatRequest = TextChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .toolChoiceOption(parameters.toolChoiceOption())
            .toolChoice(parameters.toolChoice())
            .frequencyPenalty(parameters.frequencyPenalty())
            .logitBias(parameters.logitBias())
            .logprobs(parameters.logprobs())
            .topLogprobs(parameters.topLogprobs())
            .maxCompletionTokens(parameters.maxCompletionTokens())
            .n(parameters.n())
            .presencePenalty(parameters.presencePenalty())
            .seed(parameters.seed())
            .stop(parameters.stop())
            .temperature(parameters.temperature())
            .topP(parameters.topP())
            .responseFormat(parameters.responseFormat())
            .jsonSchema(parameters.jsonSchema())
            .context(parameters.context())
            .timeLimit(parameters.timeLimit())
            .guidedChoice(parameters.guidedChoice())
            .guidedRegex(parameters.guidedRegex())
            .guidedGrammar(parameters.guidedGrammar())
            .repetitionPenalty(parameters.repetitionPenalty())
            .lengthPenalty(parameters.lengthPenalty())
            .timeLimit(timeout)
            .includeReasoning(includeReasoning)
            .reasoningEffort(thinkingEffort)
            .chatTemplateKwargs(chatTemplateKwargs)
            .build();

        return client.chatStreaming(
            parameters.transactionId(),
            extractionTags,
            textChatRequest,
            new InternalChatHandler(new InterceptorContext(chatProvider, chatRequest, null), handler, toolInterceptor)
        );
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
    public final static class Builder extends ModelService.Builder<Builder> {
        private MessageInterceptor messageInterceptor;
        private ToolInterceptor toolInterceptor;

        private Builder() {}

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
         * @param toolInterceptor the interceptor to apply (may be {@code null})
         */
        public Builder toolInterceptor(ToolInterceptor toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
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