/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.tokenization.TokenizationParameters;
import com.ibm.watsonx.ai.tokenization.TokenizationService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class TokenizationServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final AuthenticationProvider authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final TokenizationService tokenizationService = TokenizationService.builder()
        .baseUrl(URL)
        .projectId(PROJECT_ID)
        .authenticationProvider(authentication)
        .modelId("ibm/granite-4-h-small")
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_tokens_synchronously_when_text_is_provided() {

        var response = tokenizationService.tokenize(
            "Tokenize this!",
            TokenizationParameters.builder()
                .returnTokens(true)
                .build());

        assertNotNull(response);
        assertNotNull(response.modelId());
        assertNotNull(response.result());
        assertNotNull(response.result().tokenCount());
        assertNotNull(response.result().tokens());
        assertTrue(response.result().tokens().size() > 0);
    }

    @Test
    void should_return_tokens_asynchronously_when_text_is_provided() throws Exception {

        var response = tokenizationService.asyncTokenize(
            "Tokenize this!",
            TokenizationParameters.builder()
                .returnTokens(true)
                .build())
            .get(3, TimeUnit.SECONDS);

        assertNotNull(response);
        assertNotNull(response.modelId());
        assertNotNull(response.result());
        assertNotNull(response.result().tokenCount());
        assertNotNull(response.result().tokens());
        assertTrue(response.result().tokens().size() > 0);
    }
}
