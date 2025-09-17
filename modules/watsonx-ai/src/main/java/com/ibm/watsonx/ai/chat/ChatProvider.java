/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.util.StreamingStateTracker;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher;
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
     * <p>
     * This subscriber is designed to be thread-safe for a single streaming request. It correctly handles state aggregation and memory visibility for
     * its internal fields.
     * <p>
     * <b>Thread Safety Considerations:</b>
     * <p>
     * This implementation guarantees that all callback methods on the provided {@code ChatHandler} instance will be invoked sequentially, even if the
     * {@code ChatHandler} is shared across multiple concurrent streaming requests. This is achieved by using a synchronized lock on the handler
     * instance itself, which serializes access to its methods.
     *
     * @param toolChoiceOption the tool choice strategy used during generation
     * @param toolHasParameters A map indicating whether each tool requires parameters
     * @param extractionTags the {@link ExtractionTags} used to extract the thinking/responses during streaming
     * @param handler the {@link ChatHandler} instance to receive streaming callbacks.
     *
     * @return a {@link Flow.Subscriber} capable of consuming {@code String} events representing streamed chat responses
     */
    public default Flow.Subscriber<String> subscriber(
        String toolChoiceOption, // Read-only
        Map<String, Boolean> toolHasParameters, // Read-only
        ExtractionTags extractionTags, // Read-only
        ChatHandler handler) {
        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile String completionId;
            private volatile String finishReason;
            private volatile String role;
            private volatile String refusal;
            private volatile boolean success = true;
            private volatile boolean pendingSSEError = false;
            private final StringBuffer contentBuffer = new StringBuffer();
            private final StringBuffer thinkingBuffer = new StringBuffer();
            private final ChatResponse chatResponse = new ChatResponse();
            private final List<StreamingToolFetcher> tools = Collections.synchronizedList(new ArrayList<>());
            private final StreamingStateTracker stateTracker = nonNull(extractionTags) ? new StreamingStateTracker(extractionTags) : null;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {

                try {

                    if (isNull(partialMessage) || partialMessage.isBlank())
                        return;

                    if (partialMessage.startsWith("event: error")) {
                        pendingSSEError = true;
                        return;
                    }

                    if (!partialMessage.startsWith("data:"))
                        return;

                    var messageData = partialMessage.split("data: ")[1];

                    if (pendingSSEError) {
                        pendingSSEError = false;
                        throw new RuntimeException(messageData);
                    }

                    // TODO: Use the ExecutorProvider.cpuExecutor().
                    var chunk = Json.fromJson(messageData, PartialChatResponse.class);

                    synchronized (chatResponse) {
                        // Last message get the "usage" values
                        if (chunk.choices().size() == 0) {
                            chatResponse.setUsage(chunk.usage());
                            return;
                        }
                    }

                    var message = chunk.choices().get(0);

                    synchronized (chatResponse) {

                        if (isNull(chatResponse.getCreated()) && nonNull(chunk.created()))
                            chatResponse.setCreated(chunk.created());

                        if (isNull(chatResponse.getCreatedAt()) && nonNull(chunk.createdAt()))
                            chatResponse.setCreatedAt(chunk.createdAt());

                        if (isNull(chatResponse.getId()) && nonNull(chunk.id())) {
                            chatResponse.setId(chunk.id());
                            completionId = chunk.id();
                        }

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
                    }

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
                                synchronized (handler) {
                                    handler.onCompleteToolCall(new CompletedToolCall(completionId, tool));
                                }
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
                                    new PartialToolCall(completionId, toolFetcher.getIndex(), toolFetcher.getId(), toolFetcher.getName(), arguments);
                                synchronized (handler) {
                                    handler.onPartialToolCall(partialToolCall);
                                }
                            }
                        }
                    }

                    if (nonNull(message.delta().content())) {

                        String token = message.delta().content();

                        if (token.isEmpty())
                            return;

                        contentBuffer.append(token);

                        if (nonNull(stateTracker)) {
                            var r = stateTracker.update(token);
                            var content = r.content();
                            synchronized (handler) {
                                switch(r.state()) {
                                    case RESPONSE, NO_THINKING -> content.ifPresent(c -> {
                                        handler.onPartialResponse(c, chunk);
                                    });
                                    case THINKING -> content.ifPresent(c -> {
                                        thinkingBuffer.append(c);
                                        handler.onPartialThinking(c, chunk);
                                    });
                                    case START, UNKNOWN -> {}
                                }
                            }
                        } else {
                            synchronized (handler) {
                                handler.onPartialResponse(token, chunk);
                            }
                        }
                    }

                    if (nonNull(message.delta().reasoningContent())) {

                        String token = message.delta().reasoningContent();

                        if (token.isEmpty())
                            return;

                        thinkingBuffer.append(token);
                        handler.onPartialThinking(token, chunk);
                    }

                } catch (RuntimeException e) {
                    onError(e);
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
                synchronized (handler) {
                    handler.onError(throwable);
                }
            }

            @Override
            public void onComplete() {
                try {

                    List<ToolCall> toolCalls = null;
                    String content = contentBuffer.toString();
                    String thinking = thinkingBuffer.toString();

                    if (nonNull(finishReason) && finishReason.equals("tool_calls")) {
                        content = null;
                        toolCalls = tools.stream()
                            .map(StreamingToolFetcher::build)
                            .toList();
                        synchronized (handler) {
                            handler.onCompleteToolCall(new CompletedToolCall(completionId, toolCalls.get(toolCalls.size() - 1)));
                        }
                    }

                    var resultMessage = new ResultMessage(role, content, thinking, refusal, toolCalls);

                    synchronized (chatResponse) {
                        chatResponse.setExtractionTags(extractionTags);
                        chatResponse.setChoices(List.of(new ResultChoice(0, resultMessage, finishReason)));
                    }

                    synchronized (handler) {
                        handler.onCompleteResponse(chatResponse);
                    }

                } catch (RuntimeException e) {
                    onError(e);
                }
            }
        };
    }
}
