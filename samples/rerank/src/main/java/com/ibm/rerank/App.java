/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.rerank;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.rerank.RerankParameters;
import com.ibm.watsonx.ai.rerank.RerankService;

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

    RerankService rerankService = RerankService.builder()
      .authenticationProvider(authProvider)
      .projectId(projectId)
      .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
      .url(url)
      .build();

    RerankParameters parameters = RerankParameters.builder()
      .query(true)
      .inputs(true)
      .build();

    var response = rerankService.rerank(
      "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
      List.of(
        "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine.",
        "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
      ),
      parameters
    );

    System.out.println(Json.prettyPrint(response));
  }
}
