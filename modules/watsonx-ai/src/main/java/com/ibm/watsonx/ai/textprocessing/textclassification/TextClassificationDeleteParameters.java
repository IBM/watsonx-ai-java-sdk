/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import java.util.Optional;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a text classification delete operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextClassificationDeleteParameters.builder()
 *     .projectId("project-id")
 *     .hardDelete(true)
 *     .build();
 * }</pre>
 *
 */
public final class TextClassificationDeleteParameters extends WatsonxParameters {
    private final Optional<Boolean> hardDelete;

    private TextClassificationDeleteParameters(Builder builder) {
        super(builder);
        this.hardDelete = Optional.ofNullable(builder.hardDelete);
    }

    /**
     * Gets the hard delete option.
     *
     * @return an Optional containing true if hard delete is enabled
     */
    public Optional<Boolean> hardDelete() {
        return hardDelete;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextClassificationDeleteParameters.builder()
     *     .projectId("project-id")
     *     .hardDelete(true)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextClassificationDeleteParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private Boolean hardDelete;

        private Builder() {}

        /**
         * Sets the hard delete option.
         *
         * @param hardDelete {@code true} to also delete job metadata.
         */
        public Builder hardDelete(Boolean hardDelete) {
            this.hardDelete = hardDelete;
            return this;
        }

        /**
         * Builds a {@link TextClassificationDeleteParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextClassificationDeleteParameters}
         */
        public TextClassificationDeleteParameters build() {
            return new TextClassificationDeleteParameters(this);
        }
    }
}

