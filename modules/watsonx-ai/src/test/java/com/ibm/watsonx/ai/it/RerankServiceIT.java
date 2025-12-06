/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.rerank.RerankService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class RerankServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final AuthenticationProvider authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final RerankService rerankService = RerankService.builder()
        .baseUrl(URL)
        .projectId(PROJECT_ID)
        .authenticationProvider(authentication)
        .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_valid_rerank_response_when_text_and_candidates_are_provided() {
        var response = rerankService.rerank("Rerank this!", List.of("Test to rerank 1", "Test to rerank 2"));
        assertNotNull(response);
        assertNotNull(response.createdAt());
        assertNotNull(response.inputTokenCount());
        assertNotNull(response.modelId());
        assertNotNull(response.results());
        assertNotNull(response.results().get(0));
        assertNotNull(response.results().get(0).index());
        assertNotNull(response.results().get(0).score());
        assertNotNull(response.results().get(1));
        assertNotNull(response.results().get(1).index());
        assertNotNull(response.results().get(1).score());
    }
}
