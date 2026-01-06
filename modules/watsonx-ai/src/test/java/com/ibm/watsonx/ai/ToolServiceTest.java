/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.tool.ToolParameters;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.PythonInterpreterTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class ToolServiceTest extends AbstractWatsonxTest {

    @BeforeEach
    void setup() {
        when(mockAuthenticator.token()).thenReturn("token");
    }

    @Test
    void should_fetch_all_tools() throws Exception {

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
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(10))
                .build();

            var utilityTools = toolService.getAll();

            JSONAssert.assertEquals(response, toJson(utilityTools), true);
            assertEquals(10000, mockHttpRequest.getValue().timeout().orElseThrow().toMillis());
            assertEquals(
                URI.create(CloudRegion.DALLAS.wxEndpoint().concat("/v1-beta/utility_agent_tools")),
                mockHttpRequest.getValue().uri()
            );

            utilityTools = toolService.getAll(ToolParameters.builder().transactionId("my-transaction-id").build());
            assertNotNull(utilityTools);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_fetch_tool_by_name() throws Exception {

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
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var utilityTools = toolService.getByName("GoogleSearch");
            JSONAssert.assertEquals(response, toJson(utilityTools), true);
            assertEquals(
                URI.create(CloudRegion.DALLAS.wxEndpoint().concat("/v1-beta/utility_agent_tools/GoogleSearch")),
                mockHttpRequest.getValue().uri()
            );

            utilityTools = toolService.getByName("GoogleSearch", ToolParameters.builder().transactionId("my-transaction-id").build());
            assertNotNull(utilityTools);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_run_tool_with_various_input_types() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "[{\\"title\\":\\"title\\",\\"description\\":\\"description\\",\\"url\\":\\"https://url.com\\"},{\\"title\\":\\"title2\\",\\"description\\":\\"description2\\",\\"url\\":\\"https://url2.com\\"}]"
                }""");

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var body = ToolRequest.unstructuredInput("GoogleSearch", "test");
            var result = toolService.run(body);
            assertEquals(2, fromJson(result, List.class).size());
            assertEquals(
                URI.create(CloudRegion.DALLAS.wxEndpoint().concat("/v1-beta/utility_agent_tools/run")),
                mockHttpRequest.getValue().uri()
            );
            JSONAssert.assertEquals(toJson(body), bodyPublisherToString(mockHttpRequest), true);

            result = toolService.run(body, ToolParameters.builder().transactionId("my-transaction-id").build());
            assertNotNull(result);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");

            body = ToolRequest.unstructuredInput("GoogleSearch", "test", Map.of("test", "test"));
            toolService.run(body);
            JSONAssert.assertEquals(toJson(body), bodyPublisherToString(mockHttpRequest), true);

            body = ToolRequest.structuredInput("GoogleSearch", Map.of("input", "input"));
            toolService.run(body);
            JSONAssert.assertEquals(toJson(body), bodyPublisherToString(mockHttpRequest), true);

            body = ToolRequest.structuredInput("GoogleSearch", Map.of("input", "input"), Map.of("test", "test"));
            toolService.run(body);
            JSONAssert.assertEquals(toJson(body), bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_throw_runtime_exception_on_http_errors() throws Exception {

        withWatsonxServiceMock(() -> {
            try {
                when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
                    .thenThrow(new IOException())
                    .thenThrow(new InterruptedException());

                var toolService = ToolService.builder()
                    .baseUrl(CloudRegion.FRANKFURT)
                    .authenticator(mockAuthenticator)
                    .build();

                assertThrows(RuntimeException.class, () -> toolService.getAll());
                assertThrows(RuntimeException.class, () -> toolService.getAll());
                assertThrows(RuntimeException.class, () -> toolService.getByName("test"));
                assertThrows(RuntimeException.class, () -> toolService.getByName("test"));
                assertThrows(RuntimeException.class, () -> toolService.run(ToolRequest.unstructuredInput("test", "test")));
                assertThrows(RuntimeException.class, () -> toolService.run(ToolRequest.unstructuredInput("test", "test")));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_return_expected_google_search_results() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "[{\\"title\\":\\"title\\",\\"description\\":\\"description\\",\\"url\\":\\"https://url.com\\"},{\\"title\\":\\"title2\\",\\"description\\":\\"description2\\",\\"url\\":\\"https://url2.com\\"}]"
                }""");

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
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
                bodyPublisherToString(mockHttpRequest),
                true
            );

            googleSearch.search("test", 5);
            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "GoogleSearch",
                    Map.of("q", "test"),
                    Map.of("maxResults", 5)
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }

    @Test
    void should_return_expected_tavily_search_results() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                    "output": "[{\\"url\\":\\"https://example.com/test1\\",\\"title\\":\\"Title 1\\",\\"content\\":\\"Test 1\\",\\"score\\":0.63,\\"raw_content\\":null},{\\"url\\":\\"https://example.com/test2\\",\\"title\\":\\"Title 2\\",\\"content\\":\\"Test 2\\",\\"score\\":0.55,\\"raw_content\\":null}]"
                }""");

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
                .build();

            var tavilySearch = new TavilySearchTool(toolService, "apiKey");

            var result = tavilySearch.search("test");
            assertEquals(2, result.size());
            assertEquals("Title 1", result.get(0).title());
            assertEquals("Test 1", result.get(0).content());
            assertEquals(0.63, result.get(0).score());
            assertEquals("https://example.com/test1", result.get(0).url());
            assertEquals("Title 2", result.get(1).title());
            assertEquals("Test 2", result.get(1).content());
            assertEquals(0.55, result.get(1).score());
            assertEquals("https://example.com/test2", result.get(1).url());
            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "TavilySearch",
                    Map.of("query", "test"),
                    Map.of(
                        "apiKey", "apiKey",
                        "maxResults", 10
                    )
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );

            tavilySearch.search("test", 5);
            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "TavilySearch",
                    Map.of("query", "test"),
                    Map.of(
                        "apiKey", "apiKey",
                        "maxResults", 5
                    )
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }

    @Test
    void should_return_expected_web_crawler_output() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "\\"{\\\\\\"url\\\\\\":\\\\\\"https://github.com/IBM/watsonx-ai-java-sdk\\\\\\",\\\\\\"contentType\\\\\\":\\\\\\"text/html; charset=utf-8\\\\\\",\\\\\\"content\\\\\\":\\\\\\"GitHub - IBM/watsonx-ai-java-sdk: IBM watsonx.ai Java SDK\\\\\\"}\\""
                }
                """);

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.MUMBAI)
                .authenticator(mockAuthenticator)
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
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }

    @Test
    void should_return_expected_web_waether_output() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "Current weather in naples, italy:\\nTemperature: 26.9°C\\nRain: 0mm\\nRelative humidity: 47%\\nWind: 10.8km/h\\n"
                }
                """);

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.SYDNEY)
                .authenticator(mockAuthenticator)
                .build();

            var weather = new WeatherTool(toolService);

            var result = weather.find("naples");
            assertTrue(result.startsWith("Current weather in naples, italy"));

            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "Weather",
                    Map.of("location", "naples")
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );

            weather.find("naples", "italy");

            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "Weather",
                    Map.of("location", "naples", "country", "italy")
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }

    @Test
    void should_return_expected_wikipedia_search_results() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "Page: PC\\nSummary: PC or pc may refer to:\\n\\n\\n== Arts and entertainment"
                }
                """);

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.TOKYO)
                .authenticator(mockAuthenticator)
                .build();

            var wikipedia = new WikipediaTool(toolService);

            var result = wikipedia.search("pc");
            assertTrue(result.startsWith("Page: PC"));

            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "Wikipedia",
                    Map.of("query", "pc")
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }

    @Test
    void should_execute_python_code_and_return_output() {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(
            """
                {
                  "output": "Hello World!"
                }""");

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var toolService = ToolService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .build();

            var python = new PythonInterpreterTool(toolService, "my-deployment-id");

            var result = python.execute("print(\"Hello World\")");
            assertEquals("Hello World!", result);

            JSONAssert.assertEquals(
                toJson(ToolRequest.structuredInput(
                    "PythonInterpreter",
                    Map.of("code", "print(\"Hello World\")"),
                    Map.of("deploymentId", "my-deployment-id")
                )),
                bodyPublisherToString(mockHttpRequest),
                true
            );
        });
    }
}
