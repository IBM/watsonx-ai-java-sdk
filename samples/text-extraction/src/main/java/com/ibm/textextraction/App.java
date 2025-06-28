/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.textextraction;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.textextraction.CosReference;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionService;

public class App {

  private static final Config config = ConfigProvider.getConfig();

  public static void main(String[] args) throws Exception {

    var url = URI.create(config.getValue("WATSONX_URL", String.class));
    var apiKey = config.getValue("WATSONX_API_KEY", String.class);
    var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
    var cosUrl = config.getValue("CLOUD_OBJECT_STORAGE_URL", String.class);

    var documentReference = CosReference.of(
      config.getValue("WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID", String.class),
      config.getValue("WATSONX_DOCUMENT_REFERENCE_BUCKET", String.class)
    );

    var resultsReference = CosReference.of(
      config.getValue("WATSONX_RESULTS_REFERENCE_CONNECTION_ID", String.class),
      config.getValue("WATSONX_RESULTS_REFERENCE_BUCKET", String.class)
    );

    AuthenticationProvider authProvider = IAMAuthenticator.builder()
      .apiKey(apiKey)
      .timeout(Duration.ofSeconds(60))
      .build();

    TextExtractionService textExtractionService = TextExtractionService.builder()
      .authenticationProvider(authProvider)
      .projectId(projectId)
      .url(url).cosUrl(cosUrl)
      .documentReference(documentReference)
      .resultReference(resultsReference)
      .build();

    TextExtractionParameters parameters = TextExtractionParameters.builder()
      .removeOutputFile(true)
      .removeUploadedFile(true)
      .build();

    File file = new File(URI.create("file:/home/adimaio/downloads/0.png"));
    System.out.println(textExtractionService.uploadExtractAndFetch(file, parameters));
  }
}
