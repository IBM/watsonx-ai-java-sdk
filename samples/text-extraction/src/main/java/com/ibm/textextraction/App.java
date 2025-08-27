/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.textextraction;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Scanner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.textextraction.CosReference;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textextraction.TextExtractionService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

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

            try (Scanner scanner = new Scanner(System.in)) {

                System.out.println("Enter the absolute path to the file to be processed: ");
                System.out.print("file: ");

                String inputPath = scanner.nextLine().trim();
                if (inputPath.startsWith("'") && inputPath.endsWith("'")) {
                    inputPath = inputPath.substring(1, inputPath.length() - 1);
                }

                var file = new File(inputPath);

                AuthenticationProvider authProvider = IAMAuthenticator.builder()
                    .apiKey(apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .build();

                TextExtractionService textExtractionService = TextExtractionService.builder()
                    .authenticationProvider(authProvider)
                    .timeout(Duration.ofSeconds(60))
                    .projectId(projectId)
                    .url(url)
                    .cosUrl(cosUrl)
                    .documentReference(documentReference)
                    .resultReference(resultsReference)
                    .build();

                Duration timeout = Duration.ofMinutes(5);

                TextExtractionParameters parameters = TextExtractionParameters.builder()
                    .removeOutputFile(true)
                    .removeUploadedFile(true)
                    .requestedOutputs(Type.MD)
                    .timeout(timeout)
                    .build();

                System.out.println("Starting extraction process with a timeout of %s minute(s)...".formatted(timeout.toMinutes()));

                System.out.println("""
                    Extracted text:

                    %s
                    ---------------
                    """.formatted(textExtractionService.uploadExtractAndFetch(file, parameters)));

                // Wait for the asynchronous deletion
                System.out.print("Shutdown application..");
                Thread.sleep(500);
                System.out.println("DONE!");

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
