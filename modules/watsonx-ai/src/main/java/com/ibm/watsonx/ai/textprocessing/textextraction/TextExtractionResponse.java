/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.DataReference;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.Metadata;

/**
 * Represents a response for the Text Extraction API.
 */
public record TextExtractionResponse(Metadata metadata, Entity entity) {

    /**
     * Represents the full text extraction entity.
     *
     * @param documentReference Reference to the input document data.
     * @param resultsReference Reference to the extracted results data.
     * @param results The current status and metadata of the text extraction process.
     * @param parameters Parameters used for this extraction process.
     * @param custom User defined properties specified as key-value pairs.
     */
    public record Entity(DataReference documentReference, DataReference resultsReference, ExtractionResult results, Parameters parameters,
        Map<String, Object> custom) {}

    /**
     * Represents the result and status of a text extraction process.
     *
     * @param status The status of the extraction request.
     * @param numberPagesProcessed Number of pages that have been processed.
     * @param runningAt The time when processing started.
     * @param completedAt The time when the extraction completed or failed.
     * @param totalPages The total number of pages to process.
     * @param location The output file locations produced by the extraction.
     * @param error Optional error details in case of failure.
     */
    public record ExtractionResult(String status, int numberPagesProcessed, String runningAt, String completedAt, Integer totalPages,
        List<String> location, Error error) {}
}
