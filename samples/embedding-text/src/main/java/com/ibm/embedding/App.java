/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.embedding;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            EmbeddingService embeddingService = EmbeddingService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .timeout(Duration.ofSeconds(60))
                .modelId("ibm/granite-embedding-278m-multilingual")
                .baseUrl(url)
                .build();

            var response = embeddingService.embedding(
                List.of("Hello"),
                EmbeddingParameters.builder()
                    .inputText(true)
                    .build()
            );

            System.out.println(response);
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
