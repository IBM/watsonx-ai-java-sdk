/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient;

public class CustomTextClassificationRestClient extends TextClassificationRestClient {

    CustomTextClassificationRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TextClassificationResponse startClassification(StartClassificationRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'startClassification'");
    }

    @Override
    public TextClassificationResponse fetchClassificationDetails(FetchClassificationDetailsRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'fetchClassificationDetails'");
    }

    @Override
    public boolean deleteClassification(DeleteClassificationRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteClassification'");
    }

    @Override
    public boolean deleteFile(DeleteFileRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteFile(DeleteFileRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncDeleteFile'");
    }

    @Override
    public boolean upload(UploadRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    public static final class CustomTextClassificationRestClientBuilderFactory implements TextClassificationRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTextClassificationRestClient.Builder();
        }
    }

    static final class Builder extends TextClassificationRestClient.Builder {
        @Override
        public CustomTextClassificationRestClient build() {
            return new CustomTextClassificationRestClient(this);
        }
    }
}
