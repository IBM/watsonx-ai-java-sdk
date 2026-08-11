/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.gateway.catalog.ModelGatewayCatalogService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class ModelGatewayCatalogServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String URL = System.getenv("WATSONX_URL");

    @Test
    void should_return_model_list() {

        var modelGatewayCatalogService = ModelGatewayCatalogService.builder()
            .apiKey(API_KEY)
            .baseUrl(URL)
            .logRequests(true)
            .logResponses(true)
            .build();

        var models = modelGatewayCatalogService.listModels();
        assertTrue(models.size() > 0);
        assertNull(models.get(0).alias());
        assertNotNull(models.get(0).created());
        assertNull(models.get(0).description());
        assertNotNull(models.get(0).id());
        assertNotNull(models.get(0).uuid());
        assertNotNull(models.get(0).object());
        assertNotNull(models.get(0).ownedBy());
        assertNull(models.get(0).metadata());
    }

    @Test
    void should_return_model_by_id() {

        var modelGatewayCatalogService = ModelGatewayCatalogService.builder()
            .apiKey(API_KEY)
            .baseUrl(URL)
            .logRequests(true)
            .logResponses(true)
            .build();

        var models = modelGatewayCatalogService.listModels();
        assertTrue(models.size() > 0);

        var model = modelGatewayCatalogService.getModel(models.get(0).uuid());
        assertEquals(models.get(0).alias(), model.alias());
        assertEquals(models.get(0).created(), model.created());
        assertEquals(models.get(0).description(), model.description());
        assertEquals(models.get(0).id(), model.id());
        assertEquals(models.get(0).metadata(), model.metadata());
        assertEquals(models.get(0).object(), model.object());
        assertEquals(models.get(0).ownedBy(), model.ownedBy());
        assertEquals(models.get(0).uuid(), model.uuid());
    }
}
