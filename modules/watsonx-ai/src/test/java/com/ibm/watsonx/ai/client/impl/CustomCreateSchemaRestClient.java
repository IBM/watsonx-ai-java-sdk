/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaResponse;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaRestClient;
import com.ibm.watsonx.ai.textprocessing.schema.create.DeleteRequest;
import com.ibm.watsonx.ai.textprocessing.schema.create.FetchDetailsRequest;
import com.ibm.watsonx.ai.textprocessing.schema.create.StartCreateSchemaRequest;

public class CustomCreateSchemaRestClient extends CreateSchemaRestClient {

    CustomCreateSchemaRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public boolean deleteFile(DeleteFileRequest request) throws FileNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }

    @Override
    public CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteFileAsync'");
    }

    @Override
    public boolean uploadFile(UploadRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'uploadFile'");
    }

    @Override
    public boolean deleteRequest(DeleteRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteRequest'");
    }

    @Override
    public CreateSchemaResponse fetchRequestDetails(FetchDetailsRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'fetchRequestDetails'");
    }

    @Override
    public CreateSchemaResponse startRequest(StartCreateSchemaRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'startRequest'");
    }

    public static final class CustomCreateSchemaRestClientBuilderFactory implements CreateSchemaRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomCreateSchemaRestClient.Builder();
        }
    }

    static final class Builder extends CreateSchemaRestClient.Builder {
        @Override
        public CreateSchemaRestClient build() {
            return new CustomCreateSchemaRestClient(this);
        }
    }
}
