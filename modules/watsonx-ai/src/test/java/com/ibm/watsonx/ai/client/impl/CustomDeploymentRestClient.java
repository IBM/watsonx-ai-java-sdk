/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.deployment.DeploymentResource;
import com.ibm.watsonx.ai.deployment.DeploymentRestClient;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;

public class CustomDeploymentRestClient extends DeploymentRestClient {

    CustomDeploymentRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public DeploymentResource findById(FindByIdRequest parameters) {
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    public TextGenerationResponse generate(GenerateRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'generate'");
    }

    @Override
    public CompletableFuture<Void> generateStreaming(GenerateStreamingRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'generateStreaming'");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'chat'");
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatStreamingRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'chatStreaming'");
    }

    @Override
    public ForecastResponse forecast(ForecastRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'forecast'");
    }

    public static final class CustomDeploymentRestClientBuilderFactory implements DeploymentRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomDeploymentRestClient.Builder();
        }
    }

    static final class Builder extends DeploymentRestClient.Builder {
        @Override
        public DeploymentRestClient build() {
            return new CustomDeploymentRestClient(this);
        }
    }
}
