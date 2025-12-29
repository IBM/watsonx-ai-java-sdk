/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRestClient;
import com.ibm.watsonx.ai.textgeneration.TextRequest;

public class CustomTextGenerationRestClient extends TextGenerationRestClient {

    CustomTextGenerationRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TextGenerationResponse generate(String transactionId, TextRequest textRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'generate'");
    }

    @Override
    public CompletableFuture<Void> generateStreaming(String transactionId, TextRequest textRequest, TextGenerationHandler handler) {
        throw new UnsupportedOperationException("Unimplemented method 'generateStreaming'");
    }

    public static final class CustomTextGenerationRestClientBuilderFactory implements TextGenerationRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTextGenerationRestClient.Builder();
        }
    }

    static final class Builder extends TextGenerationRestClient.Builder {
        @Override
        public TextGenerationRestClient build() {
            return new CustomTextGenerationRestClient(this);
        }
    }
}
