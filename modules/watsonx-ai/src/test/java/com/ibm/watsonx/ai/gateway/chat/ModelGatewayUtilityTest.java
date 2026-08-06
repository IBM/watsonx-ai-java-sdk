/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ModelGatewayUtilityTest {

    private static final String MODEL_ID = "openai/gpt-4o";
    private static final long TIME_LIMIT = 10_000L;

    private static ModelGatewayChatRequest requestWith(ModelGatewayParameters parameters) {
        return ModelGatewayChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .parameters(parameters)
            .build();
    }

    @Test
    void should_resolve_gateway_parameters() {
        var parameters = ModelGatewayParameters.builder()
            .temperature(0.7)
            .reasoningEffort(ModelGatewayParameters.ReasoningEffort.LOW)
            .store(true)
            .build();
        var request = requestWith(parameters);

        var result = ModelGatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT);

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
        assertEquals(0.7, result.temperature());
        assertEquals("low", result.reasoningEffort());
        assertTrue(result.store());
    }

    @Test
    void should_fall_back_to_service_defaults() {
        var defaults = ModelGatewayParameters.builder()
            .temperature(0.3)
            .serviceTier(ModelGatewayParameters.ServiceTier.PRIORITY)
            .build();
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayUtility.buildGatewayRequest(request, defaults, MODEL_ID, TIME_LIMIT);

        assertNotNull(result);
        assertEquals(0.3, result.temperature());
        assertEquals("priority", result.serviceTier());
    }

    @Test
    void should_prefer_request_over_defaults() {
        var defaults = ModelGatewayParameters.builder().temperature(0.3).build();
        var request = requestWith(ModelGatewayParameters.builder().temperature(0.9).build());

        var result = ModelGatewayUtility.buildGatewayRequest(request, defaults, MODEL_ID, TIME_LIMIT);

        assertEquals(0.9, result.temperature());
    }

    @Test
    void should_use_fallback_model_and_time_limit_when_unset() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT);

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
        assertEquals(TIME_LIMIT, result.timeLimit());
    }

    @Test
    void should_enable_usage_reporting_on_streaming() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT, true);

        assertTrue(result.stream());
        assertNotNull(result.streamOptions());
        assertTrue(result.streamOptions().includeUsage());
    }

    @Test
    void should_not_set_stream_on_non_streaming() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT);

        assertNull(result.stream());
        assertNull(result.streamOptions());
    }
}
