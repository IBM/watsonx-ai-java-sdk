/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;

@DisabledInNativeImage
public class FoundationModelRoundTripTest {

    @Test
    void should_serialize_default_value_with_default_json_key() {

        var EXPECTED = """
            {
                "default": "bfloat16"
            }""";

        var defaultValue = new FoundationModel.DefaultValue("bfloat16");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(defaultValue), true);

        var deserialized = Json.fromJson(Json.toJson(defaultValue), FoundationModel.DefaultValue.class);
        assertEquals(defaultValue, deserialized);
    }

    @Test
    void should_serialize_num_gpus_with_default_json_key() {

        var EXPECTED = """
            {
                "default": 2
            }""";

        var numGpus = new FoundationModel.NumGpus(2);

        JSONAssert.assertEquals(EXPECTED, Json.toJson(numGpus), true);

        var deserialized = Json.fromJson(Json.toJson(numGpus), FoundationModel.NumGpus.class);
        assertEquals(numGpus, deserialized);
    }

    @Test
    void should_serialize_gradient_checkpointing_with_default_json_key() {

        var EXPECTED = """
            {
                "default": true
            }""";

        var gradientCheckpointing = new FoundationModel.GradientCheckpointing(true);

        JSONAssert.assertEquals(EXPECTED, Json.toJson(gradientCheckpointing), true);

        var deserialized = Json.fromJson(Json.toJson(gradientCheckpointing), FoundationModel.GradientCheckpointing.class);
        assertEquals(gradientCheckpointing, deserialized);
    }

    @Test
    void should_serialize_target_modules_with_default_json_key_holding_a_list() {

        var EXPECTED = """
            {
                "default": ["q_proj", "v_proj"]
            }""";

        var targetModules = new FoundationModel.TargetModules(List.of("q_proj", "v_proj"));

        JSONAssert.assertEquals(EXPECTED, Json.toJson(targetModules), true);

        var deserialized = Json.fromJson(Json.toJson(targetModules), FoundationModel.TargetModules.class);
        assertEquals(targetModules, deserialized);
    }

    @Test
    void should_serialize_init_method_with_supported_list_and_default_json_key() {

        var EXPECTED = """
            {
                "supported": ["random", "text"],
                "default": "random"
            }""";

        var initMethod = new FoundationModel.InitMethod(List.of("random", "text"), "random");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(initMethod), true);

        var deserialized = Json.fromJson(Json.toJson(initMethod), FoundationModel.InitMethod.class);
        assertEquals(initMethod, deserialized);
    }

    @Test
    void should_serialize_num_virtual_tokens_with_supported_list_and_default_json_key() {

        var EXPECTED = """
            {
                "supported": [20, 50, 100],
                "default": 100
            }""";

        var numVirtualTokens = new FoundationModel.NumVirtualTokens(List.of(20, 50, 100), 100);

        JSONAssert.assertEquals(EXPECTED, Json.toJson(numVirtualTokens), true);

        var deserialized = Json.fromJson(Json.toJson(numVirtualTokens), FoundationModel.NumVirtualTokens.class);
        assertEquals(numVirtualTokens, deserialized);
    }

    @Test
    void should_serialize_int_range_with_default_min_and_max_json_keys() {

        var EXPECTED = """
            {
                "default": 20,
                "min": 1,
                "max": 50
            }""";

        var intRange = new FoundationModel.IntRange(20, 1, 50);

        JSONAssert.assertEquals(EXPECTED, Json.toJson(intRange), true);

        var deserialized = Json.fromJson(Json.toJson(intRange), FoundationModel.IntRange.class);
        assertEquals(intRange, deserialized);
    }

    @Test
    void should_serialize_double_range_with_default_min_and_max_json_keys() {

        var EXPECTED = """
            {
                "default": 0.3,
                "min": 1.0E-5,
                "max": 0.5
            }""";

        var doubleRange = new FoundationModel.DoubleRange(0.3, 1.0E-5, 0.5);

        JSONAssert.assertEquals(EXPECTED, Json.toJson(doubleRange), true);

        var deserialized = Json.fromJson(Json.toJson(doubleRange), FoundationModel.DoubleRange.class);
        assertEquals(doubleRange, deserialized);
    }

    @Test
    void should_serialize_deployment_parameter_with_default_json_key_among_other_fields() {

        var EXPECTED = """
            {
                "name": "enable_lora",
                "display_name": "Enable Lora",
                "default": false,
                "type": "boolean"
            }""";

        var deploymentParameter = new FoundationModel.DeploymentParameter("enable_lora", "Enable Lora", false, "boolean");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(deploymentParameter), true);

        var deserialized = Json.fromJson(Json.toJson(deploymentParameter), FoundationModel.DeploymentParameter.class);
        assertEquals(deploymentParameter, deserialized);
    }
}
