/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionResponse;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient;

public class CustomTextExtractionRestClient extends TextExtractionRestClient {

    CustomTextExtractionRestClient(Builder builder) {
        super(builder);
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
    public String readFile(ReadFileRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'readFile'");
    }

    @Override
    public boolean upload(UploadRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public boolean deleteExtraction(DeleteExtractionRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteExtraction'");
    }

    @Override
    public TextExtractionResponse fetchExtractionDetails(FetchExtractionDetailsRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'fetchExtractionDetails'");
    }

    @Override
    public TextExtractionResponse startExtraction(StartExtractionRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'startExtraction'");
    }

    public static final class CustomTextExtractionRestClientBuilderFactory implements TextExtractionRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTextExtractionRestClient.Builder();
        }
    }

    static final class Builder extends TextExtractionRestClient.Builder {
        @Override
        public TextExtractionRestClient build() {
            return new CustomTextExtractionRestClient(this);
        }
    }
}
