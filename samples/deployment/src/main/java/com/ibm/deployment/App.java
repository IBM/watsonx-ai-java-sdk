/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.deployment;

import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.deployment.FindByIdParameters;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var deployment = config.getValue("WATSONX_DEPLOYMENT", String.class);
        var spaceId = config.getValue("WATSONX_SPACE_ID", String.class);

        AuthenticationProvider authProvider = IAMAuthenticator.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

        DeploymentService deploymentService = DeploymentService.builder()
            .authenticationProvider(authProvider)
            .timeout(Duration.ofSeconds(60))
            .deployment(deployment)
            .url(url)
            .build();

        var deploymentInfo = deploymentService.findById(
            FindByIdParameters.builder()
                .spaceId(spaceId)
                .build()
        );

        System.out.println("""
            ---------------------------------------------
            Model: %s
            Deployment Asset Type: %s
            Deployment Status: %s
            ---------------------------------------------""".formatted(
            deploymentInfo.metadata().name(), deploymentInfo.entity().deployedAssetType(), deploymentInfo.entity().status().state()));

        var message = "How are you?";
        System.out.println("USER: ".concat(message));
        System.out.println("ASSISTANT: ".concat(deploymentService.chat(UserMessage.text(message)).toText()));
    }
}
