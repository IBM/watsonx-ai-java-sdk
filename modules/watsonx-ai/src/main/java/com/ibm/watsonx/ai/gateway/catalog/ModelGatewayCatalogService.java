/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with the IBM watsonx.ai Model Gateway catalog APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayCatalogService catalogService = ModelGatewayCatalogService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .build();
 *
 * List<ModelGatewayModel> models = catalogService.listModels();
 * ModelGatewayModel model = catalogService.getModel("gpt-4o");  // alias or UUID
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ModelGatewayCatalogService extends WatsonxService {

    private final ModelGatewayCatalogRestClient client;

    private ModelGatewayCatalogService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = ModelGatewayCatalogRestClient.builder()
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
     * Lists all configured model details aggregated across all configured providers.
     *
     * @return a list of {@link ModelGatewayModel} instances
     */
    public List<ModelGatewayModel> listModels() {
        return client.listModels();
    }

    /**
     * Retrieves a specific model configuration by UUID or alias.
     *
     * @param modelId the UUID or alias of the model to retrieve
     * @return the {@link ModelGatewayModel} matching the given identifier
     */
    public ModelGatewayModel getModel(String modelId) {
        return client.getModel(modelId);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayCatalogService} instances with configurable parameters.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ModelGatewayCatalogService} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayCatalogService}
         */
        public ModelGatewayCatalogService build() {
            return new ModelGatewayCatalogService(this);
        }
    }
}
