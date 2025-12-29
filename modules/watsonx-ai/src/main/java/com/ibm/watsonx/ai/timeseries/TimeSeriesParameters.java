/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxModelParameters;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.timeseries.ForecastRequest.Parameters;


/**
 * Represents a set of parameters used to control the behavior of a forecast generation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = TimeSeriesParameters.builder()
 *     .predictionLength(512)
 *     .build();
 * }</pre>
 *
 */
public final class TimeSeriesParameters extends WatsonxModelParameters {
    private final Integer predictionLength;
    private final ForecastData futureData;

    private TimeSeriesParameters(Builder builder) {
        super(builder);
        predictionLength = builder.predictionLength;
        futureData = builder.futureData;
    }

    /**
     * Gets the prediction length.
     *
     * @return the number of time steps to predict
     */
    public Integer predictionLength() {
        return predictionLength;
    }

    /**
     * Gets the future data for exogenous features.
     *
     * @return the future data
     */
    public ForecastData futureData() {
        return futureData;
    }

    public Parameters toParameters() {
        return (nonNull(predictionLength)) ? new Parameters(predictionLength) : null;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = TimeSeriesParameters.builder()
     *     .predictionLength(512)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TimeSeriesParameters} instances.
     */
    public static final class Builder extends WatsonxModelParameters.Builder<Builder> {
        private Integer predictionLength;
        private ForecastData futureData;

        private Builder() {}

        /**
         * Sets the prediction length for the forecast.
         *
         * @param predictionLength number of time steps to predict
         */
        public Builder predictionLength(int predictionLength) {
            this.predictionLength = predictionLength;
            return this;
        }

        /**
         * Sets the future data for the forecast.
         * <p>
         * Allows the use of exogenous or known-in-advance features that extend into the forecast horizon (e.g., holidays, weather, scheduled events).
         * This data must be structured similarly to the main input data but should only contain future timestamps and must not include the target
         * columns.
         * <p>
         * <strong>Note:</strong> This parameter is only applicable when using {@link DeploymentService}.
         *
         * @param futureData additional future-known features for the forecast
         */
        public Builder futureData(ForecastData futureData) {
            this.futureData = futureData;
            return this;
        }

        /**
         * Builds a {@link TimeSeriesParameters} instance.
         *
         * @return a new instance of {@link TimeSeriesParameters}
         */
        public TimeSeriesParameters build() {
            return new TimeSeriesParameters(this);
        }
    }
}
