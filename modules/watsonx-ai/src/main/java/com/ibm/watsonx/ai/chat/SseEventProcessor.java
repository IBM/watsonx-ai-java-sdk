/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.CompleteToolCallEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.ErrorEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialThinkingEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialToolCallEvent;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionEntry;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.streaming.StreamingStateTracker;
import com.ibm.watsonx.ai.chat.streaming.StreamingToolFetcher;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatResponse;

/**
 * Processes Server-Sent Events.
 * <p>
 * The processor is stateful and thread-safe, designed to be used by a single streaming session.
 */
public class SseEventProcessor {
    private volatile Map<Integer, String> finishReasons = new ConcurrentHashMap<>();
    private final Map<Integer, String> roles = new ConcurrentHashMap<>();
    private final Map<Integer, StringBuilder> refusalBuffers = new ConcurrentHashMap<>();
    private volatile Long created;
    private volatile String createdAt;
    private volatile String id;
    private volatile String modelId;
    private volatile String object;
    private volatile String model;
    private volatile String modelVersion;
    private volatile boolean pendingSSEError = false;
    private volatile ChatUsage chatUsage;
    private volatile Map<String, List<ModerationResult>> moderations;
    private volatile Map<String, List<DetectionEntry>> detections;
    private volatile String serviceTier;
    private volatile String systemFingerprint;
    private volatile Boolean cached;
    private final Map<Integer, StringBuilder> contentBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, StringBuilder> thinkingBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, List<StreamingToolFetcher>> toolFetchers = new ConcurrentHashMap<>();
    private final Map<Integer, StreamingStateTracker> stateTrackers = new ConcurrentHashMap<>();
    private final ExtractionTags extractionTags;
    private final Supplier<TextChatResponse.Builder<?>> responseBuilderFactory;

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

        /**
         * Event emitted when a partial response content token is received.
         *
         * @param content the newly received content token
         * @param chunk the partial chat response this token belongs to
         */
        record PartialResponseEvent(String content, PartialChatResponse chunk) implements CallbackEvent {}

        /**
         * Event emitted when a partial thinking/reasoning token is received.
         *
         * @param content the newly received thinking/reasoning token
         * @param chunk the partial chat response this token belongs to
         */
        record PartialThinkingEvent(String content, PartialChatResponse chunk) implements CallbackEvent {}

        /**
         * Event emitted when tool call arguments are being streamed.
         *
         * @param toolCall the partial tool call being assembled
         */
        record PartialToolCallEvent(PartialToolCall toolCall) implements CallbackEvent {}

        /**
         * Event emitted when a tool call has been fully assembled.
         *
         * @param completeToolCall the fully assembled tool call
         */
        record CompleteToolCallEvent(CompletedToolCall completeToolCall) implements CallbackEvent {}

        /**
         * Event emitted when an error occurs during chunk processing.
         *
         * @param error the error that occurred
         */
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

        public ProcessResult {
            events = isNull(events) ? null : List.copyOf(events);
        }

        /**
         * Creates an empty result (no events, no error).
         */
        public static ProcessResult empty() {
            return new ProcessResult(List.of(), false, null);
        }

        /**
         * Creates a result with the given events.
         */
        public static ProcessResult events(List<CallbackEvent> events) {
            return new ProcessResult(events, false, null);
        }

        /**
         * Creates an error result.
         */
        public static ProcessResult error(Throwable t) {
            return new ProcessResult(List.of(), true, t);
        }
    }

    /**
     * Creates a new SseEventProcessor for a streaming session, using the given factory to build the final response.
     *
     * @param tools the list of available {@link Tool}s
     * @param extractionTags optional tags for extracting thinking content from the response
     * @param responseBuilderFactory supplies the builder used by {@link #buildResponse()}
     */
    public SseEventProcessor(List<Tool> tools, ExtractionTags extractionTags, Supplier<TextChatResponse.Builder<?>> responseBuilderFactory) {
        this.extractionTags = extractionTags;
        this.responseBuilderFactory = responseBuilderFactory;
    }

    /**
     * Processes a single SSE chunk and returns the resulting events.
     * <p>
     * SSE errors and processing failures mark unfinished choices as failed, preserving their partial data without completing their tool calls.
     *
     * @param partialMessage the raw SSE message (e.g., "data: {...}")
     * @return a {@link ProcessResult} containing events to dispatch or an error
     */
    public ProcessResult processChunk(String partialMessage) {
        try {
            return processMessage(partialMessage);
        } catch (RuntimeException e) {
            markIncompleteChoicesFailed();
            throw e;
        }
    }

    private ProcessResult processMessage(String partialMessage) {
        if (isNull(partialMessage) || partialMessage.isBlank())
            return ProcessResult.empty();

        if (partialMessage.startsWith("event:")) {
            var eventType = partialMessage.substring("event:".length());
            if (eventType.startsWith(" "))
                eventType = eventType.substring(1);
            pendingSSEError = "error".equals(eventType);
            if (pendingSSEError)
                markIncompleteChoicesFailed();
            return ProcessResult.empty();
        }

        if (!partialMessage.startsWith("data:"))
            return ProcessResult.empty();

        var messageData = partialMessage.substring("data:".length());
        if (messageData.startsWith(" "))
            messageData = messageData.substring(1);

        if (pendingSSEError) {
            pendingSSEError = false;
            return ProcessResult.error(new RuntimeException(messageData.isBlank() ? "SSE error event without data" : messageData));
        }

        if (messageData.isBlank())
            return ProcessResult.empty();

        // OpenAI-compatible endpoints (Model Gateway) terminate the stream with a "data: [DONE]" sentinel.
        if ("[DONE]".equals(messageData))
            return completePendingToolCalls();

        var chunk = Json.fromJson(messageData, PartialChatResponse.class);
        var events = new ArrayList<CallbackEvent>();

        if (nonNull(chunk.usage()))
            chatUsage = chunk.usage();

        if (isNull(serviceTier) && nonNull(chunk.serviceTier()))
            serviceTier = chunk.serviceTier();

        if (isNull(systemFingerprint) && nonNull(chunk.systemFingerprint()))
            systemFingerprint = chunk.systemFingerprint();

        if (isNull(cached) && nonNull(chunk.cached()))
            cached = chunk.cached();

        if (nonNull(chunk.moderations()) && !chunk.moderations().isEmpty())
            moderations = mergeModerations(moderations, chunk.moderations());

        if (nonNull(chunk.detections()) && !chunk.detections().isEmpty())
            detections = mergeDetections(detections, chunk.detections());

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

        // A trailing usage chunk can be the only terminal signal from some models.
        if (isNull(chunk.choices()) || chunk.choices().isEmpty())
            return completePendingToolCalls();

        for (var message : chunk.choices()) {
            var choiceChunk = chunk.choices().size() == 1 ? chunk
                : new PartialChatResponse(chunk.id(), chunk.object(), chunk.modelId(), chunk.model(), List.of(message), chunk.created(),
                    chunk.modelVersion(), chunk.createdAt(), chunk.usage(), chunk.moderations(), chunk.detections(), chunk.serviceTier(),
                    chunk.systemFingerprint(), chunk.cached());
            processChoice(choiceChunk, message, events);
        }
        return ProcessResult.events(events);
    }

    private void markIncompleteChoicesFailed() {
        contentBuffers.keySet().forEach(index -> finishReasons.putIfAbsent(index, FinishReason.ERROR.value()));
    }

    private void processChoice(PartialChatResponse chunk, PartialChatResponse.ResultChoice message, List<CallbackEvent> events) {
        var messageIndex = message.index();
        var finishReason = finishReasons.get(messageIndex);

        // OpenAI-compatible endpoints (Model Gateway) send finish_reason as an empty string on intermediate chunks and only set the real
        // value ("stop", "tool_calls", ...) on the terminal chunk. Treating a blank value as absent prevents it from poisoning the map and
        // ensures the real reason (and the completion of the last tool call) is recorded.
        boolean finishReasonJustSet = false;
        if (isNull(finishReason) && nonNull(message.finishReason()) && !message.finishReason().isBlank()) {
            finishReason = message.finishReason();
            finishReasons.put(messageIndex, finishReason);
            finishReasonJustSet = true;
        }

        // A terminal chunk may omit its delta while still carrying the finish reason.
        if (isNull(message.delta())) {
            if (finishReasonJustSet) {
                contentBuffers.computeIfAbsent(messageIndex, key -> new StringBuilder());
                thinkingBuffers.computeIfAbsent(messageIndex, key -> new StringBuilder());
                completeToolCalls(messageIndex, events);
            }
            return;
        }

        var contentBuffer = contentBuffers.computeIfAbsent(messageIndex, key -> new StringBuilder());
        var thinkingBuffer = thinkingBuffers.computeIfAbsent(messageIndex, key -> new StringBuilder());

        if (nonNull(message.delta().role()))
            roles.putIfAbsent(messageIndex, message.delta().role());

        if (nonNull(message.delta().refusal()))
            refusalBuffers.computeIfAbsent(messageIndex, key -> new StringBuilder()).append(message.delta().refusal());

        if (message.delta().toolCalls() != null) {

            StreamingToolFetcher toolFetcher;

            for (ToolCall deltaTool : message.delta().toolCalls()) {

                var tools = toolFetchers.computeIfAbsent(messageIndex, ArrayList::new);

                var toolIndex = nonNull(deltaTool.index())
                    ? deltaTool.index()
                    : resolveToolIndex(deltaTool, tools);

                toolFetcher = tools.stream()
                    .filter(fetcher -> fetcher.getToolIndex() == toolIndex)
                    .findFirst()
                    .orElse(null);

                if (nonNull(toolFetcher) && toolFetcher.isCompleted())
                    throw new IllegalStateException("Received a delta for completed tool index " + toolIndex + " in choice " + messageIndex);

                // A higher index is the boundary for earlier valid calls. Incomplete arguments can still arrive interleaved.
                if (isNull(finishReason) || FinishReason.STOP.value().equals(finishReason) || FinishReason.TOOL_CALLS.value().equals(finishReason)) {
                    for (var previousTool : tools) {
                        if (previousTool.getToolIndex() < toolIndex && !previousTool.isCompleted() && nonNull(previousTool.getName())
                            && Json.isValidObjectOrEncodedObject(previousTool.build().toolCall().function().arguments()))
                            completeToolCall(previousTool, events);
                    }
                }

                if (isNull(toolFetcher)) {
                    toolFetcher = new StreamingToolFetcher(id, messageIndex, toolIndex);
                    tools.add(toolFetcher);
                }

                toolFetcher.setId(deltaTool.id());

                if (nonNull(deltaTool.function())) {
                    toolFetcher.setName(deltaTool.function().name());
                    toolFetcher.appendArguments(deltaTool.function().arguments());

                    var arguments = deltaTool.function().arguments();

                    if (nonNull(arguments) && !arguments.isEmpty()) {
                        toolFetcher.markArgumentsEmitted();
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
                if (nonNull(extractionTags)) {
                    var stateTracker = stateTrackers.computeIfAbsent(messageIndex, key -> new StreamingStateTracker(extractionTags));
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

            if (!token.isEmpty()) {
                thinkingBuffer.append(token);
                events.add(new PartialThinkingEvent(token, chunk));
            }
        }

        // Successful terminal reasons include "stop" on OpenAI-compatible endpoints.
        if (finishReasonJustSet)
            completeToolCalls(messageIndex, events);

    }

    /**
     * Completes pending tool calls when a trailing usage chunk or the end of the stream is reached.
     * <p>
     * Each tool call is emitted at most once, even when multiple terminal signals arrive. Choices stopped by a limit, cancellation, filtering or an
     * error retain their partial tool calls without emitting pending completion callbacks. Previously emitted completions remain delivered.
     * <p>
     * Without a finish reason, all tool arguments in a choice must encode valid JSON objects, directly or as JSON strings, before that choice can
     * complete. Truncated or invalid arguments remain in the final response without a finish reason or pending tool completion callbacks. Missing
     * arguments are treated as an empty object.
     *
     * @return tool completion events, or an empty result when no pending tool call can complete
     */
    public ProcessResult completePendingToolCalls() {
        var events = new ArrayList<CallbackEvent>();
        for (var messageIndex : toolFetchers.keySet().stream().sorted().toList()) {
            var tools = toolFetchers.get(messageIndex);
            if (nonNull(tools) && !tools.isEmpty()) {
                if (!finishReasons.containsKey(messageIndex)
                    && !tools.stream().allMatch(tool -> Json.isValidObjectOrEncodedObject(tool.build().toolCall().function().arguments())))
                    continue;
                finishReasons.putIfAbsent(messageIndex, FinishReason.TOOL_CALLS.value());
                completeToolCalls(messageIndex, events);
            }
        }
        return events.isEmpty() ? ProcessResult.empty() : ProcessResult.events(events);
    }

    private void completeToolCalls(int messageIndex, List<CallbackEvent> events) {
        var finishReason = finishReasons.get(messageIndex);
        if (nonNull(finishReason) && !FinishReason.STOP.value().equals(finishReason) && !FinishReason.TOOL_CALLS.value().equals(finishReason))
            return;

        var tools = toolFetchers.get(messageIndex);
        if (nonNull(tools))
            tools.forEach(tool -> completeToolCall(tool, events));
    }

    /**
     * Builds the final {@link TextChatResponse} from accumulated streaming data.
     *
     * @return the complete {@link TextChatResponse}
     */
    public TextChatResponse buildResponse() {

        var choices = contentBuffers.keySet().stream()
            .sorted()
            .map(key -> {

                var content = contentBuffers.get(key).isEmpty() ? null : contentBuffers.get(key).toString().trim();
                var thinking = thinkingBuffers.get(key).isEmpty() ? null : thinkingBuffers.get(key).toString().trim();
                var tools = toolFetchers.get(key);

                var refusalBuffer = refusalBuffers.get(key);
                var refusal = isNull(refusalBuffer) ? null : refusalBuffer.toString();
                var resultMessage = new ResultMessage(roles.get(key), content, thinking, refusal,
                    nonNull(tools) && !tools.isEmpty()
                        ? tools.stream()
                            .map(StreamingToolFetcher::build)
                            .map(CompletedToolCall::toolCall)
                            .toList()
                        : null);

                return new ResultChoice(key, resultMessage, finishReasons.get(key));

            }).toList();


        var builder = responseBuilderFactory.get()
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
            .moderations(moderations)
            .detections(detections);

        // Gateway-only fields are set only when the factory produces a ModelGatewayChatResponse builder.
        if (builder instanceof ModelGatewayChatResponse.Builder<?> gatewayBuilder)
            gatewayBuilder.serviceTier(serviceTier)
                .systemFingerprint(systemFingerprint)
                .cached(cached);

        return builder.build();
    }

    private int resolveToolIndex(ToolCall delta, List<StreamingToolFetcher> tools) {

        if (tools.isEmpty())
            return 0;

        if (nonNull(delta.id()) && !delta.id().isBlank()) {
            var existing = tools.stream().filter(tool -> delta.id().equals(tool.getId())).findFirst();
            if (existing.isPresent())
                return existing.get().getToolIndex();
        }

        var last = tools.get(tools.size() - 1);
        boolean newId = nonNull(delta.id()) && !delta.id().isBlank() && nonNull(last.getId()) && !delta.id().equals(last.getId());
        boolean newName = nonNull(delta.function()) && nonNull(delta.function().name()) && !delta.function().name().isBlank()
            && nonNull(last.getName())
            && !delta.function().name().equals(last.getName());

        return newId || newName ? tools.stream().mapToInt(StreamingToolFetcher::getToolIndex).max().orElse(-1) + 1 : last.getToolIndex();
    }

    /**
     * Completes the given tool call, emitting the synthetic empty arguments when the model never streamed any.
     * <p>
     * The watsonx.ai Streaming API does not return an empty object for tools without parameters, while OpenAI-compatible endpoints do. Synthesizing
     * the fragment on completion, instead of on the first delta, emits a single {@link PartialToolCallEvent} in both cases.
     */
    private void completeToolCall(StreamingToolFetcher toolFetcher, List<CallbackEvent> events) {
        if (!toolFetcher.markCompleted())
            return;

        var completedToolCall = toolFetcher.build();
        var toolCall = completedToolCall.toolCall();

        if (toolFetcher.markArgumentsEmitted())
            events.add(new PartialToolCallEvent(new PartialToolCall(id, toolFetcher.getChoiceIndex(), toolFetcher.getToolIndex(),
                toolCall.id(), toolCall.function().name(), toolCall.function().arguments())));

        events.add(new CompleteToolCallEvent(completedToolCall));
    }

    /**
     * Concatenates moderation matches from the current chunk into the accumulator, keyed by detector name.
     */
    private static Map<String, List<ModerationResult>> mergeModerations(Map<String, List<ModerationResult>> acc,
        Map<String, List<ModerationResult>> chunk) {

        var merged = isNull(acc) ? new LinkedHashMap<String, List<ModerationResult>>() : new LinkedHashMap<>(acc);

        chunk.forEach((detector, results) -> {
            var list = merged.computeIfAbsent(detector, k -> new ArrayList<>());
            list.addAll(results);
        });

        return merged;
    }

    /**
     * Concatenates detection entries from the current chunk into the accumulator, keyed by target position and grouped by {@code choiceIndex}.
     */
    private static Map<String, List<DetectionEntry>> mergeDetections(Map<String, List<DetectionEntry>> acc, Map<String, List<DetectionEntry>> chunk) {

        var merged = isNull(acc) ? new LinkedHashMap<String, List<DetectionEntry>>() : new LinkedHashMap<>(acc);

        chunk.forEach((target, chunkEntries) -> {
            var byChoice = new LinkedHashMap<Integer, List<DetectionResult>>();
            var existing = merged.get(target);
            if (nonNull(existing))
                existing.forEach(e -> byChoice.computeIfAbsent(e.choiceIndex(), k -> new ArrayList<>()).addAll(e.results()));
            chunkEntries.forEach(e -> byChoice.computeIfAbsent(e.choiceIndex(), k -> new ArrayList<>()).addAll(e.results()));

            var rebuilt = new ArrayList<DetectionEntry>();
            byChoice.forEach((idx, results) -> rebuilt.add(new DetectionEntry(idx, results)));
            merged.put(target, rebuilt);
        });

        return merged;
    }
}
