/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.transcription;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Transcribe Audio APIs.
 */
public abstract class TranscriptionRestClient extends WatsonxRestClient {

    protected TranscriptionRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Sends an audio transcription request
     *
     * @param request the {@link TranscriptionRequest} containing all parameters for the transcription
     * @return a {@link TranscriptionResult} containing the transcription output
     */
    public abstract TranscriptionResult transcribe(TranscriptionRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link TranscriptionRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TranscriptionRestClient.Builder builder() {
        return ServiceLoader.load(TranscriptionRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link TranscriptionRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TranscriptionRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface TranscriptionRestClientBuilderFactory extends Supplier<TranscriptionRestClient.Builder> {}
}
