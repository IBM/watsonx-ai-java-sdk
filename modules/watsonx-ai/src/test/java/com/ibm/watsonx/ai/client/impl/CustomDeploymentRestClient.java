/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.deployment.DeploymentResource;
import com.ibm.watsonx.ai.deployment.DeploymentRestClient;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest;
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
    public TextGenerationResponse generate(String transactionId, String deploymentId, Duration timeout, TextRequest textRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'generate'");
    }

    @Override
    public CompletableFuture<Void> generateStreaming(String transactionId, String deploymentId, Duration timeout, TextRequest textRequest,
        TextGenerationHandler handler) {
        throw new UnsupportedOperationException("Unimplemented method 'generateStreaming'");
    }

    @Override
    public ChatResponse chat(String transactionId, String deploymentId, Duration timeout, TextChatRequest textChatRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'chat'");
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(String transactionId, String deploymentId, TextChatRequest textChatRequest,
        ChatClientContext context, ChatHandler handler) {
        throw new UnsupportedOperationException("Unimplemented method 'chatStreaming'");
    }

    @Override
    public ForecastResponse forecast(String transactionId, String deploymentId, Duration timeout, ForecastRequest forecastRequest) {
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
