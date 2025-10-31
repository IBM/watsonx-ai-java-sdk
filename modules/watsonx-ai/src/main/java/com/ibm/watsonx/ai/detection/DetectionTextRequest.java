/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.detection.detector.BaseDetector;

/**
 * Represents a request to the watsonx.ai Text Detection API for analyzing text content using detectors.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DetectionTextRequest request =
 *     DetectionTextRequest.builder()
 *         .input("...")
 *         .detectors(Pii.create(), Hap.builder().threshold(0.3).build())
 *         .build();
 * }</pre>
 */
public final class DetectionTextRequest extends WatsonxParameters {
    private final String input;
    private final Map<String, Map<String, Object>> detectors;

    private DetectionTextRequest(Builder builder) {
        super(builder);
        input = requireNonNull(builder.input, "input cannot be null");
        detectors = requireNonNull(builder.detectors, "detectors cannot be null").stream()
            .collect(toMap(BaseDetector::getName, BaseDetector::getProperties));
    }

    public String getInput() {
        return input;
    }

    public Map<String, Map<String, Object>> getDetectors() {
        return detectors;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DetectionTextRequest request =
     *     DetectionTextRequest.builder()
     *         .input("...")
     *         .detectors(Pii.create(), Hap.builder().threshold(0.3).build())
     *         .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DetectionTextRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String input;
        private List<BaseDetector> detectors;

        private Builder() {}

        /**
         * Sets the input text to analyze.
         *
         * @param input the input string
         */
        public Builder input(String input) {
            this.input = input;
            return this;
        }

        /**
         * Sets the list of detectors to use for this request.
         *
         * @param detectors the list of detectors
         */
        public Builder detectors(List<BaseDetector> detectors) {
            this.detectors = detectors;
            return this;
        }

        /**
         * Sets the list of detectors to use for this request.
         *
         * @param detectors the list of detectors
         */
        public Builder detectors(BaseDetector... detectors) {
            return detectors(List.of(detectors));
        }

        /**
         * Builds a {@link DetectionTextRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link DetectionTextRequest}
         */
        public DetectionTextRequest build() {
            return new DetectionTextRequest(this);
        }
    }
}
