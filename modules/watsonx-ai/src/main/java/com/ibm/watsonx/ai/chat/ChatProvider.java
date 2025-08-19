/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.util.StreamingStateTracker;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher.PartialToolCall;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Interface representing a provider capable of executing chat interactions with language models.
 *
 * @see ChatService
 * @see DeploymentService
 */
public interface ChatProvider {

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public default ChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages));
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */

    public default ChatResponse chat(List<ChatMessage> messages) {
        return chat(
            ChatRequest.builder()
                .messages(messages).build()
        );
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
    public default ChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(
            ChatRequest.builder()
                .messages(messages)
                .tools(tools)
                .build()
        );
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
    public default ChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools));
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
    public default ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters) {
        return chat(
            ChatRequest.builder()
                .messages(messages)
                .parameters(parameters)
                .build()
        );
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
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatHandler handler) {
        var chatRequest = ChatRequest.builder()
            .messages(messages)
            .build();
        return chatStreaming(chatRequest, handler);
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
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatHandler handler) {
        var chatRequest = ChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .build();
        return chatStreaming(chatRequest, handler);
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
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, ChatHandler handler) {
        var chatRequest = ChatRequest.builder()
            .messages(messages)
            .parameters(parameters)
            .build();
        return chatStreaming(chatRequest, handler);
    }

    /**
     * Sends a chat request to the model using the provided messages, tools, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models, and customize the generation behavior via {@link ChatParameters}.
     *
     * @param chatRequest the chat request
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(ChatRequest chatRequest);

    /**
     * Sends a streaming chat request using the provided messages, tools, and parameters.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param chatRequest the chat request
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler);

    /**
     * Handles an error by invoking the {@link ChatHandler}'s {@code onError} callback if the given throwable is non-null.
     *
     * @param t the {@link Throwable} to handle
     * @param handler the {@link ChatHandler} that should be notified of the error
     * @return always {@code null}, enabling direct use in async exception handlers
     */
    public default Void handlerError(Throwable t, ChatHandler handler) {
        ofNullable(t).map(Throwable::getCause).ifPresent(handler::onError);
        return null;
    }

    /**
     * Returns a {@link Flow.Subscriber} implementation that processes streaming chat responses.
     *
     * @param toolChoiceOption the tool choice strategy used during generation
     * @param toolHasParameters A map indicating whether each tool requires parameters.
     * @param stateTracker the {@link StreamingStateTracker} instance responsible for tracking XML-like tag states during streaming
     * @param handler the {@link ChatHandler} instance to receive streaming callbacks
     * @return a {@link Flow.Subscriber} capable of consuming {@code String} events representing streamed chat responses
     */
    public default Flow.Subscriber<String> subscriber(String toolChoiceOption, Map<String, Boolean> toolHasParameters,
        StreamingStateTracker stateTracker, ChatHandler handler) {
        return new Flow.Subscriber<String>() {
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

                        // Watsonx doesn't return "tool_calls".
                        // Open an issue.
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

                            if (index - 1 >= 0) {
                                var tool = tools.get(index - 1).build();
                                handler.onCompleteToolCall(tool);
                            }

                        } else {
                            // Incomplete version is present, complete it.
                            toolFetcher = tools.get(index);
                        }

                        toolFetcher.setId(deltaTool.id());

                        if (nonNull(deltaTool.function())) {
                            toolFetcher.setName(deltaTool.function().name());
                            toolFetcher.appendArguments(deltaTool.function().arguments());

                            // There is a bug in the Streaming API: it does not return an empty object for tools without arguments.
                            // Open an issue.
                            var toolHasParameter = toolHasParameters.get(toolFetcher.getName());
                            var arguments = toolHasParameter ? deltaTool.function().arguments() : "{}";

                            if (!arguments.isEmpty()) {
                                var partialToolCall =
                                    new PartialToolCall(toolFetcher.getIndex(), toolFetcher.getId(), toolFetcher.getName(), arguments);
                                handler.onPartialToolCall(partialToolCall);
                            }
                        }
                    }

                    if (nonNull(message.delta().content())) {

                        String token = message.delta().content();

                        if (token.isEmpty())
                            return;

                        stringBuilder.append(token);

                        if (nonNull(stateTracker)) {
                            var r = stateTracker.update(token);
                            var content = r.content();
                            switch(r.state()) {
                                case RESPONSE -> content.ifPresent(c -> handler.onPartialResponse(c, chunk));
                                case THINKING -> content.ifPresent(c -> handler.onPartialThinking(c, chunk));
                                case UNKNOWN -> {}
                            }
                        } else {
                            handler.onPartialResponse(token, chunk);
                        }
                    }

                } catch (RuntimeException e) {
                    handler.onError(e);
                    success = !handler.failOnFirstError();
                } finally {
                    if (success) {
                        subscription.request(1);
                    } else {
                        subscription.cancel();
                    }
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
                        handler.onCompleteToolCall(toolCalls.get(toolCalls.size() - 1));
                    }

                    var resultMessage = new ResultMessage(role, content, refusal, toolCalls);
                    chatResponse.setChoices(List.of(new ResultChoice(0, resultMessage, finishReason)));
                    handler.onCompleteResponse(chatResponse);

                } catch (RuntimeException e) {
                    handler.onError(e);
                }
            }
        };
    }
}
