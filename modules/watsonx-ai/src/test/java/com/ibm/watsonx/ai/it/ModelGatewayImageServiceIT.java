/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Quality;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Size;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_GATEWAY_IMAGE_MODEL", matches = ".+")
public class ModelGatewayImageServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String URL = System.getenv("WATSONX_URL");
    static final String IMAGE_MODEL = System.getenv("WATSONX_GATEWAY_IMAGE_MODEL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final ModelGatewayImageService imageService = ModelGatewayImageService.builder()
        .baseUrl(URL)
        .authenticator(authentication)
        .modelId(IMAGE_MODEL)
        .logRequests(true)
        .logResponses(true)
        .timeout(Duration.ofMinutes(2))
        .build();

    @Test
    void should_return_valid_response_for_single_prompt() {
        var response = imageService.generate("A futuristic city at sunset");
        assertNotNull(response);
        assertTrue(response.created() > 0);
        assertNotNull(response.data());
        assertFalse(response.data().isEmpty());
    }

    @Test
    void should_return_b64_json_response_format() {
        var parameters = ModelGatewayImageParameters.builder()
            .size(Size.SIZE_1024X1024)
            .quality(Quality.LOW)
            .build();

        var response = imageService.generate("A serene mountain landscape", parameters);
        assertNotNull(response);
        assertFalse(response.data().isEmpty());
        String b64Json = response.data().get(0).b64Json();
        assertNull(response.data().get(0).url());
        assertNotNull(b64Json);
        assertFalse(b64Json.isEmpty());
    }
}
