/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Map;
import java.util.function.Function;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.StreamOptions;

/**
 * Utility class for building {@link ModelGatewayTextChatRequest} wire payloads from a {@link ModelGatewayChatRequest} and default
 * {@link ModelGatewayParameters}.
 */
public class ModelGatewayUtility {

    private ModelGatewayUtility() {}

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
        ModelGatewayChatRequest chatRequest, ModelGatewayParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit) {
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
        ModelGatewayChatRequest chatRequest, ModelGatewayParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit,
        boolean stream) {

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;

        var parameters = requireNonNullElse(chatRequest.parameters(), ModelGatewayParameters.builder().build());
        var defaults = requireNonNullElse(defaultParameters, ModelGatewayParameters.builder().build());

        var modelId = getOrDefault(parameters.modelId(), getOrDefault(defaults.modelId(), fallbackModelId));
        var timeLimit = getOrDefault(parameters.timeLimit(), getOrDefault(defaults.timeLimit(), fallbackTimeLimit));

        // When streaming, default stream_options.include_usage to true (unless overridden) so the final chunk carries token usage.
        var streamOptions = gwOrDefault(parameters, defaults, ModelGatewayParameters::streamOptions);
        if (stream && isNull(streamOptions))
            streamOptions = new StreamOptions(true);

        var builder = ModelGatewayTextChatRequest.builder()
            .model(modelId)
            .messages(messages)
            .tools(tools)
            .stream(stream ? true : null)
            .toolChoice(resolveToolChoice(parameters, defaults))
            .frequencyPenalty(getOrDefault(parameters.frequencyPenalty(), defaults.frequencyPenalty()))
            .logitBias(getOrDefault(parameters.logitBias(), defaults.logitBias()))
            .logprobs(getOrDefault(parameters.logprobs(), defaults.logprobs()))
            .topLogprobs(getOrDefault(parameters.topLogprobs(), defaults.topLogprobs()))
            .maxCompletionTokens(getOrDefault(parameters.maxCompletionTokens(), defaults.maxCompletionTokens()))
            .maxTokens(gwOrDefault(parameters, defaults, ModelGatewayParameters::maxTokens))
            .n(getOrDefault(parameters.n(), defaults.n()))
            .presencePenalty(getOrDefault(parameters.presencePenalty(), defaults.presencePenalty()))
            .seed(getOrDefault(parameters.seed(), defaults.seed()))
            .stop(getOrDefault(parameters.stop(), defaults.stop()))
            .temperature(getOrDefault(parameters.temperature(), defaults.temperature()))
            .topP(getOrDefault(parameters.topP(), defaults.topP()))
            .timeLimit(timeLimit)
            .audio(gwOrDefault(parameters, defaults, ModelGatewayParameters::audio))
            .metadata(gwOrDefault(parameters, defaults, ModelGatewayParameters::metadata))
            .modalities(gwOrDefault(parameters, defaults, ModelGatewayParameters::modalities))
            .parallelToolCalls(gwOrDefault(parameters, defaults, ModelGatewayParameters::parallelToolCalls))
            .prediction(gwOrDefault(parameters, defaults, ModelGatewayParameters::prediction))
            .reasoningEffort(gwOrDefault(parameters, defaults, ModelGatewayParameters::reasoningEffort))
            .serviceTier(gwOrDefault(parameters, defaults, ModelGatewayParameters::serviceTier))
            .store(gwOrDefault(parameters, defaults, ModelGatewayParameters::store))
            .streamOptions(streamOptions)
            .router(gwOrDefault(parameters, defaults, ModelGatewayParameters::router))
            .user(gwOrDefault(parameters, defaults, ModelGatewayParameters::user));

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

    /**
     * Resolves a gateway-only field, preferring the per-request value over the default. Either instance may be {@code null} when the caller supplied
     * non-gateway parameters (or none), in which case the corresponding value is treated as unset.
     */
    private static <T> T gwOrDefault(ModelGatewayParameters parameters, ModelGatewayParameters defaults, Function<ModelGatewayParameters, T> getter) {
        var perRequest = nonNull(parameters) ? getter.apply(parameters) : null;
        var fallback = nonNull(defaults) ? getter.apply(defaults) : null;
        return getOrDefault(perRequest, fallback);
    }
}
