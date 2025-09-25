/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.tokenization;

import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.tokenization.TokenizationService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            TokenizationService tokenizationService = TokenizationService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .timeout(Duration.ofSeconds(60))
                .modelId("ibm/granite-3-3-8b-instruct")
                .baseUrl(url)
                .build();

            var response = tokenizationService.tokenize("Hello!");
            System.out.println(Json.prettyPrint(response));
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
