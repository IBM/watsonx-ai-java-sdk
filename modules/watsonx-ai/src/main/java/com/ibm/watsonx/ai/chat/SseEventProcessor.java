/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.CompleteToolCallEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.ErrorEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialThinkingEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialToolCallEvent;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.streaming.StreamingStateTracker;
import com.ibm.watsonx.ai.chat.streaming.StreamingToolFetcher;
import com.ibm.watsonx.ai.core.Json;

/**
 * Processes Server-Sent Events.
 * <p>
 * The processor is stateful and thread-safe, designed to be used by a single streaming session.
 */
public class SseEventProcessor {
    private final Object usageLock = new Object();
    private volatile Map<Integer, String> finishReasons = new ConcurrentHashMap<>();
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
    private final Map<Integer, StringBuilder> contentBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, StringBuilder> thinkingBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, List<StreamingToolFetcher>> toolFetchers = new ConcurrentHashMap<>();
    private final StreamingStateTracker stateTracker;
    private final Map<String, Boolean> toolHasParameters;
    private final ExtractionTags extractionTags;

    /**
     * Sealed interface representing domain events emitted during SSE processing.
     * <p>
     * Each event type corresponds to a specific state change in the streaming session:
     * <ul>
     * <li>{@link PartialResponseEvent} - New content token received</li>
     * <li>{@link PartialThinkingEvent} - New thinking/reasoning token received</li>
     * <li>{@link PartialToolCallEvent} - Tool call arguments being streamed</li>
     * <li>{@link CompleteToolCallEvent} - Tool call fully assembled</li>
     * <li>{@link ErrorEvent} - Error occurred during processing</li>
     * </ul>
     */
    public static sealed interface CallbackEvent
        permits PartialResponseEvent, PartialThinkingEvent, PartialToolCallEvent, CompleteToolCallEvent, ErrorEvent {

        /** Event emitted when a partial response content token is received. */
        record PartialResponseEvent(String content, PartialChatResponse chunk) implements CallbackEvent {}

        /** Event emitted when a partial thinking/reasoning token is received. */
        record PartialThinkingEvent(String content, PartialChatResponse chunk) implements CallbackEvent {}

        /** Event emitted when tool call arguments are being streamed. */
        record PartialToolCallEvent(PartialToolCall toolCall) implements CallbackEvent {}

        /** Event emitted when a tool call has been fully assembled. */
        record CompleteToolCallEvent(CompletedToolCall completeToolCall) implements CallbackEvent {}

        /** Event emitted when an error occurs during chunk processing. */
        record ErrorEvent(Throwable error) implements CallbackEvent {}
    }

    /**
     * Result of processing a single SSE chunk.
     * <p>
     * Contains either:
     * <ul>
     * <li>A list of events to be dispatched (normal case)</li>
     * <li>An error that occurred during processing</li>
     * <li>An empty result (chunk was ignored, e.g., "event: close")</li>
     * </ul>
     *
     * @param events list of events generated from this chunk
     * @param hasError true if an error occurred
     * @param error the error that occurred, or null
     */
    public record ProcessResult(List<CallbackEvent> events, boolean hasError, Throwable error) {

        /** Creates an empty result (no events, no error). */
        public static ProcessResult empty() {
            return new ProcessResult(List.of(), false, null);
        }

        /** Creates a result with the given events. */
        public static ProcessResult events(List<CallbackEvent> events) {
            return new ProcessResult(events, false, null);
        }

        /** Creates an error result. */
        public static ProcessResult error(Throwable t) {
            return new ProcessResult(List.of(), true, t);
        }
    }

    /**
     * Creates a new ChatStreamProcessor for a streaming session.
     *
     * @param toolHasParameters map indicating which tools have parameters
     * @param extractionTags optional tags for extracting thinking content from the response
     */
    public SseEventProcessor(Map<String, Boolean> toolHasParameters, ExtractionTags extractionTags) {
        this.toolHasParameters = toolHasParameters;
        this.extractionTags = extractionTags;
        stateTracker = nonNull(extractionTags) ? new StreamingStateTracker(extractionTags) : null;
    }

    /**
     * Processes a single SSE chunk and returns the resulting events.
     *
     * @param partialMessage the raw SSE message (e.g., "data: {...}")
     * @return a {@link ProcessResult} containing events to dispatch or an error
     */
    public ProcessResult processChunk(String partialMessage) {
        if (isNull(partialMessage) || partialMessage.isBlank())
            return ProcessResult.empty();

        if (partialMessage.startsWith("event: error")) {
            pendingSSEError = true;
            return ProcessResult.empty();
        }

        if (partialMessage.startsWith("event: close"))
            return ProcessResult.empty();

        if (!partialMessage.startsWith("data:"))
            return ProcessResult.empty();

        var messageData = partialMessage.split("data: ")[1];

        if (pendingSSEError) {
            pendingSSEError = false;
            return ProcessResult.error(new RuntimeException(messageData));
        }

        var chunk = Json.fromJson(messageData, PartialChatResponse.class);
        var events = new ArrayList<CallbackEvent>();

        if (nonNull(chunk.usage())) {
            synchronized (usageLock) {
                chatUsage = chunk.usage();
            }
        }

        // Nothing to process.
        if (chunk.choices().size() == 0)
            return ProcessResult.empty();

        var message = chunk.choices().get(0);
        var messageIndex = message.index();
        var finishReason = finishReasons.get(messageIndex);
        var contentBuffer = contentBuffers.computeIfAbsent(messageIndex, StringBuilder::new);
        var thinkingBuffer = thinkingBuffers.computeIfAbsent(messageIndex, StringBuilder::new);

        if (isNull(created) && nonNull(chunk.created()))
            created = chunk.created();

        if (isNull(createdAt) && nonNull(chunk.createdAt()))
            createdAt = chunk.createdAt();

        if (isNull(id) && nonNull(chunk.id()))
            id = chunk.id();

        if (isNull(modelId) && nonNull(chunk.modelId()))
            modelId = chunk.modelId();

        if (isNull(object) && nonNull(chunk.object()))
            object = chunk.object();

        if (isNull(modelVersion) && nonNull(chunk.modelVersion()))
            modelVersion = chunk.modelVersion();

        if (isNull(model) && nonNull(chunk.model()))
            model = chunk.model();

        if (isNull(finishReason) && nonNull(message.finishReason())) {
            finishReason = message.finishReason();
            finishReasons.put(messageIndex, finishReason);
        }

        if (isNull(role) && nonNull(message.delta().role()))
            role = message.delta().role();

        if (isNull(refusal) && nonNull(message.delta().refusal()))
            refusal = message.delta().refusal();

        if (message.delta().toolCalls() != null) {

            StreamingToolFetcher toolFetcher;

            for (ToolCall deltaTool : message.delta().toolCalls()) {

                var toolIndex = deltaTool.index();
                var tools = toolFetchers.computeIfAbsent(messageIndex, ArrayList::new);

                // Check if there is an incomplete version of the TextChatToolCall object.
                if ((toolIndex + 1) > tools.size()) {
                    // First occurrence of the object, create it.
                    toolFetcher = new StreamingToolFetcher(id, messageIndex, toolIndex);
                    tools.add(toolFetcher);
                    if (toolIndex - 1 >= 0) {
                        events.add(new CompleteToolCallEvent(tools.get(toolIndex - 1).build()));
                    }
                } else {
                    // Incomplete version is present, complete it.
                    toolFetcher = tools.get(toolIndex);
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
                            new PartialToolCall(id, messageIndex, toolFetcher.getToolIndex(), toolFetcher.getId(), toolFetcher.getName(), arguments);
                        events.add(new PartialToolCallEvent(partialToolCall));
                    }
                }
            }
        }

        if (nonNull(message.delta().content())) {
            String token = message.delta().content();
            if (!token.isEmpty()) {
                contentBuffer.append(token);
                if (nonNull(stateTracker)) {
                    var r = stateTracker.update(token);
                    var content = r.content();
                    switch(r.state()) {
                        case RESPONSE, NO_THINKING -> content.ifPresent(c -> events.add(new PartialResponseEvent(c, chunk)));
                        case THINKING -> content.ifPresent(c -> {
                            thinkingBuffer.append(c);
                            events.add(new PartialThinkingEvent(c, chunk));
                        });
                        case START, UNKNOWN -> {}
                    }
                } else {
                    events.add(new PartialResponseEvent(token, chunk));
                }
            }
        }

        if (nonNull(message.delta().reasoningContent())) {

            String token = message.delta().reasoningContent();

            if (token.isEmpty())
                return ProcessResult.empty();

            thinkingBuffer.append(token);
            events.add(new PartialThinkingEvent(token, chunk));
        }

        if ("tool_calls".equals(finishReason)) {
            var tools = toolFetchers.get(messageIndex);
            events.add(new CompleteToolCallEvent(tools.get(tools.size() - 1).build()));
        }

        return ProcessResult.events(events);
    }

    /**
     * Builds the final {@link ChatResponse} from accumulated streaming data.
     *
     * @return the complete {@link ChatResponse}
     */
    public ChatResponse buildResponse() {

        var choices = contentBuffers.keySet().stream()
            .map(key -> {

                var content = contentBuffers.get(key).isEmpty() ? null : contentBuffers.get(key).toString().trim();
                var thinking = thinkingBuffers.get(key).isEmpty() ? null : thinkingBuffers.get(key).toString().trim();
                var tools = toolFetchers.get(key);

                var resultMessage = new ResultMessage(role, content, thinking, refusal,
                    nonNull(tools) && !tools.isEmpty()
                        ? tools.stream()
                            .map(StreamingToolFetcher::build)
                            .map(CompletedToolCall::toolCall)
                            .toList()
                        : null);

                return new ResultChoice(key, resultMessage, finishReasons.get(key));

            }).toList();


        return ChatResponse.build()
            .created(created)
            .createdAt(createdAt)
            .id(id)
            .modelId(modelId)
            .object(object)
            .model(model)
            .modelVersion(modelVersion)
            .extractionTags(extractionTags)
            .usage(chatUsage)
            .choices(choices)
            .build();
    }
}
