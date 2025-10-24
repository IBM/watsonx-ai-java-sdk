/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.DataReference;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.Metadata;

/**
 * Represents a response for the Text Classification API.
 */
public record TextClassificationResponse(Metadata metadata, Entity entity) {

    /**
     * Represents the document details for the text classification.
     *
     * @param documentReference Reference to the input document data.
     * @param results The current status and metadata of the text classification process.
     * @param parameters Parameters used for this classification process.
     * @param custom User defined properties specified as key-value pairs.
     */
    public record Entity(DataReference documentReference, ClassificationResult results, Parameters parameters,
        Map<String, Object> custom) {}

    /**
     * Represents the result and status of a text classification process.
     *
     * @param status The status of the extraction request.
     * @param runningAt The time when processing started.
     * @param completedAt The time when the classification completed or failed.
     * @param numberPagesProcessed The number of pages to process.
     * @param documentClassified A flag to indicate if the classification was found.
     * @param documentType The classification of the document if found.
     * @param error Optional error details in case of failure.
     */
    public record ClassificationResult(String status, String runningAt, String completedAt, Integer numberPagesProcessed, Boolean documentClassified,
        String documentType, Error error) {}
}
