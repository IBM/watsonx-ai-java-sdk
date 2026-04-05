/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.file.FileData;
import com.ibm.watsonx.ai.file.FileDeleteRequest;
import com.ibm.watsonx.ai.file.FileDeleteResponse;
import com.ibm.watsonx.ai.file.FileListRequest;
import com.ibm.watsonx.ai.file.FileListResponse;
import com.ibm.watsonx.ai.file.FileRestClient;
import com.ibm.watsonx.ai.file.FileRetrieveRequest;
import com.ibm.watsonx.ai.file.FileUploadRequest;

public class CustomFileRestClient extends FileRestClient {

    CustomFileRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public FileData upload(FileUploadRequest fileUploadRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public FileListResponse list(FileListRequest fileListRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'list'");
    }

    @Override
    public String retrieve(FileRetrieveRequest fileRetrieveRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'retrieve'");
    }

    @Override
    public FileDeleteResponse delete(FileDeleteRequest fileDeleteRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public CompletableFuture<FileDeleteResponse> deleteAsync(FileDeleteRequest fileDeleteRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAsync'");
    }

    public static final class CustomFileRestClientBuilderFactory implements FileRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomFileRestClient.Builder();
        }
    }

    static final class Builder extends FileRestClient.Builder {
        @Override
        public CustomFileRestClient build() {
            return new CustomFileRestClient(this);
        }
    }
}
