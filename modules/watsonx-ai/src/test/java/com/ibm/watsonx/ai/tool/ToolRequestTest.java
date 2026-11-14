/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRequestTest {

    @Test
    void should_snapshot_nested_tool_inputs_configuration_and_schemas() {
        var values = new ArrayList<>(List.of("original"));
        var source = new LinkedHashMap<String, Object>();
        source.put("values", values);

        var structured = ToolRequest.structuredInput("Tool", source, source);
        var unstructured = ToolRequest.unstructuredInput("Tool", "input", source);
        var tool = new UtilityTool("Tool", "description", "agent", source, source);
        values.clear();
        source.clear();

        for (var snapshot : List.of(structured.input(), structured.config(), unstructured.config(), tool.inputSchema(), tool.configSchema())) {
            assertEquals(Map.of("values", List.of("original")), snapshot);
            var nested = (List<?>) ((Map<?, ?>) snapshot).get("values");
            assertThrows(UnsupportedOperationException.class, nested::clear);
        }
    }
}
