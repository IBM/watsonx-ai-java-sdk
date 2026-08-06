/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

/**
 * Represents a model configuration returned by the IBM watsonx.ai Model Gateway.
 *
 * @param uuid the unique identifier for the model
 * @param object the object type, always {@code "model"}
 * @param created the Unix timestamp (in seconds) when the model configuration was created
 * @param ownedBy the provider that owns the model, in the format {@code "<provider_type>:<provider_name>"}
 * @param id the official provider-specific server-side unique identifier of the model instance
 * @param alias an optional friendly name for the model
 * @param description a custom user-defined description for the model
 * @param metadata additional configuration for the model
 */
public record ModelGatewayModel(
    String uuid,
    String object,
    Long created,
    String ownedBy,
    String id,
    String alias,
    String description,
    Metadata metadata) {

    /**
     * Additional configuration for a model.
     *
     * @param cost the cost per 1000 tokens for the model, in USD
     * @param modelFamily the family or series this model belongs to
     * @param recommenderLabel the label used by the Recommender API to map to supported models
     * @param region the region where this model is deployed
     * @param batch whether the model is eligible or preferred for batch requests
     * @param contextWindow the maximum number of tokens the model can process in a single request
     */
    public record Metadata(
        Double cost,
        String modelFamily,
        String recommenderLabel,
        String region,
        Boolean batch,
        Integer contextWindow) {}
}
