package com.ibm.watsonx.runtime.chat.model;

import java.util.List;

/**
 * Represents the partial response from a chat streaming request.
 */
public final record PartialChatResponse(String id, String object, String modelId, String model, List<ResultChoice> choices,
    Long created, String modelVersion, String createdAt, ChatUsage usage) {

    public record ResultChoice(Integer index, ResultMessage delta, String finishReason) {}
}
