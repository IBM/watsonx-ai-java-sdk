/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.deployment;

import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var deployment = config.getValue("WATSONX_DEPLOYMENT_ID", String.class);
        var spaceId = config.getValue("WATSONX_SPACE_ID", String.class);

        AuthenticationProvider authProvider = IAMAuthenticator.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

        DeploymentService deploymentService = DeploymentService.builder()
            .authenticationProvider(authProvider)
            .url(url)
            .build();

        var deploymentInfo = deploymentService.findById(
            FindByIdRequest.builder()
                .spaceId(spaceId)
                .deploymentId(deployment)
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
        var chatRequest = ChatRequest.builder()
            .deploymentId(deployment)
            .messages(UserMessage.text(message))
            .build();

        System.out.println("USER: ".concat(message));
        System.out.println("ASSISTANT: ".concat(deploymentService.chat(chatRequest).extractContent()));
    }
}
