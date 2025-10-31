/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelParameters;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelResponse;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelRestClient;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelTask;

public class CustomFoundationModelRestClient extends FoundationModelRestClient {

    CustomFoundationModelRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public FoundationModelResponse<FoundationModel> getModels(Integer start, Integer limit, String transactionId, Boolean techPreview,
        String filters) {
        throw new UnsupportedOperationException("Unimplemented method 'getModels'");
    }

    @Override
    public FoundationModelResponse<FoundationModelTask> getTasks(FoundationModelParameters parameters) {
        throw new UnsupportedOperationException("Unimplemented method 'getTasks'");
    }

    public static final class CustomFoundationModelRestClientBuilderFactory
        implements FoundationModelRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomFoundationModelRestClient.Builder();
        }
    }

    static final class Builder extends FoundationModelRestClient.Builder {
        @Override
        public FoundationModelRestClient build() {
            return new CustomFoundationModelRestClient(this);
        }
    }
}
