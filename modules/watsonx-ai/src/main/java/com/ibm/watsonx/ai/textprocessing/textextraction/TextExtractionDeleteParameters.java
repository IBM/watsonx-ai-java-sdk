/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.Optional;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a text extraction delete operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionDeleteParameters.builder()
 *     .projectId("project-id")
 *     .hardDelete(true)
 *     .build();
 * }</pre>
 *
 */
public final class TextExtractionDeleteParameters extends WatsonxParameters {
    private final Optional<Boolean> hardDelete;

    private TextExtractionDeleteParameters(Builder builder) {
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
     * TextExtractionDeleteParameters.builder()
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
     * Builder class for constructing {@link TextExtractionDeleteParameters} instances.
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
         * Builds a {@link TextExtractionDeleteParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextExtractionDeleteParameters}
         */
        public TextExtractionDeleteParameters build() {
            return new TextExtractionDeleteParameters(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((hardDelete == null) ? 0 : hardDelete.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        TextExtractionDeleteParameters other = (TextExtractionDeleteParameters) obj;
        if (hardDelete == null) {
            if (other.hardDelete != null)
                return false;
        } else if (!hardDelete.equals(other.hardDelete))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextExtractionDeleteParameters [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId
            + ", hardDelete=" + hardDelete + "]";
    }
}
