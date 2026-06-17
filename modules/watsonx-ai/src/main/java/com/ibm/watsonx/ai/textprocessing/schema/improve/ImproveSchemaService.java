/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaResponse.ImproveSchemaResult;

/**
 * Service class to interact with IBM watsonx.ai Improve Schema APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ImproveSchemaService improveSchemaService = ImproveSchemaService.builder()
 *   .baseUrl("https://...")    // or use CloudRegion
 *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *   .projectId("project-id")
 *   .build();
 *
 * Schema existingSchema = ...; // Get schema from CreateSchemaService
 * ImproveSchemaResult result = improveSchemaService.improveSchemaAndFetch(existingSchema);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ImproveSchemaService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ImproveSchemaService.class);
    private final ImproveSchemaRestClient client;

    private ImproveSchemaService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = ImproveSchemaRestClient.builder()
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
     * Starts the improve schema process for an existing schema.
     *
     * @param schema The schema to improve.
     * @return An {@link ImproveSchemaResponse} representing the submitted request and its current status.
     */
    public ImproveSchemaResponse startImproveSchema(Schema schema) {
        requireNonNull(schema, "schema cannot be null");
        return startImproveSchema(schema, null);
    }

    /**
     * Starts the improve schema process for an existing schema.
     *
     * @param schema The schema to improve.
     * @param parameters The configuration parameters for improve schema.
     * @return An {@link ImproveSchemaResponse} representing the submitted request and its current status.
     */
    public ImproveSchemaResponse startImproveSchema(Schema schema, ImproveSchemaParameters parameters) {
        requireNonNull(schema, "schema cannot be null");
        return startImproveSchema(UUID.randomUUID().toString(), schema, parameters, false);
    }

    /**
     * Starts the improve schema process and waits until the schema is improved.
     *
     * @param schema The schema to improve.
     * @return An {@link ImproveSchemaResult} with the improved schema.
     * @see #improveSchemaAndFetch(Schema, ImproveSchemaParameters)
     */
    public ImproveSchemaResult improveSchemaAndFetch(Schema schema) {
        requireNonNull(schema, "schema cannot be null");
        return improveSchemaAndFetch(schema, null);
    }

    /**
     * Starts the improve schema process and waits until the schema is improved.
     *
     * @param schema The schema to improve.
     * @param parameters The configuration parameters for improve schema.
     * @return An {@link ImproveSchemaResult} with the improved schema.
     */
    public ImproveSchemaResult improveSchemaAndFetch(Schema schema, ImproveSchemaParameters parameters) {
        requireNonNull(schema, "schema cannot be null");
        return improveSchemaAndFetch(UUID.randomUUID().toString(), schema, parameters);
    }

    /**
     * Retrieves the results of an improve schema request by its unique identifier.
     *
     * @param id The unique identifier of the improve schema request.
     * @return An {@link ImproveSchemaResponse} containing the results of the request.
     */
    public ImproveSchemaResponse fetchRequest(String id) {
        requireNonNull(id, "id cannot be null");
        return fetchRequest(id, ImproveSchemaFetchParameters.builder().build());
    }

    /**
     * Retrieves the results of an improve schema request by its unique identifier.
     *
     * @param id The unique identifier of the improve schema request.
     * @param parameters Parameters to specify the project or space context in which the request was made.
     * @return An {@link ImproveSchemaResponse} containing the results of the request.
     */
    public ImproveSchemaResponse fetchRequest(String id, ImproveSchemaFetchParameters parameters) {
        requireNonNull(id, "id cannot be null");
        return fetchImproveSchemaRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Deletes an improve schema request.
     *
     * @param id The unique identifier of the improve schema request to delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, ImproveSchemaDeleteParameters.builder().build());
    }

    /**
     * Deletes an improve schema request.
     *
     * @param id The unique identifier of the improve schema request to delete.
     * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id, ImproveSchemaDeleteParameters parameters) {

        requireNonNull(id, "The id can not be null");

        var builder = ImproveSchemaDeleteParameters.builder();
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
    // Retrieves the results of an improve schema request by its unique identifier.
    //
    private ImproveSchemaResponse fetchImproveSchemaRequest(String requestId, String id, ImproveSchemaFetchParameters parameters) {
        requireNonNull(requestId, "The requestId can not be null");
        requireNonNull(id, "The id can not be null");

        var builder = ImproveSchemaFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = new ImproveFetchDetailsRequest(requestId, id, p);
        return client.fetchRequestDetails(request);
    }

    //
    // Start the improve schema and wait until the result is ready.
    //
    private ImproveSchemaResult improveSchemaAndFetch(String requestId, Schema schema, ImproveSchemaParameters parameters) {
        requireNonNull(requestId, "requestId cannot be null");

        var improveSchemaResponse = startImproveSchema(requestId, schema, parameters, true);
        return waitForCompletion(requestId, improveSchemaResponse, parameters).entity().results();
    }

    //
    // Starts the improve schema process.
    //
    private ImproveSchemaResponse startImproveSchema(String requestId, Schema schema, ImproveSchemaParameters parameters,
        boolean waitUntilJobIsDone) {
        requireNonNull(schema, "schema cannot be null");
        requireNonNull(requestId, "requestId cannot be null");

        String projectId = null;
        String spaceId = null;
        Parameters params = null;
        Duration timeout = this.timeout;
        String transactionId = null;

        if (nonNull(parameters)) {
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            params = parameters.toParameters(schema);
            timeout = parameters.timeout() != null ? parameters.timeout() : timeout;
            transactionId = parameters.transactionId();
        } else {
            params = new Parameters(schema, null);
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        var improveSchemaRequest = new ImproveSchemaRequest(projectId, spaceId, params);
        var request = new StartImproveSchemaRequest(requestId, transactionId, improveSchemaRequest);
        var response = client.startRequest(request);

        if (!waitUntilJobIsDone)
            return response;

        Status status;
        long sleepTime = 100;
        LocalTime endTime = LocalTime.now().plus(timeout);
        String processId = null;

        do {

            if (LocalTime.now().isAfter(endTime)) {

                if (nonNull(processId)) {
                    deleteRequest(
                        processId,
                        ImproveSchemaDeleteParameters.builder()
                            .projectId(projectId)
                            .spaceId(spaceId)
                            .transactionId(transactionId)
                            .build()
                    );
                }

                throw new ImproveSchemaException("timeout",
                    "Execution to improve schema took longer than the timeout set by %s milliseconds"
                        .formatted(timeout.toMillis()));
            }

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (Exception e) {
                throw new ImproveSchemaException("interrupted", e.getMessage());
            }

            processId = response.metadata().id();
            response = fetchImproveSchemaRequest(requestId, processId, ImproveSchemaFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .transactionId(transactionId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            logger.debug("Improve schema status: {} for request {}", status, processId);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    //
    // Waits for the improve schema process to complete.
    //
    private ImproveSchemaResponse waitForCompletion(String requestId, ImproveSchemaResponse improveSchemaResponse,
        ImproveSchemaParameters parameters) {

        requireNonNull(requestId);

        Status status = Status.fromValue(improveSchemaResponse.entity().results().status());

        return switch(status) {
            case COMPLETED -> improveSchemaResponse;
            case FAILED -> {
                var error = improveSchemaResponse.entity().results().error();
                throw new ImproveSchemaException(error.code(), error.message());
            }
            default -> throw new ImproveSchemaException("generic_error",
                "Status %s not managed".formatted(status));
        };
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ImproveSchemaService improveSchemaService = ImproveSchemaService.builder()
     *   .baseUrl("https://...")    // or use CloudRegion
     *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *   .projectId("project-id")
     *   .build();
     *
     * Schema existingSchema = ...; // Get schema from CreateSchemaService
     * ImproveSchemaResult result = improveSchemaService.improveSchemaAndFetch(existingSchema);
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ImproveSchemaService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds an {@link ImproveSchemaService} instance using the configured parameters.
         *
         * @return a new instance of {@link ImproveSchemaService}
         */
        public ImproveSchemaService build() {
            return new ImproveSchemaService(this);
        }
    }
}
