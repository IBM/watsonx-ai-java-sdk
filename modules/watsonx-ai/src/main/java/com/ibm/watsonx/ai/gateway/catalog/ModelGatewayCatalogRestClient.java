/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Model Gateway catalog APIs.
 */
public abstract class ModelGatewayCatalogRestClient extends WatsonxRestClient {

    protected ModelGatewayCatalogRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Lists all configured model details aggregated across all configured providers.
     *
     * @return a list of {@link ModelGatewayModel} instances
     */
    public abstract List<ModelGatewayModel> listModels();

    /**
     * Retrieves a specific model configuration by UUID or alias.
     *
     * @param modelId the UUID or alias of the model to retrieve
     * @return the {@link ModelGatewayModel} matching the given identifier
     */
    public abstract ModelGatewayModel getModel(String modelId);

    /**
     * Creates a new {@link Builder} using the first available {@link ModelGatewayCatalogRestClientBuilderFactory} discovered via
     * {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ModelGatewayCatalogRestClient.Builder builder() {
        return ServiceLoader.load(ModelGatewayCatalogRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ModelGatewayCatalogRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ModelGatewayCatalogRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ModelGatewayCatalogRestClientBuilderFactory extends Supplier<ModelGatewayCatalogRestClient.Builder> {}
}
