/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import java.util.List;

/**
 * Represents the partial response from a chat streaming request.
 */
public record PartialChatResponse(String id, String object, String modelId, String model,
    List<ResultChoice> choices, Long created, String modelVersion, String createdAt, ChatUsage usage) {

    public Integer index() {
        return choices.get(0).index();
    }

    public record ResultChoice(Integer index, ResultMessage delta, String finishReason) {}
}
