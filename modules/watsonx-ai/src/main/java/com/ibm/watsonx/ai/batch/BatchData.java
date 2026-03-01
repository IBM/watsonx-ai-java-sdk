/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import java.util.List;
import java.util.Map;

/**
 * Represents a batch job returned by the watsonx.ai Batches API.
 *
 * @param id the unique identifier of the batch job (e.g., {@code batch_id})
 * @param object the object type, always {@code "batch"}
 * @param endpoint the API endpoint used to process the batch items (e.g., {@code /v1/chat/completions})
 * @param errors the {@link FileErrors} object containing any validation or processing errors associated with the batch job; may be {@code null}
 * @param inputFileId the identifier of the uploaded input file containing the batch requests (e.g., {@code file-CAMS_ASSET_ID})
 * @param completionWindow the time window within which the batch job must complete (e.g., {@code 24h})
 * @param status the current status of the batch job (e.g., {@code validating}, {@code in_progress}, {@code completed}, {@code failed},
 *            {@code cancelling}, {@code cancelled}, {@code expired})
 * @param outputFileId the identifier of the output file containing the results, available once the job completes successfully; may be {@code null}
 * @param errorFileId the identifier of the error file containing failed request details; may be {@code null}
 * @param createdAt Unix timestamp (in milliseconds) when the batch job was created
 * @param inProgressAt Unix timestamp (in milliseconds) when the batch job started processing; may be {@code null}
 * @param expiresAt Unix timestamp (in milliseconds) when the batch job will expire; may be {@code null}
 * @param finalizingAt Unix timestamp (in milliseconds) when the batch job started finalizing; may be {@code null}
 * @param completedAt Unix timestamp (in milliseconds) when the batch job completed successfully; may be {@code null}
 * @param failedAt Unix timestamp (in milliseconds) when the batch job failed; may be {@code null}
 * @param expiredAt Unix timestamp (in milliseconds) when the batch job expired; may be {@code null}
 * @param cancellingAt Unix timestamp (in milliseconds) when cancellation was requested; may be {@code null}
 * @param cancelledAt Unix timestamp (in milliseconds) when the batch job was fully cancelled; may be {@code null}
 * @param requestCounts the {@link RequestCounts} object summarizing total, completed, and failed request counts
 * @param metadata optional key-value pairs associated with the batch job; may be {@code null}
 */
public record BatchData(
    String id,
    String object,
    String endpoint,
    FileErrors errors,
    String inputFileId,
    String completionWindow,
    String status,
    String outputFileId,
    String errorFileId,
    Long createdAt,
    Long inProgressAt,
    Long expiresAt,
    Long finalizingAt,
    Long completedAt,
    Long failedAt,
    Long expiredAt,
    Long cancellingAt,
    Long cancelledAt,
    RequestCounts requestCounts,
    Map<String, String> metadata) {

    /**
     * Represents the errors associated with a batch job.
     *
     * @param object the object type of the error container
     * @param data the list of {@link FileError} entries describing individual errors
     */
    public record FileErrors(String object, List<FileError> data) {}

    /**
     * Represents a single error detail within a batch job.
     *
     * @param code the error code identifying the type of error
     * @param message a human-readable description of the error
     * @param param the parameter name that caused the error, if applicable; may be {@code null}
     * @param line the line number in the input file where the error occurred, if applicable; may be {@code null}
     */
    public record FileError(String code, String message, String param, Integer line) {}

    /**
     * Represents the request counts for a batch job.
     *
     * @param total the total number of requests in the batch
     * @param completed the number of requests that completed successfully
     * @param failed the number of requests that failed
     */
    public record RequestCounts(Integer total, Integer completed, Integer failed) {}
}
