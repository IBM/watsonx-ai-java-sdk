/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;

public class ChatUtility {

    /**
     * Builds a {@link TextChatRequest} from the provided {@link ChatRequest}.
     *
     * @param chatRequest the {@link ChatRequest} object
     * @return a fully constructed {@link TextChatRequest} object
     */
    public static TextChatRequest buildTextChatRequest(ChatRequest chatRequest, ChatParameters defaultParameters) {

        var messages = chatRequest.messages();
        var tools = nonNull(chatRequest.tools()) && !chatRequest.tools().isEmpty() ? chatRequest.tools() : null;
        var parameters = requireNonNullElse(chatRequest.parameters(), ChatParameters.builder().build());
        defaultParameters = requireNonNullElse(defaultParameters, ChatParameters.builder().build());

        if (messages.stream().anyMatch(ControlMessage.class::isInstance)
            && (isNull(chatRequest.thinking()) || isNull(chatRequest.thinking().extractionTags())))
            throw new IllegalArgumentException("Extraction tags are required when using control messages");

        var projectId = nonNull(parameters.projectId())
            ? parameters.projectId()
            : defaultParameters.projectId();

        var spaceId = nonNull(parameters.spaceId())
            ? parameters.spaceId()
            : defaultParameters.spaceId();

        var modelId = nonNull(parameters.modelId())
            ? parameters.modelId()
            : defaultParameters.modelId();

        var timeout = nonNull(parameters.timeLimit())
            ? parameters.timeLimit()
            : defaultParameters.timeLimit();

        Boolean includeReasoning = null;
        String thinkingEffort = null;
        Map<String, Object> chatTemplateKwargs = null;
        if (nonNull(chatRequest.thinking())) {
            var thinking = chatRequest.thinking();
            includeReasoning = thinking.includeReasoning();
            thinkingEffort = nonNull(thinking.thinkingEffort()) ? thinking.thinkingEffort().getValue() : null;
            if (nonNull(thinking.enabled()))
                chatTemplateKwargs = Map.of("thinking", thinking.enabled());
        }

        return TextChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .toolChoiceOption(getOrDefault(parameters.toolChoiceOption(), defaultParameters.toolChoiceOption()))
            .toolChoice(getOrDefault(parameters.toolChoice(), defaultParameters.toolChoice()))
            .frequencyPenalty(getOrDefault(parameters.frequencyPenalty(), defaultParameters.frequencyPenalty()))
            .logitBias(getOrDefault(parameters.logitBias(), defaultParameters.logitBias()))
            .logprobs(getOrDefault(parameters.logprobs(), defaultParameters.logprobs()))
            .topLogprobs(getOrDefault(parameters.topLogprobs(), defaultParameters.topLogprobs()))
            .maxCompletionTokens(getOrDefault(parameters.maxCompletionTokens(), defaultParameters.maxCompletionTokens()))
            .n(getOrDefault(parameters.n(), defaultParameters.n()))
            .presencePenalty(getOrDefault(parameters.presencePenalty(), defaultParameters.presencePenalty()))
            .seed(getOrDefault(parameters.seed(), defaultParameters.seed()))
            .stop(getOrDefault(parameters.stop(), defaultParameters.stop()))
            .temperature(getOrDefault(parameters.temperature(), defaultParameters.temperature()))
            .topP(getOrDefault(parameters.topP(), defaultParameters.topP()))
            .responseFormat(getOrDefault(parameters.responseFormat(), defaultParameters.responseFormat()))
            .jsonSchema(getOrDefault(parameters.jsonSchema(), defaultParameters.jsonSchema()))
            .context(getOrDefault(parameters.context(), defaultParameters.context()))
            .timeLimit(getOrDefault(parameters.timeLimit(), timeout))
            .guidedChoice(getOrDefault(parameters.guidedChoice(), defaultParameters.guidedChoice()))
            .guidedRegex(getOrDefault(parameters.guidedRegex(), defaultParameters.guidedRegex()))
            .guidedGrammar(getOrDefault(parameters.guidedGrammar(), defaultParameters.guidedGrammar()))
            .repetitionPenalty(getOrDefault(parameters.repetitionPenalty(), defaultParameters.repetitionPenalty()))
            .lengthPenalty(getOrDefault(parameters.lengthPenalty(), defaultParameters.lengthPenalty()))
            .includeReasoning(includeReasoning)
            .reasoningEffort(thinkingEffort)
            .chatTemplateKwargs(chatTemplateKwargs)
            .crypto(getOrDefault(parameters.crypto(), defaultParameters.crypto()))
            .build();
    }
}
