/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.ChatResponse.DetectionEntry;
import com.ibm.watsonx.ai.chat.ChatResponse.ModerationResult;

/**
 * Represents the partial response from a chat streaming request.
 *
 * @param id the unique identifier of the response
 * @param object the object type of the response
 * @param modelId the identifier of the model that produced the response
 * @param model the name of the model that produced the response
 * @param choices the list of result choices returned in this chunk
 * @param created the creation timestamp, in epoch seconds
 * @param modelVersion the version of the model that produced the response
 * @param createdAt the ISO 8601 timestamp when the response was created
 * @param usage the token usage statistics, if present
 * @param moderations the moderation results detected in this chunk, keyed by detector name
 * @param detections the detection results reported in this chunk, keyed by target position
 */
public record PartialChatResponse(String id, String object, String modelId, String model,
    List<ResultChoice> choices, Long created, String modelVersion, String createdAt, ChatUsage usage,
    Map<String, List<ModerationResult>> moderations, Map<String, List<DetectionEntry>> detections) {

    /**
     * Returns the index of the first result choice.
     *
     * @return the index of the first choice
     */
    public Integer index() {
        return choices.get(0).index();
    }

    /**
     * Represents a single result choice within a partial chat response.
     *
     * @param index the index of the choice
     * @param delta the incremental message content for this choice
     * @param finishReason the reason the model stopped generating, if present
     */
    public record ResultChoice(Integer index, ResultMessage delta, String finishReason) {}
}
