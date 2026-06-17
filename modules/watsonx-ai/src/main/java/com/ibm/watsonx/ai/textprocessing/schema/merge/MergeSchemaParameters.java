/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import java.time.Duration;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents a set of parameters used to control the behavior of an merge schema operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MergeSchemaParameters.builder()
 *     .semanticConfig(semanticConfig)
 *     .timeout(Duration.ofMinutes(5))
 *     .build();
 * }</pre>
 *
 */
public final class MergeSchemaParameters extends WatsonxParameters {
    private final SemanticConfig semanticConfig;
    private final Duration timeout;

    private MergeSchemaParameters(Builder builder) {
        super(builder);
        semanticConfig = builder.semanticConfig;
        timeout = builder.timeout;
    }

    /**
     * Gets the semantic configuration.
     *
     * @return the semantic configuration
     */
    public SemanticConfig semanticConfig() {
        return semanticConfig;
    }

    /**
     * Gets the timeout duration.
     *
     * @return the timeout duration
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Converts this parameters object to a {@link Parameters} record for API requests.
     *
     * @param schema the schema to improve
     * @return a Parameters record containing the configuration
     */
    protected Parameters toParameters(List<Schema> schemas) {
        Parameters.SemanticConfig semanticConfigRecord = null;
        if (semanticConfig != null)
            semanticConfigRecord = new Parameters.SemanticConfig(semanticConfig.defaultModelName());

        return new Parameters(schemas, semanticConfigRecord);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * MergeSchemaParameters.builder()
     *     .semanticConfig(semanticConfig)
     *     .timeout(Duration.ofMinutes(5))
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link MergeSchemaParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private SemanticConfig semanticConfig;
        private Duration timeout;

        private Builder() {}

        /**
         * Sets the semantic configuration.
         *
         * @param semanticConfig the semantic configuration
         */
        public Builder semanticConfig(SemanticConfig semanticConfig) {
            this.semanticConfig = semanticConfig;
            return this;
        }

        /**
         * Sets the timeout for synchronous operations.
         *
         * @param timeout the timeout duration
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds an {@link MergeSchemaParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link MergeSchemaParameters}
         */
        public MergeSchemaParameters build() {
            return new MergeSchemaParameters(this);
        }
    }
}
