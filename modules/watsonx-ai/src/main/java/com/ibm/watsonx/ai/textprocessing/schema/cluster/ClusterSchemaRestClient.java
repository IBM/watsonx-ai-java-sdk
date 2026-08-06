/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Cluster Schema APIs.
 */
public abstract class ClusterSchemaRestClient extends WatsonxRestClient {

    protected ClusterSchemaRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Deletes a submitted cluster schema request job.
     *
     * @param request the {@link DeleteRequest} containing request id and parameters
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise
     */
    public abstract boolean deleteRequest(DeleteRequest request);

    /**
     * Retrieves the details and results of a submitted cluster schema request job.
     *
     * @param request the {@link FetchDetailsRequest} containing request id and fetch parameters
     * @return a {@link ClusterSchemaResponse} containing the job status and results
     */
    public abstract ClusterSchemaResponse fetchRequestDetails(FetchDetailsRequest request);

    /**
     * Starts a new cluster schema request job.
     *
     * @param request the {@link StartClusterSchemaRequest} containing the schemas and parameters
     * @return a {@link ClusterSchemaResponse} representing the created cluster schema job
     */
    public abstract ClusterSchemaResponse startRequest(StartClusterSchemaRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link ClusterSchemaRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ClusterSchemaRestClient.Builder builder() {
        return ServiceLoader.load(ClusterSchemaRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ClusterSchemaRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ClusterSchemaRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ClusterSchemaRestClientBuilderFactory extends Supplier<ClusterSchemaRestClient.Builder> {}
}
