/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Model Gateway image generation API.
 */
public abstract class ModelGatewayImageRestClient extends WatsonxRestClient {

    protected ModelGatewayImageRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Sends a synchronous image generation request to the Model Gateway.
     *
     * @param request the {@link ModelGatewayImageGenerationRequest} wire payload already containing model and all parameters
     * @return a {@link ModelGatewayImageResponse} containing the generated images
     */
    public abstract ModelGatewayImageResponse generate(ModelGatewayImageGenerationRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link ModelGatewayImageRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ModelGatewayImageRestClient.Builder builder() {
        return ServiceLoader.load(ModelGatewayImageRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ModelGatewayImageRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ModelGatewayImageRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ModelGatewayImageRestClientBuilderFactory extends Supplier<ModelGatewayImageRestClient.Builder> {}
}
