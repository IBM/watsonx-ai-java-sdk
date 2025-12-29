/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolRestClient;
import com.ibm.watsonx.ai.tool.ToolService.Resources;
import com.ibm.watsonx.ai.tool.UtilityTool;

public class CustomToolRestClient extends ToolRestClient {

    CustomToolRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public Resources getAll(String transactionId) {
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    @Override
    public UtilityTool getByName(String transactionId, String name) {
        throw new UnsupportedOperationException("Unimplemented method 'getByName'");
    }

    @Override
    public String run(String transactionId, ToolRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    public static final class CustomToolRestClientBuilderFactory implements ToolRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomToolRestClient.Builder();
        }
    }

    static final class Builder extends ToolRestClient.Builder {
        @Override
        public ToolRestClient build() {
            return new CustomToolRestClient(this);
        }
    }
}
