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
 * Represents a request to the watsonx.ai Generated Text Detection API for analyzing a prompt and its generated response.
 * <p>
 * Supports answer relevance detection.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DetectionGeneratedRequest request =
 *     DetectionGeneratedRequest.builder()
 *         .prompt("What is IBM?")
 *         .generatedText("IBM is a technology company.")
 *         .detectors(AnswerRelevance.ofDefaults())
 *         .build();
 * }</pre>
 */
public final class DetectionGeneratedRequest extends WatsonxParameters {
    private final String prompt;
    private final String generatedText;
    private final Map<String, Map<String, Object>> detectors;
    private final String serviceInstanceId;

    private DetectionGeneratedRequest(Builder builder) {
        super(builder);
        prompt = requireNonNull(builder.prompt, "prompt cannot be null");
        generatedText = requireNonNull(builder.generatedText, "generatedText cannot be null");
        detectors = Collections.unmodifiableMap(requireNonNull(builder.detectors, "detectors cannot be null"));
        serviceInstanceId = builder.serviceInstanceId;
    }

    /**
     * Returns the text prompt.
     *
     * @return the prompt
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Returns the generated text.
     *
     * @return the generated text
     */
    public String generatedText() {
        return generatedText;
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
     * DetectionGeneratedRequest request =
     *     DetectionGeneratedRequest.builder()
     *         .prompt("What is IBM?")
     *         .generatedText("IBM is a technology company.")
     *         .detectors(AnswerRelevance.ofDefaults())
     *         .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DetectionGeneratedRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String prompt;
        private String generatedText;
        private Map<String, Map<String, Object>> detectors;
        private String serviceInstanceId;

        private Builder() {}

        /**
         * Sets the text prompt.
         *
         * @param prompt the prompt string
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Sets the generated text.
         *
         * @param generatedText the generated text string
         */
        public Builder generatedText(String generatedText) {
            this.generatedText = generatedText;
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
         * Builds a {@link DetectionGeneratedRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link DetectionGeneratedRequest}
         */
        public DetectionGeneratedRequest build() {
            return new DetectionGeneratedRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((prompt == null) ? 0 : prompt.hashCode());
        result = prime * result + ((generatedText == null) ? 0 : generatedText.hashCode());
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
        DetectionGeneratedRequest other = (DetectionGeneratedRequest) obj;
        if (prompt == null) {
            if (other.prompt != null)
                return false;
        } else if (!prompt.equals(other.prompt))
            return false;
        if (generatedText == null) {
            if (other.generatedText != null)
                return false;
        } else if (!generatedText.equals(other.generatedText))
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
        return "DetectionGeneratedRequest [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", prompt="
            + prompt + ", generatedText=" + generatedText + ", detectors=" + detectors + ", serviceInstanceId=" + serviceInstanceId + "]";
    }
}
