/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.textprocessing.schema.merge.DeleteRequest;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeFetchDetailsRequest;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaResponse;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaRestClient;
import com.ibm.watsonx.ai.textprocessing.schema.merge.StartMergeSchemaRequest;

public class CustomMergeSchemaRestClient extends MergeSchemaRestClient {

    CustomMergeSchemaRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public boolean deleteRequest(DeleteRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteRequest'");
    }

    @Override
    public MergeSchemaResponse fetchRequestDetails(MergeFetchDetailsRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'fetchRequestDetails'");
    }

    @Override
    public MergeSchemaResponse startRequest(StartMergeSchemaRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'startRequest'");
    }

    public static final class CustomMergeSchemaRestClientBuilderFactory implements MergeSchemaRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomMergeSchemaRestClient.Builder();
        }
    }

    static final class Builder extends MergeSchemaRestClient.Builder {
        @Override
        public MergeSchemaRestClient build() {
            return new CustomMergeSchemaRestClient(this);
        }
    }
}
