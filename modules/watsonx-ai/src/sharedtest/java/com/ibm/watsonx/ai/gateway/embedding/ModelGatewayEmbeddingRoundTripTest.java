/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingResponse.Embedding;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingResponse.Usage;

@DisabledInNativeImage
public class ModelGatewayEmbeddingRoundTripTest {

    @Test
    void should_serialize_the_base64_embedding_in_the_shape_the_gateway_sent_it() {

        var base64 = encodeFloats(0.1f, 0.2f, 0.3f);
        var EXPECTED = """
            { "object": "embedding", "index": 0, "embedding": "%s" }""".formatted(base64);

        var embedding = Embedding.of("embedding", 0, base64);

        var json = Json.toJson(embedding);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertTrue(json.contains(base64));
        assertFalse(json.contains("0.1"));

        var roundTripped = Json.fromJson(json, Embedding.class);
        assertEquals(embedding, roundTripped);
    }

    @Test
    void should_serialize_the_float_embedding_in_the_shape_the_gateway_sent_it() {

        var EXPECTED = """
            { "object": "embedding", "index": 1, "embedding": [0.1, 0.2, 0.3] }""";

        var embedding = Embedding.of("embedding", 1, List.of(0.1, 0.2, 0.3));

        var json = Json.toJson(embedding);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertFalse(json.contains("\"base64\""));

        var roundTripped = Json.fromJson(json, Embedding.class);
        assertEquals(embedding, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_embedding_usage() {

        var EXPECTED = """
            { "prompt_tokens": 12, "total_tokens": 12 }""";

        var usage = new Usage(12, 12);

        var json = Json.toJson(usage);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, Usage.class);
        assertEquals(usage, roundTripped);
    }

    @Test
    void should_serialize_and_deserialize_the_embedding_response() {

        var base64 = encodeFloats(0.1f, 0.2f, 0.3f);
        var EXPECTED = """
            {
                "object": "list",
                "model": "ibm/slate-125m-english-rtrvr",
                "data": [ { "object": "embedding", "index": 0, "embedding": "%s" } ],
                "usage": { "prompt_tokens": 5, "total_tokens": 5 }
            }""".formatted(base64);

        var response = new ModelGatewayEmbeddingResponse(
            "list", "ibm/slate-125m-english-rtrvr", List.of(Embedding.of("embedding", 0, base64)), new Usage(5, 5));

        var json = Json.toJson(response);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTripped = Json.fromJson(json, ModelGatewayEmbeddingResponse.class);
        assertEquals(response, roundTripped);
    }

    private static String encodeFloats(float... values) {
        var buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (var value : values)
            buffer.putFloat(value);
        return Base64.getEncoder().encodeToString(buffer.array());
    }
}
