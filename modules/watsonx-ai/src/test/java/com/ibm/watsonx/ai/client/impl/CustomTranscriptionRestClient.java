/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.transcription.TranscriptionRequest;
import com.ibm.watsonx.ai.transcription.TranscriptionRestClient;
import com.ibm.watsonx.ai.transcription.TranscriptionResult;

public class CustomTranscriptionRestClient extends TranscriptionRestClient {

    CustomTranscriptionRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TranscriptionResult transcribe(TranscriptionRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'transcribe'");
    }

    public static final class CustomTranscriptionRestClientBuilderFactory implements TranscriptionRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTranscriptionRestClient.Builder();
        }
    }

    static final class Builder extends TranscriptionRestClient.Builder {
        @Override
        public TranscriptionRestClient build() {
            return new CustomTranscriptionRestClient(this);
        }
    }
}
