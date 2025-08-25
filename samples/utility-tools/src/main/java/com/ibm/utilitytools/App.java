/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.utilitytools;

import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var watsonxApiKey = config.getValue("WATSONX_API_KEY", String.class);
        var tavilyApiKey = config.getOptionalValue("TAVILY_SEARCH_API_KEY", String.class);

        AuthenticationProvider authProvider = IAMAuthenticator.builder()
            .apiKey(watsonxApiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

        ToolService toolService = ToolService.builder()
            .authenticationProvider(authProvider)
            .timeout(Duration.ofSeconds(60))
            .url(url)
            .build();

        GoogleSearchTool googleSearchTool = new GoogleSearchTool(toolService);
        WebCrawlerTool webCrawlerTool = new WebCrawlerTool(toolService);
        WikipediaTool wikipediaTool = new WikipediaTool(toolService);
        WeatherTool weatherTool = new WeatherTool(toolService);

        var googleSearchResult = googleSearchTool.search("watsonx.ai java sdk", 1).get(0);

        System.out.println("""
            ----- GOOGLE SEARCH RESULT -----
            URL: %s
            DESCRIPTION: %s
            --------------------------------\n""".formatted(googleSearchResult.url(), googleSearchResult.description()));

        if (tavilyApiKey.isPresent()) {

            TavilySearchTool tavilySearchTool = new TavilySearchTool(toolService, tavilyApiKey.get());
            var tavilySearchResult = tavilySearchTool.search("watsonx.ai java sdk", 1).get(0);

            System.out.println("""
                ----- TAVILY SEARCH RESULT -----
                URL: %s
                TITLE: %s
                CONTENT: %s
                SCORE: %f
                --------------------------------\n""".formatted(tavilySearchResult.url(), tavilySearchResult.title(), tavilySearchResult.content(),
                tavilySearchResult.score()));
        }

        var webCrawlerResult = webCrawlerTool.process("https://github.com/IBM/watsonx-ai-java-sdk");
        System.out.println("""
            ----- WEB CRAWLER RESULT -----
            %s
            ...
            --------------------------------\n""".formatted(webCrawlerResult.subSequence(0, 100)));

        var wikipediaResult = wikipediaTool.search("watsonx.ai");
        System.out.println("""
            ----- WIKIPEDIA RESULT -----
            %s
            ...
            --------------------------------\n""".formatted(wikipediaResult.subSequence(0, 100)));

        var weatherResult = weatherTool.find("Rome");
        System.out.println("""
            ----- WEATHER RESULT -----
            %s
            --------------------------------""".formatted(weatherResult.trim()));
        System.exit(0);
    }
}
