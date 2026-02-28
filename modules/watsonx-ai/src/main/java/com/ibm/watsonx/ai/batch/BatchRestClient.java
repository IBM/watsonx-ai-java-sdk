/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Batches APIs.
 */
public abstract class BatchRestClient extends WatsonxRestClient {

    protected BatchRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Submits a new batch job using an uploaded input file.
     * <p>
     * The batch job will process the requests in the input file for the specified endpoint.
     *
     * @param batchCreateRequest the {@link BatchCreateRequest} object.
     * @return a {@link BatchData} object representing the submitted batch job.
     */
    public abstract BatchData submit(BatchCreateRequest batchCreateRequest);

    /**
     * Returns a list of batch jobs created.
     *
     * @param batchListRequest the {@link BatchListRequest} object.
     * @return a {@link BatchListResponse} containing the list of batch jobs and pagination metadata.
     */
    public abstract BatchListResponse list(BatchListRequest batchListRequest);

    /**
     * Retrieves details for a specific batch job.
     *
     * @param batchRetrieveRequest the {@link BatchRetrieveRequest} object.
     * @return a {@link BatchData} object with the batch job details.
     */
    public abstract BatchData retrieve(BatchRetrieveRequest batchRetrieveRequest);

    /**
     * Cancels an in-progress batch job.
     * <p>
     * The batch job will transition to the {@code cancelling} state and may take several minutes to reach the {@code cancelled} state. Partial
     * results, if available, will be preserved and accessible via the output file.
     *
     * @param batchCancelRequest the {@link BatchCancelRequest} object.
     * @return a {@link BatchData} object representing the batch job in its cancelling state.
     */
    public abstract BatchData cancel(BatchCancelRequest batchCancelRequest);

    /**
     * Creates a new {@link Builder} using the first available {@link BatchRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static BatchRestClient.Builder builder() {
        return ServiceLoader.load(BatchRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link BatchRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<BatchRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface BatchRestClientBuilderFactory extends Supplier<BatchRestClient.Builder> {}
}
