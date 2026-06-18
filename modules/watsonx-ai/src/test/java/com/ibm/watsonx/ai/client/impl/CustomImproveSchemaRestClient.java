/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.textprocessing.schema.improve.DeleteRequest;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveFetchDetailsRequest;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaResponse;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaRestClient;
import com.ibm.watsonx.ai.textprocessing.schema.improve.StartImproveSchemaRequest;

public class CustomImproveSchemaRestClient extends ImproveSchemaRestClient {

    CustomImproveSchemaRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public boolean deleteRequest(DeleteRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteRequest'");
    }

    @Override
    public ImproveSchemaResponse fetchRequestDetails(ImproveFetchDetailsRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'fetchRequestDetails'");
    }

    @Override
    public ImproveSchemaResponse startRequest(StartImproveSchemaRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'startRequest'");
    }

    public static final class CustomImproveSchemaRestClientBuilderFactory implements ImproveSchemaRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomImproveSchemaRestClient.Builder();
        }
    }

    static final class Builder extends ImproveSchemaRestClient.Builder {
        @Override
        public ImproveSchemaRestClient build() {
            return new CustomImproveSchemaRestClient(this);
        }
    }
}
