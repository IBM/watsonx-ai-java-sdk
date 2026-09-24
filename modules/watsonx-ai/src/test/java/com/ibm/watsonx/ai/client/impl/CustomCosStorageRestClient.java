/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.storage.cos.CosStorageRestClient;

public class CustomCosStorageRestClient extends CosStorageRestClient {

    CustomCosStorageRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public boolean upload(String requestTrackingId, InputStream is, String fileName) {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public boolean upload(String requestTrackingId, File file) {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public String uploadAndGetId(String requestTrackingId, InputStream is, String fileName) {
        throw new UnsupportedOperationException("Unimplemented method 'uploadAndGetId'");
    }

    @Override
    public String readFile(String requestTrackingId, String fileName) throws FileNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'readFile'");
    }

    @Override
    public CompletableFuture<Boolean> deleteFileAsync(DeleteFileRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteFileAsync'");
    }

    public static final class CustomCosStorageRestClientBuilderFactory implements CosStorageRestClientBuilderFactory {
        @Override
        public CosStorageRestClient.Builder<?> get() {
            return new CustomCosStorageRestClient.Builder();
        }
    }

    static final class Builder extends CosStorageRestClient.Builder<Builder> {
        @Override
        public CustomCosStorageRestClient build() {
            return new CustomCosStorageRestClient(this);
        }
    }
}
