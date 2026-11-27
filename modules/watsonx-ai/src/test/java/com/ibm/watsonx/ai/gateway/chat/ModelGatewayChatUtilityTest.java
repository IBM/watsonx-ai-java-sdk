/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ModelGatewayChatUtilityTest {

    private static final String MODEL_ID = "openai/gpt-4o";
    private static final String UNSUPPORTED_TIME_LIMIT = "timeLimit is not supported by Model Gateway. "
        + "Configure the HTTP timeout with ModelGatewayChatService.Builder.timeout(Duration).";

    private static ModelGatewayChatRequest requestWith(ModelGatewayChatParameters parameters) {
        return ModelGatewayChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .parameters(parameters)
            .build();
    }

    @Test
    void should_resolve_gateway_parameters() {
        var parameters = ModelGatewayChatParameters.builder()
            .temperature(0.7)
            .reasoningEffort(ModelGatewayChatParameters.ReasoningEffort.LOW)
            .store(true)
            .build();
        var request = requestWith(parameters);

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, null, MODEL_ID);

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
        assertEquals(0.7, result.temperature());
        assertEquals("low", result.reasoningEffort());
        assertTrue(result.store());
    }

    @Test
    void should_fall_back_to_service_defaults() {
        var defaults = ModelGatewayChatParameters.builder()
            .temperature(0.3)
            .serviceTier(ModelGatewayChatParameters.ServiceTier.PRIORITY)
            .build();
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, defaults, MODEL_ID);

        assertNotNull(result);
        assertEquals(0.3, result.temperature());
        assertEquals("priority", result.serviceTier());
    }

    @Test
    void should_prefer_request_over_defaults() {
        var defaults = ModelGatewayChatParameters.builder().temperature(0.3).build();
        var request = requestWith(ModelGatewayChatParameters.builder().temperature(0.9).build());

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, defaults, MODEL_ID);

        assertEquals(0.9, result.temperature());
    }

    @Test
    void should_use_fallback_model() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, null, MODEL_ID);

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void should_reject_request_or_default_time_limit_for_sync_and_streaming_requests(boolean streaming, boolean requestLimit) {
        var unsupported = ModelGatewayChatParameters.builder().timeLimit(Duration.ofSeconds(1)).build();
        var supported = ModelGatewayChatParameters.builder().temperature(0.5).build();
        var request = requestWith(requestLimit ? unsupported : supported);
        var defaults = requestLimit ? supported : unsupported;

        var error = assertThrows(IllegalArgumentException.class,
            () -> ModelGatewayChatUtility.buildGatewayRequest(request, defaults, MODEL_ID, streaming));

        assertEquals(UNSUPPORTED_TIME_LIMIT, error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_accept_explicitly_null_time_limit(boolean streaming) {
        var parameters = ModelGatewayChatParameters.builder().timeLimit(null).temperature(0.5).build();
        var request = requestWith(parameters);

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, parameters, MODEL_ID, streaming);

        assertEquals(0.5, result.temperature());
    }

    @Test
    void should_enable_usage_reporting_on_streaming() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, null, MODEL_ID, true);

        assertTrue(result.stream());
        assertNotNull(result.streamOptions());
        assertTrue(result.streamOptions().includeUsage());
    }

    @Test
    void should_not_set_stream_on_non_streaming() {
        var request = ModelGatewayChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = ModelGatewayChatUtility.buildGatewayRequest(request, null, MODEL_ID);

        assertNull(result.stream());
        assertNull(result.streamOptions());
    }

    @Test
    void should_replace_default_json_schema_with_request_response_format() {
        var defaults = ModelGatewayChatParameters.builder()
            .responseAsJsonSchema("default", Map.of("type", "object"), true)
            .build();

        for (var parameters : List.of(ModelGatewayChatParameters.builder().responseAsText().build(),
            ModelGatewayChatParameters.builder().responseAsJson().build())) {
            var result = ModelGatewayChatUtility.buildGatewayRequest(requestWith(parameters), defaults, MODEL_ID);

            assertEquals(Map.of("type", parameters.responseFormat()), result.responseFormat());
        }
    }

    @Test
    void should_keep_default_json_schema_when_request_does_not_set_format() {
        var defaults = ModelGatewayChatParameters.builder()
            .responseAsJsonSchema("default", Map.of("type", "object"), true)
            .build();

        var result = ModelGatewayChatUtility.buildGatewayRequest(
            requestWith(ModelGatewayChatParameters.builder().temperature(0.5).build()), defaults, MODEL_ID);

        assertEquals("json_schema", result.responseFormat().get("type"));
        assertEquals(Map.of("name", "default", "schema", Map.of("type", "object"), "strict", true),
            result.responseFormat().get("json_schema"));
    }

    @Test
    void should_remove_schema_when_switching_response_format_on_copied_parameters() {
        var original = ModelGatewayChatParameters.builder()
            .responseAsJsonSchema("original", Map.of("type", "object"), true)
            .build();

        for (var parameters : List.of(original.toBuilder().responseAsText().build(), original.toBuilder().responseAsJson().build())) {
            assertNull(parameters.jsonSchema());
            var result = ModelGatewayChatUtility.buildGatewayRequest(requestWith(parameters), original, MODEL_ID);
            assertEquals(Map.of("type", parameters.responseFormat()), result.responseFormat());
        }
    }

    @Test
    void should_omit_stream_options_from_non_streaming_requests() {
        var defaults = ModelGatewayChatParameters.builder().streamOptions(new ModelGatewayChatParameters.StreamOptions(true)).build();
        var request = requestWith(ModelGatewayChatParameters.builder().streamOptions(new ModelGatewayChatParameters.StreamOptions(false)).build());

        assertNull(ModelGatewayChatUtility.buildGatewayRequest(request, defaults, MODEL_ID).streamOptions());
        assertNull(ModelGatewayChatUtility.buildGatewayRequest(requestWith(null), defaults, MODEL_ID).streamOptions());
        assertEquals(new ModelGatewayChatParameters.StreamOptions(false),
            ModelGatewayChatUtility.buildGatewayRequest(request, defaults, MODEL_ID, true).streamOptions());
    }
}
