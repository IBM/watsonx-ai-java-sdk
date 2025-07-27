/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.utils.Utils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class ToolServiceTest {

    @Mock
    HttpClient mockHttpClient;

    @Mock
    HttpResponse<String> mockHttpResponse;

    @Captor
    ArgumentCaptor<HttpRequest> httpRequest;

    @Mock
    AuthenticationProvider mockAuthenticationProvider;

    @BeforeEach
    void setup() {
        when(mockAuthenticationProvider.getToken()).thenReturn("token");
    }

    @Test
    void test_get_all() throws Exception {

        var response = """
            {
                "resources": [
                    {
                        "name": "GoogleSearch",
                        "description": "Search for online trends, news, current events, real-time information, or research topics.",
                        "agent_description": "Search for online trends, news, current events, real-time information, or research topics.",
                        "config_schema": {
                            "title": "config schema for GoogleSearch tool",
                            "type": "object",
                            "properties": {
                                "maxResults": {
                                    "title": "Max number of results to return",
                                    "type": "integer",
                                    "minimum": 1,
                                    "maximum": 20,
                                    "wx_ui_name": "Max results",
                                    "wx_ui_field_type": "numberInput",
                                    "wx_ui_default": 10
                                }
                            }
                        }
                    }]
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(response);
        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .logRequests(true)
            .logResponses(true)
            .timeout(Duration.ofSeconds(10))
            .build();

        var utilityTools = toolService.getAll();

        JSONAssert.assertEquals(response, toJson(utilityTools), true);
        assertEquals(10000, httpRequest.getValue().timeout().orElseThrow().toMillis());
        assertEquals(
            URI.create(CloudRegion.DALLAS.getWxEndpoint().concat("/v1-beta/utility_agent_tools")),
            httpRequest.getValue().uri()
        );
    }

    @Test
    void test_get_by_name() throws Exception {

        var response = """
            {
              "name": "GoogleSearch",
              "description": "Search for online trends, news, current events, real-time information, or research topics.",
              "agent_description": "Search for online trends, news, current events, real-time information, or research topics.",
              "config_schema": {
                "title": "config schema for GoogleSearch tool",
                "type": "object",
                "properties": {
                  "maxResults": {
                    "title": "Max number of results to return",
                    "type": "integer",
                    "minimum": 1,
                    "maximum": 20,
                    "wx_ui_name": "Max results",
                    "wx_ui_field_type": "numberInput",
                    "wx_ui_default": 10
                  }
                }
              }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(response);
        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var utilityTools = toolService.getByName("GoogleSearch");
        JSONAssert.assertEquals(response, toJson(utilityTools), true);
        assertEquals(
            URI.create(CloudRegion.DALLAS.getWxEndpoint().concat("/v1-beta/utility_agent_tools/GoogleSearch")),
            httpRequest.getValue().uri()
        );
    }

    @Test
    void test_run() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "[{\\"title\\":\\"title\\",\\"description\\":\\"description\\",\\"url\\":\\"https://url.com\\"},{\\"title\\":\\"title2\\",\\"description\\":\\"description2\\",\\"url\\":\\"https://url2.com\\"}]"
                }""");

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.DALLAS)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var body = ToolRequest.unstructuredInput("GoogleSearch", "test");
        var result = toolService.run(body);
        assertEquals(2, fromJson(result, List.class).size());
        assertEquals(
            URI.create(CloudRegion.DALLAS.getWxEndpoint().concat("/v1-beta/utility_agent_tools/run")),
            httpRequest.getValue().uri()
        );
        JSONAssert.assertEquals(toJson(body), bodyPublisherToString(httpRequest), true);

        body = ToolRequest.unstructuredInput("GoogleSearch", "test", Map.of("test", "test"));
        toolService.run(body);
        JSONAssert.assertEquals(toJson(body), bodyPublisherToString(httpRequest), true);

        body = ToolRequest.structuredInput("GoogleSearch", Map.of("input", "input"));
        toolService.run(body);
        JSONAssert.assertEquals(toJson(body), bodyPublisherToString(httpRequest), true);

        body = ToolRequest.structuredInput("GoogleSearch", Map.of("input", "input"), Map.of("test", "test"));
        toolService.run(body);
        JSONAssert.assertEquals(toJson(body), bodyPublisherToString(httpRequest), true);
    }

    @Test
    void test_exceptions() throws Exception {

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class)))
            .thenThrow(new IOException())
            .thenThrow(new InterruptedException());

        var toolService = ToolService.builder()
            .url(CloudRegion.FRANKFURT)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        assertThrows(RuntimeException.class, () -> toolService.getAll());
        assertThrows(RuntimeException.class, () -> toolService.getAll());
        assertThrows(RuntimeException.class, () -> toolService.getByName("test"));
        assertThrows(RuntimeException.class, () -> toolService.getByName("test"));
        assertThrows(RuntimeException.class, () -> toolService.run(ToolRequest.unstructuredInput("test", "test")));
        assertThrows(RuntimeException.class, () -> toolService.run(ToolRequest.unstructuredInput("test", "test")));
    }

    @Test
    void test_google_search_tool() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "[{\\"title\\":\\"title\\",\\"description\\":\\"description\\",\\"url\\":\\"https://url.com\\"},{\\"title\\":\\"title2\\",\\"description\\":\\"description2\\",\\"url\\":\\"https://url2.com\\"}]"
                }""");

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.LONDON)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var googleSearch = new GoogleSearchTool(toolService);

        var result = googleSearch.search("test");
        assertEquals(2, result.size());
        assertEquals("title", result.get(0).title());
        assertEquals("description", result.get(0).description());
        assertEquals("https://url.com", result.get(0).url());
        assertEquals("title2", result.get(1).title());
        assertEquals("description2", result.get(1).description());
        assertEquals("https://url2.com", result.get(1).url());
        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "GoogleSearch",
                Map.of("q", "test"),
                Map.of("maxResults", 10)
            )),
            bodyPublisherToString(httpRequest),
            true
        );

        googleSearch.search("test", 5);
        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "GoogleSearch",
                Map.of("q", "test"),
                Map.of("maxResults", 5)
            )),
            bodyPublisherToString(httpRequest),
            true
        );
    }

    @Test
    void test_web_crawler_tool() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "\\"{\\\\\\"url\\\\\\":\\\\\\"https://github.com/IBM/watsonx-ai-java-sdk\\\\\\",\\\\\\"contentType\\\\\\":\\\\\\"text/html; charset=utf-8\\\\\\",\\\\\\"content\\\\\\":\\\\\\"GitHub - IBM/watsonx-ai-java-sdk: IBM watsonx.ai Java SDK\\\\\\"}\\""
                }
                """);

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.MUMBAI)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var webcrawler = new WebCrawlerTool(toolService);

        var result = webcrawler.process("https://github.com/IBM/watsonx-ai-java-sdk");
        assertTrue(result.startsWith("GitHub - IBM/watsonx-ai-java-sdk:"));

        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "WebCrawler",
                Map.of("url", "https://github.com/IBM/watsonx-ai-java-sdk"),
                null
            )),
            bodyPublisherToString(httpRequest),
            true
        );
    }

    @Test
    void test_weather_tool() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "Current weather in naples, italy:\\nTemperature: 26.9°C\\nRain: 0mm\\nRelative humidity: 47%\\nWind: 10.8km/h\\n"
                }
                """);

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.SYDNEY)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var weather = new WeatherTool(toolService);

        var result = weather.find("naples");
        assertTrue(result.startsWith("Current weather in naples, italy"));

        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "Weather",
                Map.of("location", "naples")
            )),
            bodyPublisherToString(httpRequest),
            true
        );

        weather.find("naples", "italy");

        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "Weather",
                Map.of("location", "naples", "country", "italy")
            )),
            bodyPublisherToString(httpRequest),
            true
        );
    }

    @Test
    void test_wikipedia_tool() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "Page: PC\\nSummary: PC or pc may refer to:\\n\\n\\n== Arts and entertainment"
                }
                """);

        when(mockHttpClient.send(httpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        var toolService = ToolService.builder()
            .url(CloudRegion.TOKYO)
            .authenticationProvider(mockAuthenticationProvider)
            .httpClient(mockHttpClient)
            .build();

        var wikipedia = new WikipediaTool(toolService);

        var result = wikipedia.search("pc");
        assertTrue(result.startsWith("Page: PC"));

        JSONAssert.assertEquals(
            toJson(ToolRequest.structuredInput(
                "Wikipedia",
                Map.of("query", "pc")
            )),
            bodyPublisherToString(httpRequest),
            true
        );
    }
}
