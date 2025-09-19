/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Time series forecast APIs.
 */
public abstract class TimeSeriesRestClient extends WatsonxRestClient {

    protected TimeSeriesRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes a time series forecast request against the watsonx.ai API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param request The {@link ForecastRequest} request.
     * @return A {@link ForecastResponse} containing the predicted time series values and associated metadata.
     */
    public abstract ForecastResponse forecast(String transactionId, ForecastRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link TimeSeriesRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TimeSeriesRestClient.Builder builder() {
        return ServiceLoader.load(TimeSeriesRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link TimeSeriesRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TimeSeriesRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks (e.g., Quarkus, Spring) to provide their own client implementations.
     */
    public interface TimeSeriesRestClientBuilderFactory extends Supplier<TimeSeriesRestClient.Builder> {}
}
