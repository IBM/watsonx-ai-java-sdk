/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.ibm.watsonx.ai.chat.model.FinishReason.TOOL_CALLS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
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
    private volatile ChatUsage chatUsage;
    private volatile Map<String, List<ModerationResult>> moderations;
    private volatile Map<String, List<DetectionEntry>> detections;
    private volatile String serviceTier;
    private volatile String systemFingerprint;
    private volatile Boolean cached;
    private final Map<Integer, StringBuilder> contentBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, StringBuilder> thinkingBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, List<StreamingToolFetcher>> toolFetchers = new ConcurrentHashMap<>();
    private final StreamingStateTracker stateTracker;
    private final Map<String, Boolean> toolHasParameters;
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
        this.toolHasParameters = toolHasParameters(tools);
        this.extractionTags = extractionTags;
        this.responseBuilderFactory = responseBuilderFactory;
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

        // OpenAI-compatible endpoints (Model Gateway) terminate the stream with a "data: [DONE]" sentinel.
        if ("[DONE]".equals(messageData))
            return ProcessResult.empty();

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

        if (chunk.choices().size() == 0) {

            // For certain models, watsonx.ai does not return FinishReason.TOOL_CHOICE when ToolChoiceOption.REQUIRED is set.
            // Therefore, this check is required to ensure that ChatHandler.onCompleteToolCall is called for the last tool in the
            // StreamingToolFetcher. The completion performed when a new tool call delta starts is not enough.

            if (toolFetchers.isEmpty())
                return ProcessResult.empty();

            toolFetchers.keySet().forEach(messageIndex -> {

                if (nonNull(finishReasons.get(messageIndex)))
                    // FinishReason is not null, so the tool call has already been processed.
                    return;

                var tools = toolFetchers.get(messageIndex);

                if (isNull(tools) || tools.isEmpty())
                    // Nothing to complete, so the index must not be marked as processed.
                    return;

                finishReasons.put(messageIndex, FinishReason.TOOL_CALLS.value());
                events.add(new CompleteToolCallEvent(tools.get(tools.size() - 1).build()));
            });


            return events.isEmpty() ? ProcessResult.empty() : ProcessResult.events(events);
        }

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

        // OpenAI-compatible endpoints (Model Gateway) send finish_reason as an empty string on intermediate chunks and only set the real
        // value ("stop", "tool_calls", ...) on the terminal chunk. Treating a blank value as absent prevents it from poisoning the map and
        // ensures the real reason (and the completion of the last tool call) is recorded.
        boolean finishReasonJustSet = false;
        if (isNull(finishReason) && nonNull(message.finishReason()) && !message.finishReason().isBlank()) {
            finishReason = message.finishReason();
            finishReasons.put(messageIndex, finishReason);
            finishReasonJustSet = true;
        }

        if (isNull(role) && nonNull(message.delta().role()))
            role = message.delta().role();

        if (isNull(refusal) && nonNull(message.delta().refusal()))
            refusal = message.delta().refusal();

        if (message.delta().toolCalls() != null) {

            StreamingToolFetcher toolFetcher;

            for (ToolCall deltaTool : message.delta().toolCalls()) {

                var tools = toolFetchers.computeIfAbsent(messageIndex, ArrayList::new);

                var toolIndex = nonNull(deltaTool.index())
                    ? deltaTool.index()
                    : tools.isEmpty() ? 0 : tools.get(tools.size() - 1).getToolIndex();

                // Check if there is an incomplete version of the TextChatToolCall object.
                toolFetcher = tools.stream()
                    .filter(fetcher -> fetcher.getToolIndex() == toolIndex)
                    .findFirst()
                    .orElse(null);

                if (isNull(toolFetcher)) {
                    // First occurrence of the object, create it.
                    if (!tools.isEmpty()) {
                        events.add(new CompleteToolCallEvent(tools.get(tools.size() - 1).build()));
                    }
                    toolFetcher = new StreamingToolFetcher(id, messageIndex, toolIndex);
                    tools.add(toolFetcher);
                }

                toolFetcher.setId(deltaTool.id());

                if (nonNull(deltaTool.function())) {
                    toolFetcher.setName(deltaTool.function().name());
                    toolFetcher.appendArguments(deltaTool.function().arguments());

                    // There is a bug in the Streaming API: it does not return an empty object for tools without arguments.
                    // Open an issue.
                    var toolHasParameter = nonNull(toolFetcher.getName()) ? toolHasParameters.get(toolFetcher.getName()) : null;
                    var parameterless = nonNull(toolHasParameter) && !toolHasParameter;
                    var arguments = parameterless && toolFetcher.markEmptyArgumentsEmitted()
                        ? "{}"
                        : deltaTool.function().arguments();

                    if (nonNull(arguments) && !arguments.isEmpty()) {
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

            if (!token.isEmpty()) {
                thinkingBuffer.append(token);
                events.add(new PartialThinkingEvent(token, chunk));
            }
        }

        // Complete the last tool call only on the chunk that transitions the finish reason to "tool_calls". OpenAI-compatible endpoints
        // (Model Gateway) send a trailing usage chunk that still carries a populated choices[0] with finish_reason "" after the terminal
        // chunk, guarding on finishReasonJustSet prevents that chunk from emitting a duplicate CompleteToolCallEvent.
        // Some models declare "tool_calls" without ever emitting a tool_calls delta (observed on llama-3-3-70b when tools are combined with
        // a json_schema response_format), so there may be no fetcher to complete.
        if (finishReasonJustSet && TOOL_CALLS.value().equals(finishReason)) {
            var tools = toolFetchers.get(messageIndex);
            if (nonNull(tools) && !tools.isEmpty())
                events.add(new CompleteToolCallEvent(tools.get(tools.size() - 1).build()));
        }

        return ProcessResult.events(events);
    }

    /**
     * Builds the final {@link TextChatResponse} from accumulated streaming data.
     *
     * @return the complete {@link TextChatResponse}
     */
    public TextChatResponse buildResponse() {

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

    /**
     * Builds a map indicating whether each tool has parameters.
     *
     * @param tools the list of available {@link Tool}s
     * @return a map where keys are tool names and values indicate if the tool has parameters
     */
    private static Map<String, Boolean> toolHasParameters(List<Tool> tools) {
        if (isNull(tools) || tools.size() == 0)
            return Map.of();

        return tools.stream().collect(toMap(
            tool -> tool.function().name(),
            Tool::hasParameters
        ));
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
