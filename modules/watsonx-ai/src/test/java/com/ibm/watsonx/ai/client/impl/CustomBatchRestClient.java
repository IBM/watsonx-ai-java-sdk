/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.batch.BatchCancelRequest;
import com.ibm.watsonx.ai.batch.BatchCreateRequest;
import com.ibm.watsonx.ai.batch.BatchData;
import com.ibm.watsonx.ai.batch.BatchListRequest;
import com.ibm.watsonx.ai.batch.BatchListResponse;
import com.ibm.watsonx.ai.batch.BatchRestClient;
import com.ibm.watsonx.ai.batch.BatchRetrieveRequest;

public class CustomBatchRestClient extends BatchRestClient {

    CustomBatchRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public BatchData submit(BatchCreateRequest batchCreateRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'submit'");
    }

    @Override
    public BatchListResponse list(BatchListRequest batchListRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'list'");
    }

    @Override
    public BatchData retrieve(BatchRetrieveRequest batchRetrieveRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'retrieve'");
    }

    @Override
    public BatchData cancel(BatchCancelRequest batchCancelRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'cancel'");
    }

    public static final class CustomBatchRestClientBuilderFactory implements BatchRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomBatchRestClient.Builder();
        }
    }

    static final class Builder extends BatchRestClient.Builder {
        @Override
        public CustomBatchRestClient build() {
            return new CustomBatchRestClient(this);
        }
    }
}
