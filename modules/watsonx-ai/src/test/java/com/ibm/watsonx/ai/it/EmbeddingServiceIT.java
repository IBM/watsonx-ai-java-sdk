/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.embedding.EmbeddingService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class EmbeddingServiceIT {


    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final EmbeddingService embeddingService = EmbeddingService.builder()
        .baseUrl(URL)
        .projectId(PROJECT_ID)
        .authenticator(authentication)
        .modelId("ibm/granite-embedding-278m-multilingual")
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_valid_embedding_response_when_text_is_provided() {
        var response = embeddingService.embedding("Embedding this!");
        assertNotNull(response);
        assertNotNull(response.createdAt());
        assertNotNull(response.inputTokenCount());
        assertNotNull(response.modelId());
        assertNotNull(response.results());
        assertTrue(response.results().size() > 0);
    }
}
