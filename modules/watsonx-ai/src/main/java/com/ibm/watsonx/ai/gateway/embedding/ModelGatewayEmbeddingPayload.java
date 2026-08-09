/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import java.util.List;

/**
 * Payload request for the Model Gateway embeddings endpoint.
 *
 * @param model the model identifier
 * @param input the input texts to embed, copied and exposed as an unmodifiable list
 * @param dimensions the number of dimensions for the output embeddings, or {@code null} if not set
 * @param encodingFormat the format in which the embeddings are returned, or {@code null} if not set
 * @param user a unique identifier representing the end-user, or {@code null} if not set
 */
public record ModelGatewayEmbeddingPayload(
    String model,
    List<String> input,
    Integer dimensions,
    String encodingFormat,
    String user) {

    public ModelGatewayEmbeddingPayload {
        input = isNull(input) ? null : List.copyOf(input);
    }

    /**
     * Creates the payload for the given model by flattening a request and its parameters.
     *
     * @param model the model identifier
     * @param request the {@link ModelGatewayEmbeddingRequest} containing the input and optional parameters
     * @return a new {@link ModelGatewayEmbeddingPayload}
     */
    static ModelGatewayEmbeddingPayload of(String model, ModelGatewayEmbeddingRequest request) {

        var parameters = requireNonNullElse(request.parameters(), ModelGatewayEmbeddingParameters.builder().build());

        return new ModelGatewayEmbeddingPayload(
            model,
            request.input(),
            parameters.dimensions(),
            parameters.encodingFormat(),
            parameters.user());
    }
}
