/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.StreamOptions;

/**
 * Utility class for building {@link ModelGatewayTextChatRequest} wire payloads from a {@link ModelGatewayChatRequest} and default
 * {@link ModelGatewayChatParameters}.
 */
public class ModelGatewayChatUtility {

    private ModelGatewayChatUtility() {}

    /**
     * Builds a {@link ModelGatewayTextChatRequest} by merging per-request parameters over service-level defaults.
     * <p>
     * Per-request parameters take precedence, defaults fill in any unset values. The {@code fallbackModelId} and {@code fallbackTimeLimit} are the
     * service-level values applied when neither the per-request nor the default parameters specify a model id or time limit.
     *
     * @param chatRequest the incoming {@link ModelGatewayChatRequest}
     * @param defaultParameters the service-level default parameters, or {@code null}
     * @param fallbackModelId the service-level model id used when no parameters supply one
     * @param fallbackTimeLimit the service-level request time limit (in milliseconds) used when no parameters supply one
     * @return a fully resolved {@link ModelGatewayTextChatRequest}
     */
    public static ModelGatewayTextChatRequest buildGatewayRequest(
        ModelGatewayChatRequest chatRequest, ModelGatewayChatParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit) {
        return buildGatewayRequest(chatRequest, defaultParameters, fallbackModelId, fallbackTimeLimit, false);
    }

    /**
     * Builds a {@link ModelGatewayTextChatRequest}, optionally flagged for server-sent-event streaming.
     * <p>
     * When {@code stream} is {@code true}, the {@code stream} field is set and, unless the caller already supplied {@code streamOptions}, usage
     * reporting is enabled ({@code stream_options.include_usage = true}) so the terminal chunk carries token counts.
     *
     * @param chatRequest the incoming {@link ModelGatewayChatRequest}
     * @param defaultParameters the service-level default parameters, or {@code null}
     * @param fallbackModelId the service-level model id used when no parameters supply one
     * @param fallbackTimeLimit the service-level request time limit (in milliseconds) used when no parameters supply one
     * @param stream whether to request a streaming response
     * @return a fully resolved {@link ModelGatewayTextChatRequest}
     */
    public static ModelGatewayTextChatRequest buildGatewayRequest(
        ModelGatewayChatRequest chatRequest, ModelGatewayChatParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit,
        boolean stream) {

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;

        if (messages.stream().anyMatch(ControlMessage.class::isInstance))
            throw new IllegalArgumentException("Control messages are not supported by the Model Gateway");

        var parameters = requireNonNullElse(chatRequest.parameters(), ModelGatewayChatParameters.builder().build());
        var defaults = requireNonNullElse(defaultParameters, ModelGatewayChatParameters.builder().build());

        var modelId = getOrDefault(parameters.modelId(), getOrDefault(defaults.modelId(), fallbackModelId));
        var timeLimit = getOrDefault(parameters.timeLimit(), getOrDefault(defaults.timeLimit(), fallbackTimeLimit));

        // When streaming, default stream_options.include_usage to true (unless overridden) so the final chunk carries token usage.
        var streamOptions = getOrDefault(parameters.streamOptions(), defaults.streamOptions());
        if (stream && isNull(streamOptions))
            streamOptions = new StreamOptions(true);

        // The gateway reads an absent "stream" as a non-streaming request, so the flag is sent only when streaming.
        Boolean streamFlag = stream ? true : null;

        var builder = ModelGatewayTextChatRequest.builder()
            .model(modelId)
            .messages(messages)
            .tools(tools)
            .stream(streamFlag)
            .toolChoice(resolveToolChoice(parameters, defaults))
            .frequencyPenalty(getOrDefault(parameters.frequencyPenalty(), defaults.frequencyPenalty()))
            .logitBias(getOrDefault(parameters.logitBias(), defaults.logitBias()))
            .logprobs(getOrDefault(parameters.logprobs(), defaults.logprobs()))
            .topLogprobs(getOrDefault(parameters.topLogprobs(), defaults.topLogprobs()))
            .maxCompletionTokens(getOrDefault(parameters.maxCompletionTokens(), defaults.maxCompletionTokens()))
            .maxTokens(getOrDefault(parameters.maxTokens(), defaults.maxTokens()))
            .n(getOrDefault(parameters.n(), defaults.n()))
            .presencePenalty(getOrDefault(parameters.presencePenalty(), defaults.presencePenalty()))
            .seed(getOrDefault(parameters.seed(), defaults.seed()))
            .stop(getOrDefault(parameters.stop(), defaults.stop()))
            .temperature(getOrDefault(parameters.temperature(), defaults.temperature()))
            .topP(getOrDefault(parameters.topP(), defaults.topP()))
            .timeLimit(timeLimit)
            .audio(getOrDefault(parameters.audio(), defaults.audio()))
            .metadata(getOrDefault(parameters.metadata(), defaults.metadata()))
            .modalities(getOrDefault(parameters.modalities(), defaults.modalities()))
            .parallelToolCalls(getOrDefault(parameters.parallelToolCalls(), defaults.parallelToolCalls()))
            .prediction(getOrDefault(parameters.prediction(), defaults.prediction()))
            .reasoningEffort(getOrDefault(parameters.reasoningEffort(), defaults.reasoningEffort()))
            .serviceTier(getOrDefault(parameters.serviceTier(), defaults.serviceTier()))
            .store(getOrDefault(parameters.store(), defaults.store()))
            .streamOptions(streamOptions)
            .router(getOrDefault(parameters.router(), defaults.router()))
            .user(getOrDefault(parameters.user(), defaults.user()));

        // Response format: JSON schema takes precedence over plain format.
        var jsonSchema = getOrDefault(parameters.jsonSchema(), defaults.jsonSchema());
        if (nonNull(jsonSchema)) {
            builder.jsonSchema(jsonSchema);
        } else {
            var responseFormat = getOrDefault(parameters.responseFormat(), defaults.responseFormat());
            if (nonNull(responseFormat)) {
                builder.responseFormat(Map.of("type", responseFormat));
            }
        }

        return builder.build();
    }

    /**
     * Resolves the single OpenAI-compatible {@code tool_choice} value for the gateway.
     */
    private static Object resolveToolChoice(BaseChatParameters parameters, BaseChatParameters defaults) {
        if (nonNull(parameters.toolChoice()))
            return parameters.toolChoice();
        if (nonNull(parameters.toolChoiceOption()))
            return parameters.toolChoiceOption();
        if (nonNull(defaults.toolChoice()))
            return defaults.toolChoice();
        return defaults.toolChoiceOption();
    }
}
