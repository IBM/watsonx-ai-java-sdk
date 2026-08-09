/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Cache;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Prediction;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.ReasoningEffort;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Router;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.ServiceTier;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.StreamOptions;

@SuppressWarnings("deprecation")
public class ModelGatewayChatParametersTest {

    @Test
    void should_round_trip_all_fields_via_builder_and_getters() {

        var prediction = new Prediction("content", "the known text");
        var router = new Router(new Cache(true, null, 0.8));
        var streamOptions = new StreamOptions(true);

        var params = ModelGatewayChatParameters.builder()
            .modelId("gpt-4o")
            .transactionId("txn-1")
            .temperature(0.7)
            .topP(0.9)
            .maxCompletionTokens(128)
            .frequencyPenalty(0.1)
            .presencePenalty(0.2)
            .seed(42)
            .n(2)
            .logprobs(true)
            .topLogprobs(3)
            .stop(List.of("STOP"))
            .logitBias(Map.of("50256", -100))
            .timeLimit(Duration.ofSeconds(30))
            .toolChoice("get_weather")
            .responseAsJson()
            .audio(Map.of("voice", "alloy"))
            .metadata(Map.of("tenant", "acme"))
            .modalities(List.of("text", "audio"))
            .parallelToolCalls(true)
            .prediction(prediction)
            .reasoningEffort(ReasoningEffort.HIGH)
            .serviceTier(ServiceTier.PRIORITY)
            .store(true)
            .streamOptions(streamOptions)
            .router(router)
            .maxTokens(256)
            .user("user-1")
            .build();

        // Base parameters.
        assertEquals("gpt-4o", params.modelId());
        assertEquals("txn-1", params.transactionId());
        assertEquals(0.7, params.temperature());
        assertEquals(0.9, params.topP());
        assertEquals(128, params.maxCompletionTokens());
        assertEquals(0.1, params.frequencyPenalty());
        assertEquals(0.2, params.presencePenalty());
        assertEquals(42, params.seed());
        assertEquals(2, params.n());
        assertEquals(true, params.logprobs());
        assertEquals(3, params.topLogprobs());
        assertEquals(List.of("STOP"), params.stop());
        assertEquals(Map.of("50256", -100), params.logitBias());
        assertEquals(30_000L, params.timeLimit());
        assertEquals("get_weather", ((Map<?, ?>) params.toolChoice().get("function")).get("name"));

        // Gateway-only parameters.
        assertEquals(Map.of("voice", "alloy"), params.audio());
        assertEquals(Map.of("tenant", "acme"), params.metadata());
        assertEquals(List.of("text", "audio"), params.modalities());
        assertEquals(true, params.parallelToolCalls());
        assertEquals(prediction, params.prediction());
        assertEquals("high", params.reasoningEffort());
        assertEquals("priority", params.serviceTier());
        assertEquals(true, params.store());
        assertEquals(streamOptions, params.streamOptions());
        assertEquals(router, params.router());
        assertEquals(256, params.maxTokens());
        assertEquals("user-1", params.user());

        // toBuilder() must reproduce the same view.
        var copy = params.toBuilder().build();
        assertEquals(params.modelId(), copy.modelId());
        assertEquals(params.temperature(), copy.temperature());
        assertEquals(params.stop(), copy.stop());
        assertEquals(params.timeLimit(), copy.timeLimit());
        assertEquals(params.reasoningEffort(), copy.reasoningEffort());
        assertEquals(params.serviceTier(), copy.serviceTier());
        assertEquals(params.prediction(), copy.prediction());
        assertEquals(params.router(), copy.router());
        assertEquals(params.streamOptions(), copy.streamOptions());
        assertEquals(params.maxTokens(), copy.maxTokens());
        assertEquals(params.user(), copy.user());
        assertEquals("get_weather", ((Map<?, ?>) copy.toolChoice().get("function")).get("name"));
        assertEquals(params.responseFormat(), copy.responseFormat());
    }

    @Test
    void should_leave_unset_fields_null() {
        var params = ModelGatewayChatParameters.builder().build();
        assertNull(params.audio());
        assertNull(params.metadata());
        assertNull(params.modalities());
        assertNull(params.parallelToolCalls());
        assertNull(params.prediction());
        assertNull(params.reasoningEffort());
        assertNull(params.serviceTier());
        assertNull(params.store());
        assertNull(params.streamOptions());
        assertNull(params.router());
        assertNull(params.maxTokens());
        assertNull(params.user());
    }

    @Test
    void should_accept_string_and_null_enum_setters() {
        assertEquals("low", ModelGatewayChatParameters.builder().reasoningEffort("low").build().reasoningEffort());
        assertEquals("flex", ModelGatewayChatParameters.builder().serviceTier("flex").build().serviceTier());
        assertNull(ModelGatewayChatParameters.builder().reasoningEffort((ReasoningEffort) null).build().reasoningEffort());
        assertNull(ModelGatewayChatParameters.builder().serviceTier((ServiceTier) null).build().serviceTier());
    }

    @Test
    void should_resolve_reasoning_effort_from_wire_value() {
        assertEquals(ReasoningEffort.LOW, ReasoningEffort.fromValue("low"));
        assertEquals(ReasoningEffort.MEDIUM, ReasoningEffort.fromValue("medium"));
        assertEquals(ReasoningEffort.HIGH, ReasoningEffort.fromValue("high"));
        assertEquals("high", ReasoningEffort.HIGH.value());

        var ex = assertThrows(IllegalArgumentException.class, () -> ReasoningEffort.fromValue("extreme"));
        assertTrue(ex.getMessage().contains("extreme"));
    }

    @Test
    void should_expose_service_tier_wire_values() {
        assertEquals("auto", ServiceTier.AUTO.value());
        assertEquals("default", ServiceTier.DEFAULT.value());
        assertEquals("flex", ServiceTier.FLEX.value());
        assertEquals("priority", ServiceTier.PRIORITY.value());
    }
}
