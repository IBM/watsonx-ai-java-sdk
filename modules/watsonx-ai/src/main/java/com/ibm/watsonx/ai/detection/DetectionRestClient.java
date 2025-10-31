/*
 * Copyright IBM Corp. 2025 - 2025
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

    protected DetectionRestClient(Builder builder) {
        super(builder);
    }

    public abstract DetectionResponse<DetectionTextResponse> detect(String transactionId, TextDetectionContentDetectors request);

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
