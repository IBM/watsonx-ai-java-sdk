/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelParameters;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class FoundationModelServiceIT {

    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final FoundationModelService foundationModelService = FoundationModelService.builder()
        .baseUrl(URL)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_all_models_with_valid_metadata() {
        var response = foundationModelService.getModels();
        assertNotNull(response.totalCount());
        assertNotNull(response.limit());
        assertNotNull(response.first());
        assertNotNull(response.first().href());
        assertNotNull(response.resources());
        assertTrue(response.resources().size() > 0);
    }

    @Test
    void should_return_filtered_model_when_filter_is_applied() {
        var response = foundationModelService.getModels(Filter.of(modelId("ibm/granite-4-h-small")));
        assertEquals(1, response.totalCount());
        assertNotNull(response.resources());
        assertEquals(1, response.resources().size());
    }

    @Test
    void should_return_all_tasks_with_valid_metadata() {
        var response = foundationModelService.getTasks();
        assertNotNull(response.totalCount());
        assertNotNull(response.limit());
        assertNotNull(response.first());
        assertNotNull(response.first().href());
        assertNotNull(response.resources());
        assertTrue(response.resources().size() > 0);
    }

    @Test
    void should_paginate_models_correctly() {
        var parameters = FoundationModelParameters.builder()
            .limit(1)
            .build();
        var response = foundationModelService.getModels(parameters);
        assertEquals(1, response.limit());
        assertEquals(1, response.next().start().orElse(null));
        var firstModelId = response.resources().get(0).modelId();
        assertNotNull(firstModelId);

        parameters = FoundationModelParameters.builder()
            .start(response.next().start().orElse(null))
            .limit(1)
            .build();

        response = foundationModelService.getModels(parameters);
        assertEquals(1, response.limit());
        assertEquals(2, response.next().start().orElse(null));
        var secondModelId = response.resources().get(0).modelId();
        assertNotNull(secondModelId);
        assertNotEquals(firstModelId, secondModelId);
    }
}
