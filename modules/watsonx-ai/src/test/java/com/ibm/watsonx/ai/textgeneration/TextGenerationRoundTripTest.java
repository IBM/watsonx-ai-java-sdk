/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.textgeneration.Moderation.InputRanges;
import com.ibm.watsonx.ai.textgeneration.Moderation.TextModeration;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters.ReturnOptions;

public class TextGenerationRoundTripTest {

    @Test
    void should_serialize_all_text_generation_parameters_properties() {

        var EXPECTED = """
            {
                "decoding_method": "sample",
                "length_penalty": {
                    "decay_factor": 1.5,
                    "start_index": 3
                },
                "max_new_tokens": 200,
                "min_new_tokens": 10,
                "random_seed": 42,
                "stop_sequences": ["\\n\\n", "END"],
                "temperature": 0.7,
                "time_limit": 30000,
                "top_k": 50,
                "top_p": 0.9,
                "repetition_penalty": 1.2,
                "truncate_input_tokens": 1024,
                "return_options": {
                    "input_text": true,
                    "generated_text": true
                },
                "include_stop_sequence": true,
                "prompt_variables": {
                    "name": "Alice"
                }
            }""";

        var params = TextGenerationParameters.builder()
            .decodingMethod("sample")
            .lengthPenalty(1.5, 3)
            .maxNewTokens(200)
            .minNewTokens(10)
            .randomSeed(42)
            .stopSequences(List.of("\n\n", "END"))
            .temperature(0.7)
            .timeLimit(Duration.ofSeconds(30))
            .topK(50)
            .topP(0.9)
            .repetitionPenalty(1.2)
            .truncateInputTokens(1024)
            .returnOptions(ReturnOptions.builder().inputText(true).generatedText(true).build())
            .includeStopSequence(true)
            .promptVariables(Map.of("name", "Alice"))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(params), true);
    }

    @Test
    void should_round_trip_text_generation_parameters_single_value_properties() {

        var EXPECTED = """
            {
                "decoding_method": "sample",
                "max_new_tokens": 200,
                "min_new_tokens": 10,
                "random_seed": 42,
                "stop_sequences": ["\\n\\n", "END"],
                "temperature": 0.7,
                "top_k": 50,
                "top_p": 0.9,
                "repetition_penalty": 1.2,
                "truncate_input_tokens": 1024,
                "include_stop_sequence": true,
                "prompt_variables": {
                    "name": "Alice"
                }
            }""";

        var params = TextGenerationParameters.builder()
            .decodingMethod("sample")
            .maxNewTokens(200)
            .minNewTokens(10)
            .randomSeed(42)
            .stopSequences(List.of("\n\n", "END"))
            .temperature(0.7)
            .topK(50)
            .topP(0.9)
            .repetitionPenalty(1.2)
            .truncateInputTokens(1024)
            .includeStopSequence(true)
            .promptVariables(Map.of("name", "Alice"))
            .build();

        var json = Json.toJson(params);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(params, Json.fromJson(json, TextGenerationParameters.class));
    }

    @Test
    void should_not_reconstruct_length_penalty_from_json() {

        var params = TextGenerationParameters.builder().decodingMethod("sample").lengthPenalty(1.5, 3).build();

        var back = Json.fromJson(Json.toJson(params), TextGenerationParameters.class);

        assertEquals("sample", back.decodingMethod());
        assertNull(back.lengthPenalty());
    }

    @Test
    void should_serialize_all_moderation_properties() {

        var EXPECTED = """
            {
                "hap": {
                    "input": { "enabled": true, "threshold": 0.8 },
                    "output": { "enabled": true, "threshold": 0.9 },
                    "mask": { "remove_entity_value": true }
                },
                "pii": {
                    "input": { "enabled": true },
                    "output": { "enabled": false },
                    "mask": { "remove_entity_value": false }
                },
                "granite_guardian": {
                    "input": { "enabled": true, "threshold": 0.85 },
                    "mask": { "remove_entity_value": true }
                },
                "input_ranges": [
                    { "start": 0, "end": 50 },
                    { "start": 100, "end": 150 }
                ]
            }""";

        var moderation = Moderation.builder()
            .hap(TextModeration.of(0.8f), TextModeration.of(0.9f), true)
            .pii(true, false, false)
            .graniteGuardian(TextModeration.of(0.85f), true)
            .inputRanges(List.of(InputRanges.of(0, 50), InputRanges.of(100, 150)))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(moderation), true);
    }

    @Test
    void should_round_trip_moderation_input_ranges() {

        var EXPECTED = """
            {
                "input_ranges": [
                    { "start": 0, "end": 50 },
                    { "start": 100, "end": 150 }
                ]
            }""";

        var moderation = Moderation.builder().inputRanges(List.of(InputRanges.of(0, 50), InputRanges.of(100, 150))).build();

        var json = Json.toJson(moderation);
        JSONAssert.assertEquals(EXPECTED, json, true);
        assertEquals(moderation, Json.fromJson(json, Moderation.class));
    }

    @Test
    void should_not_reconstruct_hap_pii_granite_guardian_from_json() {

        var moderation = Moderation.builder()
            .hap(TextModeration.of(0.8f), TextModeration.of(0.9f), true)
            .pii(true, false, false)
            .graniteGuardian(TextModeration.of(0.85f), true)
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();

        var back = Json.fromJson(Json.toJson(moderation), Moderation.class);

        assertNull(back.hap());
        assertNull(back.pii());
        assertNull(back.graniteGuardian());
        assertEquals(List.of(InputRanges.of(0, 50)), back.inputRanges());
    }
}
