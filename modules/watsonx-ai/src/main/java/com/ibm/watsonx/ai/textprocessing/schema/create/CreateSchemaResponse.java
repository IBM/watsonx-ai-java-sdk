/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import com.ibm.watsonx.ai.textprocessing.DataReference;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.GroundingHints;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents a response for the Create Schema API.
 */
public record CreateSchemaResponse(Metadata metadata, Entity entity) {

    /**
     * Represents the full create schema entity.
     *
     * @param documentReference Reference to the input document data.
     * @param results The current status and metadata of the create schema process.
     * @param parameters Parameters used for this create schema process.
     */
    public record Entity(DataReference documentReference, CreateSchemaResult results, Parameters parameters) {}

    /**
     * Represents the result and status of a create schema process.
     *
     * @param status The status of the request.
     * @param runningAt The time when processing started.
     * @param completedAt The time when the request is completed or failed.
     * @param numberPagesProcessed Number of pages that have been processed.
     * @param totalPages The total number of pages to process.
     * @param schema The generated schema.
     * @param groundingHints Grounding hints with examples of each field.
     * @param error Optional error details in case of failure.
     */
    public static record CreateSchemaResult(String status, String runningAt, String completedAt, int numberPagesProcessed, Integer totalPages,
        Schema schema, GroundingHints groundingHints, Error error) {}
}
