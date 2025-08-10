/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.DataReference;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.Parameters;

/**
 * Represents a response for the text extraction api.
 */
public record TextExtractionResponse(Metadata metadata, Entity entity) {

    /**
     * Common metadata for a text extraction resource.
     *
     * @param id The unique identifier of the resource.
     * @param createdAt The ISO 8601 timestamp indicating when the resource was created.
     * @param modifiedAt The ISO 8601 timestamp indicating when the resource was modified.
     * @param spaceId The id of the space containing the resource.
     * @param projectId The id of the project containing the resource.
     */
    public record Metadata(String id, String createdAt, String modifiedAt, String spaceId, String projectId) {}

    /**
     * Represents the full text extraction entity.
     *
     * @param documentReference Reference to the input document data.
     * @param resultsReference Reference to the extracted results data.
     * @param results The current status and metadata of the text extraction process.
     * @param parameters Parameters used for this extraction process.
     * @param custom Optional custom fields for additional metadata.
     */
    public record Entity(DataReference documentReference, DataReference resultsReference, ExtractionResult results, Parameters parameters,
        Map<String, Object> custom) {}

    /**
     * Represents the result and status of a text extraction process.
     *
     * @param status The status of the extraction request. Possible values: {@code submitted}, {@code uploading}, {@code running},
     *            {@code downloading}, {@code downloaded}, {@code completed}, {@code failed}.
     * @param numberPagesProcessed Number of pages that have been processed.
     * @param runningAt The time when processing started.
     * @param completedAt The time when the extraction completed or failed.
     * @param totalPages The total number of pages to process.
     * @param location The output file locations produced by the extraction.
     * @param error Optional error details in case of failure.
     */
    public record ExtractionResult(String status, int numberPagesProcessed, String runningAt, String completedAt, Integer totalPages,
        List<String> location, Error error) {}

    /**
     * Represents an error that occurred during processing.
     *
     * @param code A simple code representing the error type.
     * @param message A human-readable message describing the error.
     * @param moreInfo Optional URL pointing to more detailed information.
     */
    public record Error(String code, String message, String moreInfo) {}

    /**
     * Enum representing the possible status of requested outputs for text extraction.
     */
    public static enum Status {

        SUBMITTED("submitted"),
        UPLOADING("uploading"),
        RUNNING("running"),
        DOWNLOADING("downloading"),
        DOWNLOADED("downloaded"),
        COMPLETED("completed"),
        FAILED("failed");

        private String value;

        Status(String value) {
            this.value = value;
        }

        public static Status fromValue(String value) {
            return Stream.of(Status.values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Type value: " + value));
        }
    }
}
