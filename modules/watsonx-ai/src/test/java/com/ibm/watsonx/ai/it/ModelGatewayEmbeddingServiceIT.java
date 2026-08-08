/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingParameters;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingParameters.EncodingFormat;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingRequest;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_GATEWAY_EMBEDDING_MODEL", matches = ".+")
public class ModelGatewayEmbeddingServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String URL = System.getenv("WATSONX_URL");
    static final String EMBEDDING_MODEL = System.getenv("WATSONX_GATEWAY_EMBEDDING_MODEL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final ModelGatewayEmbeddingService embeddingService = ModelGatewayEmbeddingService.builder()
        .baseUrl(URL)
        .authenticator(authentication)
        .modelId(EMBEDDING_MODEL)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_valid_response_for_single_input() {
        var response = embeddingService.embed("Hello, world!");
        assertNotNull(response);
        assertNotNull(response.data());
        assertFalse(response.data().isEmpty());
        List<Float> vector = response.data().get(0).embedding();
        assertNotNull(vector);
        assertFalse(vector.isEmpty());
        assertNull(response.data().get(0).base64());
        assertNotNull(response.usage());
    }

    @Test
    void should_return_valid_response_for_multiple_inputs() {
        var response = embeddingService.embed("Hello", "World");
        assertNotNull(response);
        assertNotNull(response.data());
        assertFalse(response.data().isEmpty());
    }

    @Test
    void should_return_float_encoding_format() {
        var parameters = ModelGatewayEmbeddingParameters.builder()
            .encodingFormat(EncodingFormat.FLOAT)
            .build();

        var response = embeddingService.embed(List.of("Hello, world!"), parameters);
        assertNotNull(response);
        assertFalse(response.data().isEmpty());
        assertEquals("list", response.object());
        // float format: the vector comes straight from the response and base64() stays null
        List<Float> vector = response.data().get(0).embedding();
        assertNotNull(vector);
        assertFalse(vector.isEmpty());
        assertNull(response.data().get(0).base64());
    }

    @Test
    void should_return_base64_encoding_format() {
        var request = ModelGatewayEmbeddingRequest.builder()
            .input("Hello, world!")
            .parameters(
                ModelGatewayEmbeddingParameters.builder()
                    .encodingFormat(EncodingFormat.BASE64)
                    .build()
            ).build();

        var response = embeddingService.embed(request);
        assertNotNull(response);
        assertFalse(response.data().isEmpty());
        assertEquals("list", response.object());
        assertNotNull(response.usage());
        // base64 format: the raw payload is kept and embedding() exposes it already decoded
        String base64 = response.data().get(0).base64();
        assertNotNull(base64);
        assertFalse(base64.isEmpty());
        List<Float> vector = response.data().get(0).embedding();
        assertNotNull(vector);
        assertFalse(vector.isEmpty());
    }
}
