/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.timeseries.ForecastRequest;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;
import com.ibm.watsonx.ai.timeseries.TimeSeriesRestClient;

public class CustomTimeSeriesRestClient extends TimeSeriesRestClient {

    CustomTimeSeriesRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public ForecastResponse forecast(String transactionId, ForecastRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'forecast'");
    }

    public static final class CustomTimeSeriesRestClientBuilderFactory implements TimeSeriesRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTimeSeriesRestClient.Builder();
        }
    }

    static final class Builder extends TimeSeriesRestClient.Builder {
        @Override
        public TimeSeriesRestClient build() {
            return new CustomTimeSeriesRestClient(this);
        }
    }
}
