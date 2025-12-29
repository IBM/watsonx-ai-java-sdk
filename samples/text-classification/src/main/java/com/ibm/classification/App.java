/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.classification;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Scanner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;

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

            try (Scanner scanner = new Scanner(System.in)) {

                System.out.println("Enter the absolute path to the file to be processed");
                System.out.print("file: ");

                String inputPath = scanner.nextLine().trim();
                if (inputPath.startsWith("'") && inputPath.endsWith("'")) {
                    inputPath = inputPath.substring(1, inputPath.length() - 1);
                }

                var file = new File(inputPath);

                TextClassificationService textClassificationService = TextClassificationService.builder()
                    .apiKey(apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .projectId(projectId)
                    .baseUrl(url)
                    .cosUrl(cosUrl)
                    .documentReference(documentReference)
                    .build();

                Duration timeout = Duration.ofMinutes(5);

                TextClassificationParameters parameters = TextClassificationParameters.builder()
                    .removeUploadedFile(true)
                    .timeout(timeout)
                    .build();

                System.out.println("Starting classification process with a timeout of %s minute(s)...".formatted(timeout.toMinutes()));

                System.out.println("""
                    Classification:

                    %s
                    ---------------
                    """.formatted(textClassificationService.uploadClassifyAndFetch(file, parameters)));

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
