/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class FoudationModelServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final AuthenticationProvider authentication = IAMAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final FoundationModelService foundationModelService = FoundationModelService.builder()
        .baseUrl(URL)
        .authenticationProvider(authentication)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void test_get_models() {
        var response = foundationModelService.getModels();
        assertNotNull(response.totalCount());
        assertNotNull(response.limit());
        assertNotNull(response.first());
        assertNotNull(response.first().href());
        assertNotNull(response.resources());
        assertTrue(response.resources().size() > 0);
    }

    @Test
    void test_get_tasks() {
        var response = foundationModelService.getTasks();
        assertNotNull(response.totalCount());
        assertNotNull(response.limit());
        assertNotNull(response.first());
        assertNotNull(response.first().href());
        assertNotNull(response.resources());
        assertTrue(response.resources().size() > 0);
    }
}
