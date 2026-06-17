/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import java.util.Optional;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of an merge schema delete operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MergeSchemaDeleteParameters.builder()
 *     .projectId("project-id")
 *     .hardDelete(true)
 *     .build();
 * }</pre>
 *
 */
public final class MergeSchemaDeleteParameters extends WatsonxParameters {
    private final Optional<Boolean> hardDelete;

    private MergeSchemaDeleteParameters(Builder builder) {
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
     * MergeSchemaDeleteParameters.builder()
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
     * Builder class for constructing {@link MergeSchemaDeleteParameters} instances.
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
         * Builds a {@link MergeSchemaDeleteParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link MergeSchemaDeleteParameters}
         */
        public MergeSchemaDeleteParameters build() {
            return new MergeSchemaDeleteParameters(this);
        }
    }
}
