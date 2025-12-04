/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
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

/**
 * Subscriber abstraction for consuming raw SSE messages from the watsonx.ai Chat Streaming API.
 */
public interface ChatSubscriber {

    /**
     * Called when a new message chunk is received from the server.
     *
     * @param partialMessage the raw SSE event payload
     */
    void onNext(String partialMessage);

    /**
     * Called if an error occurs during streaming.
     *
     * @param throwable the error that occurred
     */
    void onError(Throwable throwable);

    /**
     * Called once the streaming session has completed successfully.
     */
    void onComplete();

    /**
     * Builds a map indicating whether each tool has parameters.
     *
     * @param tools the list of available {@link Tool}s
     * @return a map where keys are tool names and values indicate if the tool has parameters
     */
    static Map<String, Boolean> toolHasParameters(List<Tool> tools) {
        if (isNull(tools) || tools.size() == 0)
            return Map.of();

        return tools.stream().collect(toMap(
            tool -> tool.function().name(),
            Tool::hasParameters
        ));
    }

    /**
     * Handles an error by invoking the {@link ChatHandler}'s {@code onError} callback if the given throwable is non-null.
     *
     * @param t the {@link Throwable} to handle
     * @param handler the {@link ChatHandler} that should be notified of the error
     * @return always {@code null}, enabling direct use in async exception handlers
     */
    static Void handleError(Throwable t, ChatHandler handler) {
        ofNullable(t).map(Throwable::getCause).ifPresent(handler::onError);
        return null;
    }

    /**
     * Creates a {@link ChatSubscriber} that processes streamed chat responses from the watsonx.ai API.
     *
     * @param toolChoiceOption the tool choice strategy
     * @param toolHasParameters a map of tool names
     * @param extractionTags optional tags for identifying reasoning vs. final responses
     * @param handler the {@link ChatHandler} that will receive callbacks during streaming
     * @return a new {@link ChatSubscriber} instance for processing SSE events
     */
    static ChatSubscriber createSubscriber(
        String toolChoiceOption,
        Map<String, Boolean> toolHasParameters,
        ExtractionTags extractionTags,
        ChatHandler handler) {
        return new ChatSubscriber() {
            private final Object usageLock = new Object();
            private volatile String completionId;
            private volatile String finishReason;
            private volatile String role;
            private volatile String refusal;
            private volatile Long created;
            private volatile String createdAt;
            private volatile String id;
            private volatile String modelId;
            private volatile String object;
            private volatile String model;
            private volatile String modelVersion;
            private volatile boolean pendingSSEError = false;
            private ChatUsage chatUsage;
            private final StringBuffer contentBuffer = new StringBuffer();
            private final StringBuffer thinkingBuffer = new StringBuffer();
            private final List<StreamingToolFetcher> tools = Collections.synchronizedList(new ArrayList<>());
            private final StreamingStateTracker stateTracker = nonNull(extractionTags) ? new StreamingStateTracker(extractionTags) : null;
            private final ReentrantLock callbackLock = new ReentrantLock();

            @Override
            public void onNext(String partialMessage) {

                if (isNull(partialMessage) || partialMessage.isBlank())
                    return;

                if (partialMessage.startsWith("event: error")) {
                    pendingSSEError = true;
                    return;
                }

                if (partialMessage.startsWith("event: close"))
                    return;

                if (!partialMessage.startsWith("data:"))
                    return;

                var messageData = partialMessage.split("data: ")[1];

                if (pendingSSEError) {
                    pendingSSEError = false;
                    throw new RuntimeException(messageData);
                }

                var chunk = Json.fromJson(messageData, PartialChatResponse.class);

                // Last message get the "usage" values
                if (chunk.choices().size() == 0) {
                    synchronized (usageLock) {
                        chatUsage = chunk.usage();
                        return;
                    }
                }

                var message = chunk.choices().get(0);

                if (isNull(created) && nonNull(chunk.created()))
                    created = chunk.created();

                if (isNull(createdAt) && nonNull(chunk.createdAt()))
                    createdAt = chunk.createdAt();

                if (isNull(id) && nonNull(chunk.id())) {
                    id = chunk.id();
                    completionId = chunk.id();
                }

                if (isNull(modelId) && nonNull(chunk.modelId()))
                    modelId = chunk.modelId();

                if (isNull(object) && nonNull(chunk.object()))
                    object = chunk.object();

                if (isNull(modelVersion) && nonNull(chunk.modelVersion()))
                    modelVersion = chunk.modelVersion();

                if (isNull(model) && nonNull(chunk.model()))
                    model = chunk.model();

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
                            var toolFetcherToCall = tools.get(index - 1);
                            try {
                                callbackLock.lock();
                                var normalizedToolCall = ((InternalChatHandler) handler)
                                    .normalizeToolCall(new CompletedToolCall(completionId, toolFetcherToCall.build())).toolCall();
                                toolFetcherToCall.setId(normalizedToolCall.id());
                                toolFetcherToCall.setName(normalizedToolCall.function().name());
                                toolFetcherToCall.setArguments(normalizedToolCall.function().arguments());
                            } finally {
                                callbackLock.unlock();
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
                        var arguments = isNull(toolHasParameter) || toolHasParameter ? deltaTool.function().arguments() : "{}";

                        if (!arguments.isEmpty()) {
                            var partialToolCall =
                                new PartialToolCall(completionId, toolFetcher.getIndex(), toolFetcher.getId(), toolFetcher.getName(), arguments);
                            try {
                                callbackLock.lock();
                                handler.onPartialToolCall(partialToolCall);
                            } finally {
                                callbackLock.unlock();
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

                        switch(r.state()) {
                            case RESPONSE, NO_THINKING -> content.ifPresent(c -> {
                                try {
                                    callbackLock.lock();
                                    handler.onPartialResponse(c, chunk);
                                } finally {
                                    callbackLock.unlock();
                                }
                            });
                            case THINKING -> content.ifPresent(c -> {
                                thinkingBuffer.append(c);
                                try {
                                    callbackLock.lock();
                                    handler.onPartialThinking(c, chunk);
                                } finally {
                                    callbackLock.unlock();
                                }
                            });
                            case START, UNKNOWN -> {}
                        }
                    } else {
                        try {
                            callbackLock.lock();
                            handler.onPartialResponse(token, chunk);
                        } finally {
                            callbackLock.unlock();
                        }
                    }
                }

                if (nonNull(message.delta().reasoningContent())) {

                    String token = message.delta().reasoningContent();

                    if (token.isEmpty())
                        return;

                    thinkingBuffer.append(token);

                    try {
                        callbackLock.lock();
                        handler.onPartialThinking(token, chunk);
                    } finally {
                        callbackLock.unlock();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    callbackLock.lock();
                    handler.onError(throwable);
                } finally {
                    callbackLock.unlock();
                }
            }

            @Override
            public void onComplete() {
                try {

                    List<ToolCall> toolCalls = null;
                    String content = contentBuffer.isEmpty() ? null : contentBuffer.toString();
                    String thinking = thinkingBuffer.toString();

                    if (nonNull(finishReason) && finishReason.equals("tool_calls")) {
                        toolCalls = tools.stream()
                            .map(StreamingToolFetcher::build)
                            .collect(Collectors.toList());

                        try {
                            callbackLock.lock();
                            var normalizedToolCall =
                                ((InternalChatHandler) handler)
                                    .normalizeToolCall(new CompletedToolCall(completionId, toolCalls.get(toolCalls.size() - 1))).toolCall();
                            toolCalls.set(toolCalls.size() - 1, normalizedToolCall);
                        } finally {
                            callbackLock.unlock();
                        }
                    }

                    var resultMessage = new ResultMessage(role, content, thinking, refusal, isNull(toolCalls) ? null : List.copyOf(toolCalls));

                    try {
                        callbackLock.lock();
                        handler.onCompleteResponse(
                            ChatResponse.build()
                                .created(created)
                                .createdAt(createdAt)
                                .id(id)
                                .modelId(modelId)
                                .object(object)
                                .model(model)
                                .modelVersion(modelVersion)
                                .extractionTags(extractionTags)
                                .usage(chatUsage)
                                .choices(List.of(new ResultChoice(0, resultMessage, finishReason)))
                                .build()
                        );
                    } finally {
                        callbackLock.unlock();
                    }

                } catch (RuntimeException e) {
                    onError(e);
                }
            }
        };
    }
}
