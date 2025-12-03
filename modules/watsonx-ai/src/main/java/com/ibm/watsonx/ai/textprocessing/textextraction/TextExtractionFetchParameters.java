/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a text extraction fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionFetchParameters.builder()
 *     .projectId("project-id")
 *     .build();
 * }</pre>
 *
 */
public final class TextExtractionFetchParameters extends WatsonxParameters {

    private TextExtractionFetchParameters(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextExtractionFetchParameters.builder()
     *     .projectId("project-id")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionFetchParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link TextExtractionFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextExtractionFetchParameters}
         */
        public TextExtractionFetchParameters build() {
            return new TextExtractionFetchParameters(this);
        }
    }
}
