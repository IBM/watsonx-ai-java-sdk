/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageResponse.ImageData;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageResponse.InputTokensDetails;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageResponse.Usage;

public class ModelGatewayImageRoundTripTest {

    @Test
    void should_serialize_and_deserialize_the_image_data() {

        var EXPECTED = """
            {
                "url": "https://example.com/image.png",
                "b64_json": "aGVsbG8=",
                "revised_prompt": "A cat wearing a hat, studio lighting"
            }""";

        var imageData = new ImageData("https://example.com/image.png", "aGVsbG8=", "A cat wearing a hat, studio lighting");

        var json = Json.toJson(imageData);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertTrue(json.contains("\"b64_json\""));
        assertFalse(json.contains("\"b64Json\""));

        var roundTripped = Json.fromJson(json, ImageData.class);
        assertEquals(imageData, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_input_tokens_details() {

        var EXPECTED = """
            { "image_tokens": 120, "text_tokens": 8 }""";

        var details = new InputTokensDetails(120, 8);

        var json = Json.toJson(details);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, InputTokensDetails.class);
        assertEquals(details, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_image_usage() {

        var EXPECTED = """
            {
                "input_tokens": 128,
                "output_tokens": 1024,
                "total_tokens": 1152,
                "input_tokens_details": { "image_tokens": 120, "text_tokens": 8 }
            }""";

        var usage = new Usage(128, 1024, 1152, new InputTokensDetails(120, 8));

        var json = Json.toJson(usage);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, Usage.class);
        assertEquals(usage, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_image_response() {

        var EXPECTED = """
            {
                "created": 1735689600,
                "data": [
                    { "url": "https://example.com/image.png", "b64_json": "aGVsbG8=", "revised_prompt": "A cat wearing a hat" }
                ],
                "background": "opaque",
                "output_format": "png",
                "quality": "high",
                "size": "1024x1024",
                "usage": {
                    "input_tokens": 128,
                    "output_tokens": 1024,
                    "total_tokens": 1152,
                    "input_tokens_details": { "image_tokens": 120, "text_tokens": 8 }
                }
            }""";

        var response = new ModelGatewayImageResponse(
            1735689600L,
            List.of(new ImageData("https://example.com/image.png", "aGVsbG8=", "A cat wearing a hat")),
            "opaque", "png", "high", "1024x1024",
            new Usage(128, 1024, 1152, new InputTokensDetails(120, 8)));

        var json = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertTrue(json.contains("\"output_format\""));
        assertFalse(json.contains("\"outputFormat\""));

        var roundTripped = Json.fromJson(json, ModelGatewayImageResponse.class);
        assertEquals(response, roundTripped);
    }
}
