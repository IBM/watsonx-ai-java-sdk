/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Foundation Model APIs.
 */
public abstract class FoundationModelRestClient extends WatsonxRestClient {

    protected FoundationModelRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Retrieves a list of foundation models from the service.
     *
     * @param start A pagination token for fetching the next set of results (optional).
     * @param limit The number of models to return (1–200). Defaults to 100 if null.
     * @param transactionId Optional transaction ID for tracking the request.
     * @param techPreview {@code true} to include tech preview models, {@code false} otherwise.
     * @param filters A string expression for filtering models using logical combinations of filters.
     * @return A {@link FoundationModelResponse} containing the list of foundation models.
     */
    public abstract FoundationModelResponse<FoundationModel> getModels(
        Integer start,
        Integer limit,
        String transactionId,
        Boolean techPreview,
        String filters);

    /**
     * Retrieves a list of foundation model tasks.
     *
     * @param parameters Parameters to customize the get tasks operation.
     * @return A {@link FoundationModelResponse} containing the list of foundation model tasks.
     */
    public abstract FoundationModelResponse<FoundationModelTask> getTasks(FoundationModelParameters parameters);

    /**
     * Creates a new {@link Builder} using the first available {@link FoundationModelRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static FoundationModelRestClient.Builder builder() {
        return ServiceLoader.load(FoundationModelRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link FoundationModelRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<FoundationModelRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface FoundationModelRestClientBuilderFactory extends Supplier<FoundationModelRestClient.Builder> {}
}
