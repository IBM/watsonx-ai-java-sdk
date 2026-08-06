/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.List;
import com.ibm.watsonx.ai.gateway.catalog.ModelGatewayCatalogRestClient;
import com.ibm.watsonx.ai.gateway.catalog.ModelGatewayModel;

public class CustomModelGatewayCatalogRestClient extends ModelGatewayCatalogRestClient {

    CustomModelGatewayCatalogRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public List<ModelGatewayModel> listModels() {
        throw new UnsupportedOperationException("Unimplemented method 'listModels'");
    }

    @Override
    public ModelGatewayModel getModel(String modelId) {
        throw new UnsupportedOperationException("Unimplemented method 'getModel'");
    }

    public static final class CustomModelGatewayCatalogRestClientBuilderFactory
        implements ModelGatewayCatalogRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomModelGatewayCatalogRestClient.Builder();
        }
    }

    static final class Builder extends ModelGatewayCatalogRestClient.Builder {
        @Override
        public ModelGatewayCatalogRestClient build() {
            return new CustomModelGatewayCatalogRestClient(this);
        }
    }
}
