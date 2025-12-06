/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServiceLoaderUtils {

    private static Path tempDir;
    private static ClassLoader originalClassLoader;

    public static void setupServiceLoader() throws Exception {
        originalClassLoader = Thread.currentThread().getContextClassLoader();

        tempDir = Files.createTempDirectory("custom-services");
        Path metaInfServices = tempDir.resolve("META-INF/services");
        Files.createDirectories(metaInfServices);

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.chat.ChatRestClient$ChatRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomChatRestClient$CustomChatRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.core.auth.iam.IBMCloudRestClient$IBMCloudRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomIBMCloudRestClient$CustomIBMCloudRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient$CP4DRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomCP4DRestClient$CustomCP4DRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.deployment.DeploymentRestClient$DeploymentRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomDeploymentRestClient$CustomDeploymentRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.embedding.EmbeddingRestClient$EmbeddingRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomEmbeddingRestClient$CustomEmbeddingRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.foundationmodel.FoundationModelRestClient$FoundationModelRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomFoundationModelRestClient$CustomFoundationModelRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.rerank.RerankRestClient$RerankRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomRerankRestClient$CustomRerankRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient$TextExtractionRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomTextExtractionRestClient$CustomTextExtractionRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient$TextClassificationRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomTextClassificationRestClient$CustomTextClassificationRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.textgeneration.TextGenerationRestClient$TextGenerationRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomTextGenerationRestClient$CustomTextGenerationRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.timeseries.TimeSeriesRestClient$TimeSeriesRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomTimeSeriesRestClient$CustomTimeSeriesRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.tokenization.TokenizationRestClient$TokenizationRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomTokenizationRestClient$CustomTokenizationRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.tool.ToolRestClient$ToolRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomToolRestClient$CustomToolRestClientBuilderFactory");

        createServiceFile(metaInfServices,
            "com.ibm.watsonx.ai.detection.DetectionRestClient$DetectionRestClientBuilderFactory",
            "com.ibm.watsonx.ai.client.impl.CustomDetectionRestClient$CustomDetectionRestClientBuilderFactory");

        URLClassLoader tempClassLoader = new URLClassLoader(
            new URL[] { tempDir.toUri().toURL() },
            originalClassLoader
        );
        Thread.currentThread().setContextClassLoader(tempClassLoader);
    }

    public static void cleanupServiceLoader() throws Exception {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        deleteDirectory(tempDir);
    }

    private static void createServiceFile(Path metaInfServices, String serviceInterface, String implementation) throws IOException {
        Path serviceFile = metaInfServices.resolve(serviceInterface);
        Files.write(serviceFile, implementation.getBytes());
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {}
                });
        }
    }
}
