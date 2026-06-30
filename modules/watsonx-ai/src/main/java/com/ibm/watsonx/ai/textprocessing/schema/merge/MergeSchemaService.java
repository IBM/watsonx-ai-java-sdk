/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaResponse.MergeSchemaResult;

/**
 * Service class to interact with IBM watsonx.ai Merge Schema APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MergeSchemaService mergeSchemaService = MergeSchemaService.builder()
 *   .baseUrl("https://...")    // or use CloudRegion
 *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *   .projectId("project-id")
 *   .build();
 *
 * Schema existingSchema = ...; // Get schema from CreateSchemaService
 * MergeSchemaResult result = mergeSchemaService.mergeSchemaAndFetch(existingSchema);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class MergeSchemaService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(MergeSchemaService.class);
    private final MergeSchemaRestClient client;

    private MergeSchemaService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = MergeSchemaRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Starts the merge schema process for existing schemas.
     *
     * @param schemas The schemas to merge.
     * @return An {@link MergeSchemaResponse} representing the submitted request and its current status.
     */
    public MergeSchemaResponse startMergeSchema(List<Schema> schemas) {
        requireNonNull(schemas, "schemas cannot be null");
        return startMergeSchema(schemas, null);
    }

    /**
     * Starts the merge schema process for existing schemas.
     *
     * @param schemas The schemas to merge.
     * @param parameters The configuration parameters for merge schema.
     * @return An {@link MergeSchemaResponse} representing the submitted request and its current status.
     */
    public MergeSchemaResponse startMergeSchema(List<Schema> schemas, MergeSchemaParameters parameters) {
        requireNonNull(schemas, "schemas cannot be null");
        return startMergeSchema(UUID.randomUUID().toString(), schemas, parameters, false);
    }

    /**
     * Starts the merge schema process and waits until the schemas are merged.
     *
     * @param schemas The schemas to merge.
     * @return An {@link MergeSchemaResult} with the merged schema.
     * @see #mergeSchemaAndFetch(List, MergeSchemaParameters)
     */
    public MergeSchemaResult mergeSchemaAndFetch(List<Schema> schemas) {
        requireNonNull(schemas, "schemas cannot be null");
        return mergeSchemaAndFetch(schemas, null);
    }

    /**
     * Starts the merge schema process and waits until the schemas are merged.
     *
     * @param schemas The schemas to merge.
     * @param parameters The configuration parameters for merge schema.
     * @return An {@link MergeSchemaResult} with the merged schema.
     */
    public MergeSchemaResult mergeSchemaAndFetch(List<Schema> schemas, MergeSchemaParameters parameters) {
        requireNonNull(schemas, "schemas cannot be null");
        return mergeSchemaAndFetch(UUID.randomUUID().toString(), schemas, parameters);
    }

    /**
     * Retrieves the results of a merge schema request by its unique identifier.
     *
     * @param id The unique identifier of the merge schema request.
     * @return An {@link MergeSchemaResponse} containing the results of the request.
     */
    public MergeSchemaResponse fetchRequest(String id) {
        requireNonNull(id, "id cannot be null");
        return fetchRequest(id, MergeSchemaFetchParameters.builder().build());
    }

    /**
     * Retrieves the results of a merge schema request by its unique identifier.
     *
     * @param id The unique identifier of the merge schema request.
     * @param parameters Parameters to specify the project or space context in which the request was made.
     * @return An {@link MergeSchemaResponse} containing the results of the request.
     */
    public MergeSchemaResponse fetchRequest(String id, MergeSchemaFetchParameters parameters) {
        requireNonNull(id, "id cannot be null");
        return fetchMergeSchemaRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Deletes a merge schema request.
     *
     * @param id The unique identifier of the merge schema request to delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, MergeSchemaDeleteParameters.builder().build());
    }

    /**
     * Deletes a merge schema request.
     *
     * @param id The unique identifier of the merge schema request to delete.
     * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id, MergeSchemaDeleteParameters parameters) {

        requireNonNull(id, "The id can not be null");

        var builder = MergeSchemaDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var request = new DeleteRequest(parameters.transactionId(), id, p);
        return client.deleteRequest(request);
    }

    //
    // Retrieves the results of a merge schema request by its unique identifier.
    //
    private MergeSchemaResponse fetchMergeSchemaRequest(String requestId, String id, MergeSchemaFetchParameters parameters) {
        requireNonNull(requestId, "The requestId can not be null");
        requireNonNull(id, "The id can not be null");

        var builder = MergeSchemaFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = new MergeFetchDetailsRequest(requestId, id, p);
        return client.fetchRequestDetails(request);
    }

    //
    // Start the merge schema and wait until the result is ready.
    //
    private MergeSchemaResult mergeSchemaAndFetch(String requestId, List<Schema> schemas, MergeSchemaParameters parameters) {
        requireNonNull(requestId, "requestId cannot be null");

        var mergeSchemaResponse = startMergeSchema(requestId, schemas, parameters, true);
        return waitForCompletion(requestId, mergeSchemaResponse, parameters).entity().results();
    }

    //
    // Starts the merge schema process.
    //
    private MergeSchemaResponse startMergeSchema(String requestId, List<Schema> schemas, MergeSchemaParameters parameters,
        boolean waitUntilJobIsDone) {
        requireNonNull(schemas, "schemas cannot be null");
        requireNonNull(requestId, "requestId cannot be null");

        String projectId = null;
        String spaceId = null;
        Parameters params = null;
        Duration timeout = this.timeout;
        String transactionId = null;

        if (nonNull(parameters)) {
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            params = parameters.toParameters(schemas);
            timeout = parameters.timeout() != null ? parameters.timeout() : timeout;
            transactionId = parameters.transactionId();
        } else {
            params = new Parameters(schemas, null);
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        var mergeSchemaRequest = new MergeSchemaRequest(projectId, spaceId, params);
        var request = new StartMergeSchemaRequest(requestId, transactionId, mergeSchemaRequest);
        var response = client.startRequest(request);

        if (!waitUntilJobIsDone)
            return response;

        Status status;
        long sleepTime = 100;
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        String processId = response.metadata().id();

        do {

            if (System.nanoTime() - deadlineNanos >= 0) {
                cleanUpAfterAbortedMerge(processId, projectId, spaceId, transactionId);
                throw new MergeSchemaException("timeout",
                    "Execution to merge schema took longer than the timeout set by %s milliseconds"
                        .formatted(timeout.toMillis()));
            }

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cleanUpAfterAbortedMerge(processId, projectId, spaceId, transactionId);
                throw new MergeSchemaException("interrupted", e.getMessage());
            }

            processId = response.metadata().id();
            response = fetchMergeSchemaRequest(requestId, processId, MergeSchemaFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .transactionId(transactionId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            logger.debug("Merge schema status: {} for request {}", status, processId);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    //
    // Cancels the started merge-schema job so that a timed-out or interrupted synchronous merge does not
    // leave an orphaned job behind.
    //
    private void cleanUpAfterAbortedMerge(String processId, String projectId, String spaceId, String transactionId) {
        if (nonNull(processId)) {
            deleteRequest(
                processId,
                MergeSchemaDeleteParameters.builder()
                    .projectId(projectId)
                    .spaceId(spaceId)
                    .transactionId(transactionId)
                    .build());
        }
    }

    //
    // Waits for the merge schema process to complete.
    //
    private MergeSchemaResponse waitForCompletion(String requestId, MergeSchemaResponse mergeSchemaResponse,
        MergeSchemaParameters parameters) {

        requireNonNull(requestId);

        Status status = Status.fromValue(mergeSchemaResponse.entity().results().status());

        return switch(status) {
            case COMPLETED -> mergeSchemaResponse;
            case FAILED -> {
                var error = mergeSchemaResponse.entity().results().error();
                throw new MergeSchemaException(error.code(), error.message());
            }
            default -> throw new MergeSchemaException("generic_error",
                "Status %s not managed".formatted(status));
        };
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * MergeSchemaService mergeSchemaService = MergeSchemaService.builder()
     *   .baseUrl("https://...")    // or use CloudRegion
     *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *   .projectId("project-id")
     *   .build();
     *
     * List<Schema> schemas = ...; // Get schemas from CreateSchemaService
     * MergeSchemaResult result = mergeSchemaService.mergeSchemaAndFetch(schemas);
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link MergeSchemaService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds an {@link MergeSchemaService} instance using the configured parameters.
         *
         * @return a new instance of {@link MergeSchemaService}
         */
        public MergeSchemaService build() {
            return new MergeSchemaService(this);
        }
    }
}
