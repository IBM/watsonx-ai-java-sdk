/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.gateway;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Quality;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Size;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var modelId = config.getValue("WATSONX_MODEL_ID", String.class);

        var imageService = ModelGatewayImageService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .modelId(modelId)
            .build();

        var prompt = "A futuristic city skyline at sunset with flying vehicles and neon lights";

        var parameters = ModelGatewayImageParameters.builder()
            .quality(Quality.LOW)
            .size(Size.SIZE_1024X1024)
            .n(1)
            .build();

        var response = imageService.generate(prompt, parameters);

        System.out.println("Prompt: " + prompt);
        System.out.println("Created: " + response.created());
        System.out.println("Quality: " + response.quality());
        System.out.println("Size: " + response.size());
        System.out.println("Output format: " + response.outputFormat());
        System.out.println();

        for (int i = 0; i < response.data().size(); i++) {
            var image = response.data().get(i);
            System.out.println("Image #" + (i + 1) + ":");
            System.out.println("Base64 length: " + (image.b64Json() != null ? image.b64Json().length() : 0) + " chars");
        }

        System.out.println();
        System.out.println("--- Usage ---");
        if (response.usage() != null) {
            System.out.println("Input tokens:  " + response.usage().inputTokens());
            System.out.println("Output tokens: " + response.usage().outputTokens());
            System.out.println("Total tokens:  " + response.usage().totalTokens());
        } else {
            System.out.println("n/a");
        }
    }
}
