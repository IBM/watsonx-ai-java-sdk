/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Detection APIs.
 */
public abstract class DetectionRestClient extends WatsonxRestClient {

    public static final String GOVERNANCE_INSTANCE_ID_HEADER = "x-governance-instance-id";

    protected DetectionRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes a text content detection request.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param serviceInstanceId the watsonx.governance service instance id
     * @param request the detection request containing input text and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public abstract DetectionResponse<DetectionTextResponse> detect(String transactionId, String serviceInstanceId,
        TextDetectionContentDetectors request);

    /**
     * Executes a context-based detection request (context relevance and faithfulness).
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param serviceInstanceId the watsonx.governance service instance id
     * @param request the detection request containing input text, context documents, and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public abstract DetectionResponse<DetectionContextResponse> detectContext(String transactionId, String serviceInstanceId,
        TextDetectionContextDetectors request);

    /**
     * Executes a generated text detection request (answer relevance).
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param serviceInstanceId the watsonx.governance service instance id
     * @param request the detection request containing a prompt, generated text, and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public abstract DetectionResponse<DetectionGeneratedResponse> detectGenerated(String transactionId, String serviceInstanceId,
        TextDetectionGeneratedDetectors request);

    /**
     * Creates a new {@link Builder} using the first available {@link DetectionRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static DetectionRestClient.Builder builder() {
        return ServiceLoader.load(DetectionRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link DetectionRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<DetectionRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface DetectionRestClientBuilderFactory extends Supplier<DetectionRestClient.Builder> {}
}
