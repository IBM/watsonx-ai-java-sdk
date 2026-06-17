/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.GroundingHints;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents a response for the Improve Schema API.
 */
public record ImproveSchemaResponse(Metadata metadata, Entity entity) {

    /**
     * Represents the full improve schema entity.
     *
     * @param parameters Parameters used for this improve schema process.
     * @param results The current status and metadata of the improve schema process.
     */
    public record Entity(Parameters parameters, ImproveSchemaResult results) {}

    /**
     * Represents the result and status of an improve schema process.
     *
     * @param status The status of the request.
     * @param runningAt The time when processing started.
     * @param completedAt The time when the request is completed or failed.
     * @param schema The improved schema.
     * @param groundingHints Grounding hints with examples of each field.
     * @param error Optional error details in case of failure.
     */
    public static record ImproveSchemaResult(String status, String runningAt, String completedAt, Schema schema, GroundingHints groundingHints,
        Error error) {}
}
