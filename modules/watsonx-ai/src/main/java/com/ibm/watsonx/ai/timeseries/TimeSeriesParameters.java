/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.WatsonxParameters.WatsonxModelParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
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

    public TimeSeriesParameters(Builder builder) {
        super(builder);
        predictionLength = builder.predictionLength;
    }

    public Integer getPredictionLength() {
        return predictionLength;
    }

    protected Parameters toParameters() {
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

        public Builder predictionLength(int predictionLength) {
            this.predictionLength = predictionLength;
            return this;
        }

        /**
         * Builds a {@link EmbeddingParameters} instance.
         *
         * @return a new instance of {@link TimeSeriesParameters}
         */
        public TimeSeriesParameters build() {
            return new TimeSeriesParameters(this);
        }
    }
}
