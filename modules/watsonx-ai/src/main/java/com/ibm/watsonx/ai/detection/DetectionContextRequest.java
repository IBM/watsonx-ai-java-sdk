/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.detection.detector.BaseDetector;

/**
 * Represents a request to the watsonx.ai Context Detection API for analyzing input text against context documents.
 * <p>
 * Supports context relevance and faithfulness (groundedness) detection.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DetectionContextRequest request =
 *     DetectionContextRequest.builder()
 *         .input("What is IBM?")
 *         .context(List.of("https://en.wikipedia.org/wiki/IBM"))
 *         .detectors(ContextRelevance.ofDefaults(), Faithfulness.ofThreshold(0.5))
 *         .build();
 * }</pre>
 */
public final class DetectionContextRequest extends WatsonxParameters {
    private final String input;
    private final List<String> context;
    private final Map<String, Map<String, Object>> detectors;
    private final String serviceInstanceId;

    private DetectionContextRequest(Builder builder) {
        super(builder);
        input = requireNonNull(builder.input, "input cannot be null");
        context = List.copyOf(requireNonNull(builder.context, "context cannot be null"));
        detectors = Collections.unmodifiableMap(requireNonNull(builder.detectors, "detectors cannot be null"));
        serviceInstanceId = builder.serviceInstanceId;
    }

    /**
     * Returns the input text to analyze.
     *
     * @return the input text
     */
    public String input() {
        return input;
    }

    /**
     * Returns the context documents.
     *
     * @return the list of context document URLs
     */
    public List<String> context() {
        return context;
    }

    /**
     * Returns the detector configurations.
     *
     * @return a map of detector names to their configuration parameters
     */
    public Map<String, Map<String, Object>> detectors() {
        return detectors;
    }

    /**
     * Returns the watsonx.governance service instance id.
     * <p>
     * When set, it is used in place of {@link #projectId()}/{@link #spaceId()} to scope the request.
     *
     * @return the service instance id
     */
    public String serviceInstanceId() {
        return serviceInstanceId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DetectionContextRequest request =
     *     DetectionContextRequest.builder()
     *         .input("What is IBM?")
     *         .context(List.of("https://en.wikipedia.org/wiki/IBM"))
     *         .detectors(ContextRelevance.ofDefaults())
     *         .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DetectionContextRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String input;
        private List<String> context;
        private Map<String, Map<String, Object>> detectors;
        private String serviceInstanceId;

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
         * Sets the context documents.
         *
         * @param context the list of context document URLs (1-100 items)
         */
        public Builder context(List<String> context) {
            this.context = context;
            return this;
        }

        /**
         * Sets the list of detectors to use for this request.
         *
         * @param detectors the list of detectors
         */
        public Builder detectors(List<BaseDetector> detectors) {
            this.detectors = detectors.stream().collect(toMap(BaseDetector::name, BaseDetector::properties));
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
         * Sets the detector configurations directly as a map of detector names to their configuration parameters.
         *
         * @param detectors a map of detector names to their configuration parameters
         */
        Builder detectors(Map<String, Map<String, Object>> detectors) {
            this.detectors = detectors;
            return this;
        }

        /**
         * Sets the watsonx.governance service instance id.
         * <p>
         * When set, it is used in place of {@code projectId}/{@code spaceId} to scope the request.
         *
         * @param serviceInstanceId the service instance id
         */
        public Builder serviceInstanceId(String serviceInstanceId) {
            this.serviceInstanceId = serviceInstanceId;
            return this;
        }

        /**
         * Builds a {@link DetectionContextRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link DetectionContextRequest}
         */
        public DetectionContextRequest build() {
            return new DetectionContextRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((detectors == null) ? 0 : detectors.hashCode());
        result = prime * result + ((serviceInstanceId == null) ? 0 : serviceInstanceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        DetectionContextRequest other = (DetectionContextRequest) obj;
        if (input == null) {
            if (other.input != null)
                return false;
        } else if (!input.equals(other.input))
            return false;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (detectors == null) {
            if (other.detectors != null)
                return false;
        } else if (!detectors.equals(other.detectors))
            return false;
        if (serviceInstanceId == null) {
            if (other.serviceInstanceId != null)
                return false;
        } else if (!serviceInstanceId.equals(other.serviceInstanceId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DetectionContextRequest [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", input=" + input
            + ", context=" + context + ", detectors=" + detectors + ", serviceInstanceId=" + serviceInstanceId + "]";
    }
}
