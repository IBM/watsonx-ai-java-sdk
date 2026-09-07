/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.Status;

/**
 * Service class to interact with IBM watsonx.ai Cluster Schema APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ClusterSchemaService clusterSchemaService = ClusterSchemaService.builder()
 *     .baseUrl("https://...")    // or use CloudRegion
 *     .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *     .projectId("project-id")
 *     .build();
 *
 * var clustered = clusterSchemaService.clusterSchemaAndFetch(schema1, schema2);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ClusterSchemaService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSchemaService.class);
    private final ClusterSchemaRestClient client;

    ClusterSchemaService() {
        super();
        client = null;
    }

    private ClusterSchemaService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = ClusterSchemaRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(TIME_OUT)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Starts a cluster schema request for the given schemas.
     *
     * @param schemas the document schemas to cluster
     * @return a {@link ClusterSchemaResponse} representing the submitted request and its current status
     */
    public ClusterSchemaResponse startClusterSchema(ClusterSchemas... schemas) throws ClusterSchemaException {
        return startClusterSchema(null, List.of(schemas));
    }

    /**
     * Starts a cluster schema request for the given schemas with optional parameters.
     *
     * @param parameters the configuration parameters for cluster schema
     * @param schemas the document schemas to cluster
     * @return a {@link ClusterSchemaResponse} representing the submitted request and its current status
     */
    public ClusterSchemaResponse startClusterSchema(ClusterSchemaParameters parameters, ClusterSchemas... schemas) throws ClusterSchemaException {
        return startClusterSchema(parameters, List.of(schemas));
    }

    /**
     * Starts a cluster schema request for the given list of schemas.
     *
     * @param schemas the document schemas to cluster
     * @return a {@link ClusterSchemaResponse} representing the submitted request and its current status
     */
    public ClusterSchemaResponse startClusterSchema(List<ClusterSchemas> schemas) throws ClusterSchemaException {
        return startClusterSchema(null, schemas);
    }

    /**
     * Starts a cluster schema request for the given list of schemas with optional parameters.
     *
     * @param parameters the configuration parameters for cluster schema
     * @param schemas the document schemas to cluster
     * @return a {@link ClusterSchemaResponse} representing the submitted request and its current status
     */
    public ClusterSchemaResponse startClusterSchema(ClusterSchemaParameters parameters, List<ClusterSchemas> schemas) throws ClusterSchemaException {
        return startClusterSchema(UUID.randomUUID().toString(), parameters, schemas, false);
    }

    /**
     * Starts a cluster schema request and waits until the clustering is complete.
     *
     * @param schemas the document schemas to cluster
     * @return a list of clusters, where each cluster contains the semantically similar {@link ClusterSchemas}
     */
    public List<List<ClusterSchemas>> clusterSchemaAndFetch(ClusterSchemas... schemas) throws ClusterSchemaException {
        return clusterSchemaAndFetch(null, List.of(schemas));
    }

    /**
     * Starts a cluster schema request and waits until the clustering is complete.
     *
     * @param parameters the configuration parameters for cluster schema
     * @param schemas the document schemas to cluster
     * @return a list of clusters, where each cluster contains the semantically similar {@link ClusterSchemas}
     */
    public List<List<ClusterSchemas>> clusterSchemaAndFetch(ClusterSchemaParameters parameters, ClusterSchemas... schemas)
        throws ClusterSchemaException {
        return clusterSchemaAndFetch(parameters, List.of(schemas));
    }

    /**
     * Starts a cluster schema request and waits until the clustering is complete.
     *
     * @param schemas the document schemas to cluster
     * @return a list of clusters, where each cluster contains the semantically similar {@link ClusterSchemas}
     */
    public List<List<ClusterSchemas>> clusterSchemaAndFetch(List<ClusterSchemas> schemas) throws ClusterSchemaException {
        return clusterSchemaAndFetch(null, schemas);
    }

    /**
     * Starts a cluster schema request and waits until the clustering is complete.
     *
     * @param parameters the configuration parameters for cluster schema
     * @param schemas the document schemas to cluster
     * @return a list of clusters, where each cluster contains the semantically similar {@link ClusterSchemas}
     */
    public List<List<ClusterSchemas>> clusterSchemaAndFetch(ClusterSchemaParameters parameters, List<ClusterSchemas> schemas)
        throws ClusterSchemaException {
        var requestId = UUID.randomUUID().toString();
        var response = startClusterSchema(requestId, parameters, schemas, true);
        return response.entity().results().schemas();
    }

    /**
     * Retrieves the results of a cluster schema request by its unique identifier.
     * <p>
     * Note that the retention period for results is 2 days. If the request is older than 2 days, the results will no longer be available.
     *
     * @param id the unique identifier of the cluster schema request
     * @return a {@link ClusterSchemaResponse} containing the results of the request
     */
    public ClusterSchemaResponse fetchRequest(String id) {
        return fetchRequest(id, ClusterSchemaFetchParameters.builder().build());
    }

    /**
     * Retrieves the results of a cluster schema request by its unique identifier.
     * <p>
     * Note that the retention period for results is 2 days. If the request is older than 2 days, the results will no longer be available.
     *
     * @param id the unique identifier of the cluster schema request
     * @param parameters parameters to specify the project or space context in which the request was made
     * @return a {@link ClusterSchemaResponse} containing the results of the request
     */
    public ClusterSchemaResponse fetchRequest(String id, ClusterSchemaFetchParameters parameters) {
        requireNonNull(parameters, "parameters cannot be null");
        return fetchClusterSchemaRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Deletes a cluster schema request.
     *
     * @param id the unique identifier of the cluster schema request to delete
     * @return {@code true} if the request was successfully deleted, {@code false} otherwise
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, ClusterSchemaDeleteParameters.builder().build());
    }

    /**
     * Deletes a cluster schema request.
     * <p>
     * If the {@code hardDelete} parameter is set to {@code true}, it will also delete the associated job metadata.
     *
     * @param id the unique identifier of the cluster schema request to delete
     * @param parameters parameters specifying the space or project context, and whether to perform a hard delete
     * @return {@code true} if the request was successfully deleted, {@code false} otherwise
     */
    public boolean deleteRequest(String id, ClusterSchemaDeleteParameters parameters) {
        requireNonNull(id, "The id cannot be null");
        requireNonNull(parameters, "parameters cannot be null");

        var builder = ClusterSchemaDeleteParameters.builder();
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
    // Retrieves the results of a cluster schema request by its unique identifier.
    //
    private ClusterSchemaResponse fetchClusterSchemaRequest(String requestId, String id, ClusterSchemaFetchParameters parameters) {
        requireNonNull(requestId, "The requestId cannot be null");
        requireNonNull(id, "The id cannot be null");

        var builder = ClusterSchemaFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = new FetchDetailsRequest(requestId, id, p);
        return client.fetchRequestDetails(request);
    }

    //
    // Starts the cluster schema process and optionally waits for completion.
    //
    private ClusterSchemaResponse startClusterSchema(String requestId, ClusterSchemaParameters parameters, List<ClusterSchemas> schemas,
        boolean waitUntilJobIsDone) throws ClusterSchemaException {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(schemas, "schemas cannot be null");

        String projectId = null;
        String spaceId = null;
        Parameters params;
        String transactionId = null;

        if (nonNull(parameters)) {
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            var effectiveSchemas = requireNonNullElse(parameters.schemas(), schemas);
            Parameters.SemanticConfig semanticConfigRecord = null;
            if (parameters.semanticConfig() != null)
                semanticConfigRecord = new Parameters.SemanticConfig(parameters.semanticConfig().defaultModelName());
            params = new Parameters(effectiveSchemas, semanticConfigRecord);
            transactionId = parameters.transactionId();
        } else {
            params = new Parameters(schemas, null);
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        var clusterSchemaRequest = new ClusterSchemaRequest(projectId, spaceId, params);
        var request = new StartClusterSchemaRequest(requestId, transactionId, clusterSchemaRequest);
        var response = client.startRequest(request);

        if (!waitUntilJobIsDone)
            return response;

        Status status = Status.fromValue(response.entity().results().status());
        if (status == Status.COMPLETED)
            return response;

        if (status == Status.FAILED) {
            var error = response.entity().results().error();
            if (isNull(error))
                throw new ClusterSchemaException("generic_error", "The cluster schema failed without error details");
            throw new ClusterSchemaException(error.code(), error.message());
        }

        long sleepTime = 100;
        long deadlineNanos = System.nanoTime() + this.timeout.toNanos();
        String processId = response.metadata().id();

        do {

            if (System.nanoTime() - deadlineNanos >= 0) {
                deleteRequest(
                    processId,
                    ClusterSchemaDeleteParameters.builder()
                        .projectId(projectId)
                        .spaceId(spaceId)
                        .transactionId(transactionId)
                        .build());
                throw new ClusterSchemaException("timeout",
                    "Execution of cluster schema took longer than the timeout set by %s milliseconds".formatted(this.timeout.toMillis()));
            }

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ClusterSchemaException("interrupted", e.getMessage());
            }

            processId = response.metadata().id();
            response = fetchClusterSchemaRequest(requestId, processId, ClusterSchemaFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            logger.debug("Cluster schema status: {}", status);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        if (status == Status.FAILED) {
            var error = response.entity().results().error();
            if (isNull(error))
                throw new ClusterSchemaException("generic_error", "The cluster schema failed without error details");
            throw new ClusterSchemaException(error.code(), error.message());
        }

        return response;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ClusterSchemaService clusterSchemaService = ClusterSchemaService.builder()
     *     .baseUrl("https://...")
     *     .apiKey("my-api-key")
     *     .projectId("project-id")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ClusterSchemaService} instances with configurable parameters.
     */
    public static final class Builder extends ProjectService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ClusterSchemaService} instance using the configured parameters.
         *
         * @return a new instance of {@link ClusterSchemaService}
         */
        public ClusterSchemaService build() {
            return new ClusterSchemaService(this);
        }
    }
}
