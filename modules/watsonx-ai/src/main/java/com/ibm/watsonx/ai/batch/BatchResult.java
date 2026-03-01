/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

/**
 * Represents a single processed result from a batch output file.
 * <p>
 * Each {@code BatchResult} corresponds to one request from the batch input file and contains the response returned by the API for that request.
 *
 * @param <T> the type of the response body (e.g., {@code ChatResponse})
 * @param id the unique identifier of this result entry
 * @param customId the custom identifier provided in the original batch request, used to correlate this result with the corresponding input item
 * @param response the {@link Response} containing the HTTP status code, request ID, and deserialized response body
 * @param processedAt the Unix timestamp (in milliseconds) at which this request was processed
 */
public record BatchResult<T>(String id, String customId, Response<T> response, Long processedAt) {

    /**
     * Represents the HTTP response for a single batch request item.
     *
     * @param <T> the type of the deserialized response body
     * @param statusCode the HTTP status code of the response (e.g., {@code 200})
     * @param requestId the unique identifier assigned to this request by the server
     * @param body the deserialized response body
     */
    public record Response<T>(Integer statusCode, String requestId, T body) {}
}
