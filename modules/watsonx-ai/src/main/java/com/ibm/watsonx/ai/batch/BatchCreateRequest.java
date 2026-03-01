/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.Map;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to create a new batch job using the watsonx.ai Batches APIs.
 * <p>
 * The batch job will process the requests contained in the input file for the specified endpoint.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * BatchCreateRequest.builder()
 *     .inputFileId("file-abc123")
 *     .endpoint("/v1/chat/completions")
 *     .completionWindow("24h")
 *     .build();
 * }</pre>
 */
public class BatchCreateRequest extends WatsonxParameters {
    private final String inputFileId;
    private final String endpoint;
    private final String completionWindow;
    private final Map<String, Object> metadata;
    private final Duration timeout;

    private BatchCreateRequest(Builder builder) {
        super(builder);
        inputFileId = builder.inputFileId;
        endpoint = builder.endpoint;
        completionWindow = requireNonNullElse(builder.completionWindow, "24h");
        metadata = builder.metadata;
        timeout = builder.timeout;
    }

    /**
     * Returns the identifier of the uploaded input file for the batch job.
     *
     * @return the input file identifier
     */
    public String inputFileId() {
        return inputFileId;
    }

    /**
     * Returns the API endpoint to use for processing each batch item.
     *
     * @return the endpoint path (e.g., {@code /v1/chat/completions})
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * Returns the time window for completion of the batch job.
     *
     * @return the completion window (e.g., {@code 24h})
     */
    public String completionWindow() {
        return completionWindow;
    }

    /**
     * Returns the optional metadata associated with the batch job.
     *
     * @return a map of metadata key-value pairs
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * Returns the timeout duration for the batch job request.
     *
     * @return the timeout {@link Duration}
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * BatchCreateRequest.builder()
     *     .inputFileId("file-abc123")
     *     .endpoint("/v1/chat/completions")
     *     .completionWindow("24h")
     *     .build();
     * }</pre>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link Builder} pre-populated with the non-null fields of the given {@link BatchCreateRequest} instance.
     * <p>
     * This allows creating a modified copy of an existing request by overriding only the fields that need to change.
     *
     * @param request the {@link BatchCreateRequest} to copy from.
     * @return a new {@link Builder} initialized with the non-null fields of {@code request}
     */
    public static Builder builder(BatchCreateRequest request) {
        return new Builder(request);
    }

    /**
     * Builder class for constructing {@link BatchCreateRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String inputFileId;
        private String endpoint;
        private String completionWindow;
        private Map<String, Object> metadata;
        private Duration timeout;

        private Builder() {}

        private Builder(BatchCreateRequest request) {
            if (nonNull(request.projectId))
                projectId = request.projectId;
            if (nonNull(request.spaceId))
                spaceId = request.spaceId;
            if (nonNull(request.transactionId))
                transactionId = request.transactionId;
            if (nonNull(request.inputFileId))
                inputFileId = request.inputFileId;
            if (nonNull(request.endpoint))
                endpoint = request.endpoint;
            if (nonNull(request.completionWindow))
                completionWindow = request.completionWindow;
            if (nonNull(request.metadata))
                metadata = request.metadata;
            if (nonNull(request.timeout))
                timeout = request.timeout;
        }

        /**
         * Sets the identifier of the uploaded input file for the batch job.
         *
         * @param inputFileId the input file identifier
         */
        public Builder inputFileId(String inputFileId) {
            this.inputFileId = inputFileId;
            return this;
        }

        /**
         * Sets the API endpoint to use for processing each batch item.
         *
         * @param endpoint the endpoint path (e.g., {@code /v1/chat/completions})
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Sets the time window for completion of the batch job.
         *
         * @param completionWindow the completion window (e.g., {@code 24h})
         */
        public Builder completionWindow(String completionWindow) {
            this.completionWindow = completionWindow;
            return this;
        }

        /**
         * Sets optional metadata to associate with the batch job.
         *
         * @param metadata a map of metadata key-value pairs
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the timeout duration for the batch job request.
         *
         * @param timeout the maximum {@link Duration} to wait for the batch job to complete
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds a {@link BatchCreateRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link BatchCreateRequest}
         */
        public BatchCreateRequest build() {
            return new BatchCreateRequest(this);
        }
    }
}
