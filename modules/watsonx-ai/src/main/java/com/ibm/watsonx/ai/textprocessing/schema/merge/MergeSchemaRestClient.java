/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Merge Schema APIs.
 */
public abstract class MergeSchemaRestClient extends WatsonxRestClient {

    protected MergeSchemaRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Deletes a submitted merge schema request job.
     *
     * @param request The {@link DeleteRequest} containing request id and parameters.
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteRequest(DeleteRequest request);

    /**
     * Retrieves the details and results of a submitted merge schema request job.
     *
     * @param request The {@link MergeFetchDetailsRequest} containing request id and fetch parameters.
     * @return A {@link MergeSchemaResponse} containing the job status and results.
     */
    public abstract MergeSchemaResponse fetchRequestDetails(MergeFetchDetailsRequest request);

    /**
     * Starts a new merge schema request job.
     *
     * @param request The {@link StartMergeSchemaRequest} containing schema and parameters.
     * @return A {@link MergeSchemaResponse} representing the merge schema job.
     */
    public abstract MergeSchemaResponse startRequest(StartMergeSchemaRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link MergeSchemaRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static MergeSchemaRestClient.Builder builder() {
        return ServiceLoader.load(MergeSchemaRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link MergeSchemaRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<MergeSchemaRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface MergeSchemaRestClientBuilderFactory extends Supplier<MergeSchemaRestClient.Builder> {}
}
