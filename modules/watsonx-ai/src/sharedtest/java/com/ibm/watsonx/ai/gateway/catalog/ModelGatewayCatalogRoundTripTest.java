/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.gateway.catalog.ModelGatewayModel.Metadata;

@DisabledInNativeImage
public class ModelGatewayCatalogRoundTripTest {

    private static final Metadata METADATA = new Metadata(0.5, "granite", "recommended", "us-south", true, 8192);

    @Test
    void should_serialize_and_deserialize_a_gateway_model() {

        var EXPECTED = """
            {
                "uuid": "11111111-1111-1111-1111-111111111111",
                "object": "model",
                "created": 1735689600,
                "owned_by": "ibm",
                "id": "ibm/granite-3-3-8b-instruct",
                "alias": "granite-3-3-8b",
                "description": "A general purpose instruct model",
                "metadata": {
                    "cost": 0.5,
                    "model_family": "granite",
                    "recommender_label": "recommended",
                    "region": "us-south",
                    "batch": true,
                    "context_window": 8192
                }
            }""";

        var model = new ModelGatewayModel(
            "11111111-1111-1111-1111-111111111111", "model", 1735689600L, "ibm",
            "ibm/granite-3-3-8b-instruct", "granite-3-3-8b", "A general purpose instruct model", METADATA);

        var json = Json.toJson(model);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertTrue(json.contains("\"owned_by\""));
        assertFalse(json.contains("\"ownedBy\""));

        var roundTripped = Json.fromJson(json, ModelGatewayModel.class);
        assertEquals(model, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_model_metadata() {

        var EXPECTED = """
            {
                "cost": 0.5,
                "model_family": "granite",
                "recommender_label": "recommended",
                "region": "us-south",
                "batch": true,
                "context_window": 8192
            }""";

        var json = Json.toJson(METADATA);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, Metadata.class);
        assertEquals(METADATA, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_list_models_response() {

        var EXPECTED = """
            {
                "object": "list",
                "data": [
                    {
                        "uuid": "11111111-1111-1111-1111-111111111111",
                        "object": "model",
                        "created": 1735689600,
                        "owned_by": "ibm",
                        "id": "ibm/granite-3-3-8b-instruct",
                        "alias": "granite-3-3-8b",
                        "description": "A general purpose instruct model",
                        "metadata": {
                            "cost": 0.5,
                            "model_family": "granite",
                            "recommender_label": "recommended",
                            "region": "us-south",
                            "batch": true,
                            "context_window": 8192
                        }
                    }
                ]
            }""";

        var model = new ModelGatewayModel(
            "11111111-1111-1111-1111-111111111111", "model", 1735689600L, "ibm",
            "ibm/granite-3-3-8b-instruct", "granite-3-3-8b", "A general purpose instruct model", METADATA);
        var response = new ModelGatewayListModelsResponse("list", List.of(model));

        var json = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, ModelGatewayListModelsResponse.class);
        assertEquals(response, roundTripped);
    }

    @Test
    void should_omit_the_metadata_property_when_absent() {

        var model = new ModelGatewayModel(
            "22222222-2222-2222-2222-222222222222", "model", 1735689600L, "ibm",
            "ibm/granite-3-3-8b-instruct", null, null, null);

        var json = Json.toJson(model);
        assertFalse(json.contains("metadata"));

        var roundTripped = Json.fromJson(json, ModelGatewayModel.class);
        assertEquals(model, roundTripped);
    }
}
