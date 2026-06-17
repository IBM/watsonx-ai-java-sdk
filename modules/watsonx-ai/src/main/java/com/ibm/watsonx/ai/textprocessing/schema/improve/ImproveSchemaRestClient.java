/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Improve Schema APIs.
 */
public abstract class ImproveSchemaRestClient extends WatsonxRestClient {

    protected ImproveSchemaRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Deletes a submitted improve schema request job.
     *
     * @param request The {@link DeleteRequest} containing request id and parameters.
     * @return {@code true} if the job was successfully deleted, {@code false} otherwise.
     */
    public abstract boolean deleteRequest(DeleteRequest request);

    /**
     * Retrieves the details and results of a submitted improve schema request job.
     *
     * @param request The {@link ImproveFetchDetailsRequest} containing request id and fetch parameters.
     * @return A {@link ImproveSchemaResponse} containing the job status and results.
     */
    public abstract ImproveSchemaResponse fetchRequestDetails(ImproveFetchDetailsRequest request);

    /**
     * Starts a new improve schema request job.
     *
     * @param request The {@link StartImproveSchemaRequest} containing schema and parameters.
     * @return A {@link ImproveSchemaResponse} representing the improve schema job.
     */
    public abstract ImproveSchemaResponse startRequest(StartImproveSchemaRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link ImproveSchemaRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ImproveSchemaRestClient.Builder builder() {
        return ServiceLoader.load(ImproveSchemaRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ImproveSchemaRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ImproveSchemaRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ImproveSchemaRestClientBuilderFactory extends Supplier<ImproveSchemaRestClient.Builder> {}
}
