/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;

public class SseEventProcessorTest {

    @Test
    void should_propagate_moderations_and_detections_from_streaming_chunks() {

        var processor = new SseEventProcessor(List.of(), null);

        var firstChunk = "data: "
            + """
                {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Sure, your phone numbers are 3572865321."}}],\
                "created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.380Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":29,"end":39},"entity":"PhoneNumber","word":"3572865321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"3572865321","start":29,"end":39}]}]}}""";

        var stopChunk = "data: " + """
            {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant"}}],\
            "created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.381Z"}""";

        var usageChunk = "data: " + """
            {"id":"chatcmpl-1","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small",\
            "choices":[],"created":1785060893,"model_version":"4.0.0","created_at":"2026-07-26T10:14:53.381Z",\
            "usage":{"completion_tokens":19,"prompt_tokens":49,"total_tokens":68}}""";

        processor.processChunk(firstChunk);
        processor.processChunk(stopChunk);
        processor.processChunk(usageChunk);

        var response = processor.buildResponse();

        assertNotNull(response.moderations());
        assertEquals(1, response.moderations().get("pii").size());
        var mod = response.moderations().get("pii").get(0);
        assertEquals(0.8f, mod.score());
        assertEquals("PhoneNumber", mod.entity());
        assertEquals(29, mod.position().start());
        assertEquals(39, mod.position().end());

        assertNotNull(response.detections());
        assertEquals(1, response.detections().get("output").size());
        var det = response.detections().get("output").get(0);
        assertEquals(0, det.choiceIndex());
        assertEquals("en_syntax_rbr_pii", det.results().get(0).detectorId());
        assertEquals("PhoneNumber", det.results().get(0).detection());

        assertNotNull(response.usage());
        assertEquals(68, response.usage().totalTokens());
        assertEquals("stop", response.choices().get(0).finishReason());
    }

    @Test
    void should_aggregate_moderations_and_detections_across_multiple_chunks() {

        var processor = new SseEventProcessor(List.of(), null);

        // Chunk 1: empty moderations, empty output detections (the shape mistral emits before any match).
        var chunk1 = "data: " + """
            {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Here are the phone numbers you provided:"}}],\
            "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.000Z",\
            "moderations":{},"detections":{"output":[{"choice_index":0,"results":[]}]}}""";

        // Chunk 2: first PII match.
        var chunk2 = "data: "
            + """
                {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"\\n1. 357-286-5321"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.001Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":7,"end":19},"entity":"PhoneNumber","word":"357-286-5321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"357-286-5321","start":7,"end":19}]}]}}""";

        // Chunk 3: second PII match, finish_reason=stop.
        var chunk3 = "data: "
            + """
                {"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":"stop","delta":{"role":"assistant","content":"\\n2. 213-234-8765"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.002Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":6,"end":18},"entity":"PhoneNumber","word":"213-234-8765"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"213-234-8765","start":6,"end":18}]}]}}""";

        processor.processChunk(chunk1);
        processor.processChunk(chunk2);
        processor.processChunk(chunk3);

        var response = processor.buildResponse();

        var piiMatches = response.moderations().get("pii");
        assertNotNull(piiMatches);
        assertEquals(2, piiMatches.size(), "moderations from all chunks should be aggregated");
        assertEquals("357-286-5321", piiMatches.get(0).word());
        assertEquals("213-234-8765", piiMatches.get(1).word());

        var outputEntries = response.detections().get("output");
        assertNotNull(outputEntries);
        assertEquals(1, outputEntries.size(), "detections grouped by choice_index should collapse to one entry");
        var results = outputEntries.get(0).results();
        assertEquals(2, results.size(), "detection results from all chunks should be aggregated for the same choice_index");
        assertTrue(results.stream().anyMatch(r -> "357-286-5321".equals(r.text())));
        assertTrue(results.stream().anyMatch(r -> "213-234-8765".equals(r.text())));
    }

    @Test
    void should_expose_per_chunk_moderations_on_partial_response_event() {

        var processor = new SseEventProcessor(List.of(), null);

        // Chunk 1: content without any PII match, no moderations flagged.
        var chunk1 = "data: " + """
            {"id":"chatcmpl-3","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
            "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"Here are the phone numbers you provided:"}}],\
            "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.000Z",\
            "moderations":{},"detections":{"output":[{"choice_index":0,"results":[]}]}}""";

        // Chunk 2: content contains a PII match, moderations populated.
        var chunk2 = "data: "
            + """
                {"id":"chatcmpl-3","object":"chat.completion.chunk","model_id":"mistral","model":"mistral",\
                "choices":[{"index":0,"finish_reason":null,"delta":{"role":"assistant","content":"\\n1. 357-286-5321"}}],\
                "created":1,"model_version":"1.0","created_at":"2026-07-26T00:00:00.001Z",\
                "moderations":{"pii":[{"score":0.8,"input":false,"position":{"start":7,"end":19},"entity":"PhoneNumber","word":"357-286-5321"}]},\
                "detections":{"output":[{"choice_index":0,"results":[{"detector_id":"en_syntax_rbr_pii","detection_type":"pii","detection":"PhoneNumber","score":0.8,"text":"357-286-5321","start":7,"end":19}]}]}}""";

        var events1 = processor.processChunk(chunk1);
        var events2 = processor.processChunk(chunk2);

        var partial1 = events1.events().stream()
            .filter(PartialResponseEvent.class::isInstance)
            .map(PartialResponseEvent.class::cast)
            .findFirst()
            .orElseThrow();

        // Chunk 1 has no PII match: moderations is empty, detections.output[0].results is empty.
        assertNotNull(partial1.chunk().moderations());
        assertTrue(partial1.chunk().moderations().isEmpty());
        assertNotNull(partial1.chunk().detections());
        assertTrue(partial1.chunk().detections().get("output").get(0).results().isEmpty());

        var partial2 = events2.events().stream()
            .filter(PartialResponseEvent.class::isInstance)
            .map(PartialResponseEvent.class::cast)
            .findFirst()
            .orElseThrow();

        // Chunk 2 carries the PII match: a handler can react (block, tag, log) at this precise moment.
        var chunkPii = partial2.chunk().moderations().get("pii");
        assertNotNull(chunkPii);
        assertEquals(1, chunkPii.size());
        assertEquals("PhoneNumber", chunkPii.get(0).entity());
        assertEquals("357-286-5321", chunkPii.get(0).word());
        assertEquals(7, chunkPii.get(0).position().start());
        assertEquals(19, chunkPii.get(0).position().end());
    }
}
