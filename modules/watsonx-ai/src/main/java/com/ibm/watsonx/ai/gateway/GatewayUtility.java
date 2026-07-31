/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.StreamOptions;

/**
 * Utility class for building {@link GatewayTextChatRequest} wire payloads from a {@link ChatRequest} and default {@link BaseChatParameters}.
 */
public class GatewayUtility {

    private GatewayUtility() {}

    /**
     * Builds a {@link GatewayTextChatRequest} by merging per-request parameters over service-level defaults.
     * <p>
     * Per-request parameters take precedence, defaults fill in any unset values. The {@code fallbackModelId} and {@code fallbackTimeLimit} are the
     * service-level values applied when neither the per-request nor the default parameters specify a model id or time limit.
     *
     * @param chatRequest the incoming {@link ChatRequest}
     * @param defaultParameters the service-level default parameters, or {@code null}
     * @param fallbackModelId the service-level model id used when no parameters supply one
     * @param fallbackTimeLimit the service-level request time limit (in milliseconds) used when no parameters supply one
     * @return a fully resolved {@link GatewayTextChatRequest}
     * @throws IllegalArgumentException if the per-request or default parameters set a watsonx-native-only field unsupported by the gateway
     */
    public static GatewayTextChatRequest buildGatewayRequest(
        ChatRequest chatRequest, BaseChatParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit) {
        return buildGatewayRequest(chatRequest, defaultParameters, fallbackModelId, fallbackTimeLimit, false);
    }

    /**
     * Builds a {@link GatewayTextChatRequest}, optionally flagged for server-sent-event streaming.
     * <p>
     * When {@code stream} is {@code true}, the {@code stream} field is set and, unless the caller already supplied {@code streamOptions}, usage
     * reporting is enabled ({@code stream_options.include_usage = true}) so the terminal chunk carries token counts.
     *
     * @param chatRequest the incoming {@link ChatRequest}
     * @param defaultParameters the service-level default parameters, or {@code null}
     * @param fallbackModelId the service-level model id used when no parameters supply one
     * @param fallbackTimeLimit the service-level request time limit (in milliseconds) used when no parameters supply one
     * @param stream whether to request a streaming response
     * @return a fully resolved {@link GatewayTextChatRequest}
     * @throws IllegalArgumentException if the per-request or default parameters set a watsonx-native-only field unsupported by the gateway
     */
    public static GatewayTextChatRequest buildGatewayRequest(
        ChatRequest chatRequest, BaseChatParameters defaultParameters, String fallbackModelId, long fallbackTimeLimit, boolean stream) {

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;

        // Common fields are read through BaseChatParameters; gateway-only fields require a ModelGatewayParameters instance (null otherwise).
        var parameters = requireNonNullElse(chatRequest.parameters(), ModelGatewayParameters.builder().build());
        var defaults = requireNonNullElse(defaultParameters, ModelGatewayParameters.builder().build());

        // Fail loudly if watsonx-native-only fields are routed through the gateway, rather than silently dropping them.
        checkGatewayCompatible(parameters, "request parameters");
        checkGatewayCompatible(defaults, "default parameters");

        var gwParameters = parameters instanceof ModelGatewayParameters gp ? gp : null;
        var gwDefaults = defaults instanceof ModelGatewayParameters gp ? gp : null;

        var modelId = getOrDefault(parameters.modelId(), getOrDefault(defaults.modelId(), fallbackModelId));
        var timeLimit = getOrDefault(parameters.timeLimit(), getOrDefault(defaults.timeLimit(), fallbackTimeLimit));

        // When streaming, default stream_options.include_usage to true (unless overridden) so the final chunk carries token usage.
        var streamOptions = gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::streamOptions);
        if (stream && isNull(streamOptions))
            streamOptions = new StreamOptions(true);

        var builder = GatewayTextChatRequest.builder()
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
            .maxTokens(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::maxTokens))
            .n(getOrDefault(parameters.n(), defaults.n()))
            .presencePenalty(getOrDefault(parameters.presencePenalty(), defaults.presencePenalty()))
            .seed(getOrDefault(parameters.seed(), defaults.seed()))
            .stop(getOrDefault(parameters.stop(), defaults.stop()))
            .temperature(getOrDefault(parameters.temperature(), defaults.temperature()))
            .topP(getOrDefault(parameters.topP(), defaults.topP()))
            .timeLimit(timeLimit)
            .audio(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::audio))
            .metadata(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::metadata))
            .modalities(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::modalities))
            .parallelToolCalls(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::parallelToolCalls))
            .prediction(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::prediction))
            .reasoningEffort(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::reasoningEffort))
            .serviceTier(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::serviceTier))
            .store(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::store))
            .streamOptions(streamOptions)
            .router(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::router))
            .user(gwOrDefault(gwParameters, gwDefaults, ModelGatewayParameters::user));

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
     * Rejects ChatParameters parameters that the Model Gateway cannot honor.
     * <p>
     * {@link ChatParameters} carries watsonx.ai-specific fields ({@code projectId}, {@code spaceId}, {@code crypto}, the {@code guided*} constraints,
     * {@code repetitionPenalty}, {@code lengthPenalty} and {@code context}) that have no equivalent in the OpenAI-compatible gateway contract.
     *
     * @param params the resolved parameters to validate; only a {@link ChatParameters} instance is inspected, anything else is a no-op
     * @param source a human-readable label identifying where {@code params} came from, used in the error message
     * @throws IllegalArgumentException if {@code params} is a {@link ChatParameters} with any watsonx-native field set
     */
    private static void checkGatewayCompatible(BaseChatParameters params, String source) {
        if (!(params instanceof ChatParameters cp))
            return;

        var unsupported = new ArrayList<String>();
        if (nonNull(cp.projectId()))
            unsupported.add("projectId");
        if (nonNull(cp.spaceId()))
            unsupported.add("spaceId");
        if (nonNull(cp.crypto()))
            unsupported.add("crypto");
        if (nonNull(cp.guidedChoice()))
            unsupported.add("guidedChoice");
        if (nonNull(cp.guidedRegex()))
            unsupported.add("guidedRegex");
        if (nonNull(cp.guidedGrammar()))
            unsupported.add("guidedGrammar");
        if (nonNull(cp.repetitionPenalty()))
            unsupported.add("repetitionPenalty");
        if (nonNull(cp.lengthPenalty()))
            unsupported.add("lengthPenalty");
        if (nonNull(cp.context()))
            unsupported.add("context");

        if (!unsupported.isEmpty())
            throw new IllegalArgumentException(
                "The following watsonx-native parameter(s) set on the %s are not supported by the Model Gateway: %s. Remove them or use ModelGatewayParameters for gateway requests."
                    .formatted(source, unsupported));
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
