/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.gateway;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingParameters;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingParameters.EncodingFormat;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var modelId = config.getValue("WATSONX_MODEL_ID", String.class);

        var embeddingService = ModelGatewayEmbeddingService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .modelId(modelId)
            .build();

        var inputs = java.util.List.of(
            "The quick brown fox jumps over the lazy dog",
            "IBM watsonx.ai provides enterprise AI solutions"
        );

        var parameters = ModelGatewayEmbeddingParameters.builder()
            .encodingFormat(EncodingFormat.FLOAT)
            .build();

        var response = embeddingService.embed(inputs, parameters);

        System.out.println("Model: " + response.model());
        System.out.println("Object: " + response.object());
        System.out.println();

        for (var embedding : response.data()) {
            System.out.println("Embedding #" + embedding.index());
            System.out.println("  Dimensions: " + embedding.embedding().size());
            System.out.println("  First 5 values: " + embedding.embedding().subList(0, Math.min(5, embedding.embedding().size())));
        }
    }
}
