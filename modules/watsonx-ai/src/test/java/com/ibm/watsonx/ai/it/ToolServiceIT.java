/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool.GoogleSearchResult;
import com.ibm.watsonx.ai.tool.builtin.PythonInterpreterTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool.TavilySearchResult;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_WX_URL", matches = ".+")
public class ToolServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String URL = System.getenv("WATSONX_WX_URL");

    static final AuthenticationProvider authentication = IAMAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final ToolService toolService = ToolService.builder()
        .baseUrl(CloudRegion.FRANKFURT)
        .authenticationProvider(authentication)
        .logRequests(true)
        .logResponses(true)
        .timeout(Duration.ofSeconds(30))
        .build();

    @Test
    void should_return_all_tools_with_valid_metadata() {
        var response = toolService.getAll();
        assertNotNull(response);
        assertNotNull(response.resources());
        assertTrue(response.resources().size() > 0);
        assertNotNull(response.resources().get(1).name());
        assertNotNull(response.resources().get(1).agentDescription());
        assertNotNull(response.resources().get(1).configSchema());
        assertNotNull(response.resources().get(1).description());
        assertNotNull(response.resources().get(1).inputSchema());
    }

    @Test
    void should_return_tool_by_name_with_valid_metadata() {
        var response = toolService.getByName("GoogleSearch");
        assertNotNull(response);
        assertNotNull(response.name());
        assertNotNull(response.agentDescription());
        assertNotNull(response.configSchema());
        assertNotNull(response.description());
        assertNotNull(response.inputSchema());
    }

    @Test
    void should_execute_tool_and_return_non_blank_result() {
        ToolRequest request = ToolRequest.structuredInput("WebCrawler", Map.of("url", "https://github.com/IBM/watsonx-ai-java-sdk"));
        var result = toolService.run(request);
        assertNotNull(toolService.run(request));
        assertFalse(result.isBlank());
    }

    @Test
    void should_return_google_search_results_for_query() {
        GoogleSearchTool googleSearchTool = new GoogleSearchTool(toolService);
        List<GoogleSearchResult> results = googleSearchTool.search("watsonx.ai java sdk", 1);
        assertNotNull(results);
        assertTrue(results.size() == 1);
        assertNotNull(results.get(0).url());
        assertNotNull(results.get(0).description());
        assertNotNull(results.get(0).title());
    }

    @Test
    void should_return_wikipedia_search_result_for_query() {
        WikipediaTool wikipediaTool = new WikipediaTool(toolService);
        String result = wikipediaTool.search("watsonx.ai");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void should_return_weather_information_for_location() {
        WeatherTool weatherTool = new WeatherTool(toolService);
        String result = weatherTool.find("Rome");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void should_process_web_page_and_return_content() {
        WebCrawlerTool webCrawlerTool = new WebCrawlerTool(toolService);
        String result = webCrawlerTool.process("https://github.com/IBM/watsonx-ai-java-sdk");
        assertNotNull(result);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TAVILY_SEARCH_API_KEY", matches = ".+")
    void test_tashould_return_tavily_search_results_for_queryvily_search_tool() {
        String tavilyApiKey = System.getenv("TAVILY_SEARCH_API_KEY");
        TavilySearchTool tavilySearchTool = new TavilySearchTool(toolService, tavilyApiKey);
        List<TavilySearchResult> results = tavilySearchTool.search("watsonx.ai java sdk", 1);
        assertNotNull(results);
        assertTrue(results.size() == 1);
        assertNotNull(results.get(0).url());
        assertNotNull(results.get(0).title());
        assertNotNull(results.get(0).score());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PYTHON_INTERPRETER_DEPLOYMENT_ID", matches = ".+")
    void should_execute_python_code_and_return_output() {
        String deploymentId = System.getenv("PYTHON_INTERPRETER_DEPLOYMENT_ID");
        PythonInterpreterTool pythonInterpreterTool = new PythonInterpreterTool(toolService, deploymentId);
        String result = pythonInterpreterTool.execute("print(\"Hello World!\")");
        assertNotNull(result);
        assertEquals("Hello World!", result);
    }
}
