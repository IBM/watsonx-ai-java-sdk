/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.jackson3.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;

public class JsonProviderDiscoveryTest {

    @Test
    void service_loader_should_discover_exactly_one_jackson3_provider() {
        var providers = ServiceLoader.load(JsonProvider.class).stream().map(ServiceLoader.Provider::get).toList();

        assertEquals(1, providers.size());

        var provider = providers.get(0);
        assertInstanceOf(JacksonProvider.class, provider);
        assertTrue(provider.isDefault());

        var original = AssistantMessage.tools(List.of(ToolCall.of("1", "getWeather", "{\"city\":\"Rome\"}")));
        var json = provider.toJson(original);
        var roundTripped = provider.fromJson(json, AssistantMessage.class);

        assertEquals(original, roundTripped);
    }
}
