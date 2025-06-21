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
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.runtime.embedding.EmbeddingParameters;
import com.ibm.watsonx.runtime.embedding.EmbeddingService;

public class App {

  private static final Config config = ConfigProvider.getConfig();

  public static void main(String[] args) throws Exception {

    var url = URI.create(config.getValue("WATSONX_URL", String.class));
    var apiKey = config.getValue("WATSONX_API_KEY", String.class);
    var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

    AuthenticationProvider authProvider = IAMAuthenticator.builder()
      .apiKey(apiKey)
      .timeout(Duration.ofSeconds(60))
      .build();

    EmbeddingService embeddingService = EmbeddingService.builder()
      .authenticationProvider(authProvider)
      .projectId(projectId)
      .modelId("ibm/granite-embedding-278m-multilingual")
      .url(url)
      .build();

    var response = embeddingService.embedding(
      List.of("Hello"),
      EmbeddingParameters.builder()
        .inputText(true)
        .build()
    );

    System.out.println(response);
  }
}
