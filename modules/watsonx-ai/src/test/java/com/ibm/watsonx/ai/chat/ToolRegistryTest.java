/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.PythonInterpreterTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

@ExtendWith(MockitoExtension.class)
public class ToolRegistryTest {

    @Test
    void should_throw_when_no_tools_registered() {

        var ex = assertThrows(IllegalArgumentException.class, () -> ToolRegistry.builder().build());
        assertEquals("At least one tool must be provided", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> ToolRegistry.builder().register(List.of()).build());
        assertEquals("At least one tool must be provided", ex.getMessage());
    }

    @Test
    void should_return_the_schema_registered() {

        var mockToolService = mock(ToolService.class);
        var googleSearchTool = new GoogleSearchTool(mockToolService);
        var pythonInterpreterTool = new PythonInterpreterTool(mockToolService, "deploymentId");
        var tavilySearchTool = new TavilySearchTool(mockToolService, "apiKey");
        var weatherTool = new WeatherTool(mockToolService);
        var webCrawlerTool = new WebCrawlerTool(mockToolService);
        var wikipediaTool = new WikipediaTool(mockToolService);


        var toolRegistry = ToolRegistry.builder()
            .register(googleSearchTool, pythonInterpreterTool, tavilySearchTool, weatherTool, webCrawlerTool, wikipediaTool)
            .build();

        assertEquals(6, toolRegistry.tools().size());
        assertEquals(1, toolRegistry.tools(webCrawlerTool.name()).size());
        assertEquals(6, toolRegistry.tools(Set.of()).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_dispatch_to_correct_utility_tool() {

        var mockToolService = mock(ToolService.class);
        var mockToolArguments = mock(ToolArguments.class);
        when(mockToolArguments.contains(anyString())).thenReturn(true);
        when(mockToolArguments.get(anyString())).thenReturn("element");
        when(mockToolService.run(any())).thenReturn("mock-result");

        var googleSearchTool = new GoogleSearchTool(mockToolService);
        var pythonInterpreterTool = new PythonInterpreterTool(mockToolService, "deploymentId");
        var tavilySearchTool = new TavilySearchTool(mockToolService, "apiKey");
        var weatherTool = new WeatherTool(mockToolService);
        var webCrawlerTool = new WebCrawlerTool(mockToolService);
        var wikipediaTool = new WikipediaTool(mockToolService);

        var toolRegistry = ToolRegistry.builder()
            .register(googleSearchTool, pythonInterpreterTool, tavilySearchTool, weatherTool, webCrawlerTool, wikipediaTool)
            .build();

        try (var mockedJson = mockStatic(Json.class)) {

            mockedJson.when(() -> Json.fromJson(anyString(), any(TypeToken.class))).thenReturn(List.of());
            mockedJson.when(() -> Json.prettyPrint(any())).thenReturn("[]");
            var googleResult = toolRegistry.execute("google_search", mockToolArguments);
            assertEquals("[]", googleResult);

            var pythonResult = toolRegistry.execute("python_interpreter", mockToolArguments);
            assertNotNull(pythonResult);

            var tavilyResult = toolRegistry.execute("tavily_search", mockToolArguments);
            assertNotNull(tavilyResult);

            var weatherResult = toolRegistry.execute("weather", mockToolArguments);
            assertNotNull(weatherResult);

            when(mockToolArguments.contains("country")).thenReturn(false);
            weatherResult = toolRegistry.execute("weather", mockToolArguments);
            assertNotNull(weatherResult);

            mockedJson.when(() -> Json.fromJson(anyString(), eq(String.class))).thenReturn("{\"content\": \"test\"}");
            mockedJson.when(() -> Json.fromJson(anyString(), eq(Map.class))).thenReturn(Map.of("content", "test"));
            var webCrawlerResult = toolRegistry.execute("webcrawler", mockToolArguments);
            assertNotNull(webCrawlerResult);

            var wikipediaResult = toolRegistry.execute("wikipedia", mockToolArguments);
            assertNotNull(wikipediaResult);

            verify(mockToolService, times(7)).run(any(ToolRequest.class));
        }
    }

    @Test
    void should_throw_when_args_missing_or_null() {

        var mockToolService = mock(ToolService.class);
        var mockToolArguments = mock(ToolArguments.class);
        when(mockToolArguments.contains(anyString())).thenReturn(false);

        var googleSearchTool = new GoogleSearchTool(mockToolService);
        var pythonInterpreterTool = new PythonInterpreterTool(mockToolService, "deploymentId");
        var tavilySearchTool = new TavilySearchTool(mockToolService, "apiKey");
        var weatherTool = new WeatherTool(mockToolService);
        var webCrawlerTool = new WebCrawlerTool(mockToolService);
        var wikipediaTool = new WikipediaTool(mockToolService);

        var toolRegistry = ToolRegistry.builder()
            .register(googleSearchTool, pythonInterpreterTool, tavilySearchTool, weatherTool, webCrawlerTool, wikipediaTool)
            .build();

        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("google_search", mockToolArguments));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("python_interpreter", mockToolArguments));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("tavily_search", mockToolArguments));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("weather", mockToolArguments));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("webcrawler", mockToolArguments));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("wikipedia", mockToolArguments));

        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("google_search", null));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("python_interpreter", null));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("tavily_search", null));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("weather", null));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("webcrawler", null));
        assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("wikipedia", null));
    }

    @Test
    void should_invoke_before_and_after_callbacks() {

        var mockToolService = mock(ToolService.class);
        var mockToolArguments = mock(ToolArguments.class);
        when(mockToolArguments.contains(anyString())).thenReturn(true);
        when(mockToolArguments.get(anyString())).thenReturn("element");
        when(mockToolService.run(any())).thenReturn("mock-result");
        var weatherTool = new WeatherTool(mockToolService);

        AtomicInteger counter = new AtomicInteger();
        var toolRegistry = ToolRegistry.builder()
            .beforeExecution((toolName, toolArgs) -> {
                assertEquals("weather", toolName);
                assertEquals("element", toolArgs.get("location"));
                counter.incrementAndGet();
            })
            .afterExecution((toolName, toolArgs, result) -> {
                assertEquals("weather", toolName);
                assertEquals("element", toolArgs.get("location"));
                assertEquals("mock-result", result);
                counter.incrementAndGet();
            })
            .register(weatherTool)
            .build();

        var weatherResult = toolRegistry.execute("weather", mockToolArguments);
        assertNotNull(weatherResult);
        assertEquals(2, counter.get());
    }

    @Test
    void should_throw_when_tool_not_found() {

        var mockToolArguments = mock(ToolArguments.class);
        var weatherTool = mock(WeatherTool.class);

        var toolRegistry = ToolRegistry.builder()
            .register(weatherTool)
            .build();

        var ex = assertThrows(IllegalArgumentException.class, () -> toolRegistry.execute("weather", mockToolArguments));
        assertEquals("Unknown tool: weather", ex.getMessage());
    }

    @Test
    void should_invoke_on_error_callback_when_tool_fails() {

        var mockToolArguments = mock(ToolArguments.class);
        when(mockToolArguments.get(anyString())).thenReturn("element");
        var weatherTool = mock(WeatherTool.class);
        when(weatherTool.name()).thenReturn("weather");
        when(weatherTool.execute(any())).thenThrow(new RuntimeException("ex"));

        AtomicInteger counter = new AtomicInteger();
        var toolRegistry = ToolRegistry.builder()
            .onError((toolName, toolArgs, error) -> {
                assertEquals("weather", toolName);
                assertEquals("element", toolArgs.get("location"));
                assertEquals("ex", error.getMessage());
                counter.incrementAndGet();
            })
            .register(weatherTool)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> toolRegistry.execute("weather", mockToolArguments));
        assertEquals("ex", ex.getMessage());
        assertEquals(1, counter.get());
    }
}
