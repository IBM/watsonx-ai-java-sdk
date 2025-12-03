/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a text classification fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextClassificationFetchParameters.builder()
 *     .projectId("project-id")
 *     .build();
 * }</pre>
 *
 */
public final class TextClassificationFetchParameters extends WatsonxParameters {

    private TextClassificationFetchParameters(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextClassificationFetchParameters.builder()
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
     * Builder class for constructing {@link TextClassificationFetchParameters} instances.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link TextClassificationFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextClassificationFetchParameters}
         */
        public TextClassificationFetchParameters build() {
            return new TextClassificationFetchParameters(this);
        }
    }
}
