/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import static java.util.Objects.isNull;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a cluster schema operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ClusterSchemaParameters.builder()
 *     .schemas(schema1, schema2)
 *     .build();
 * }</pre>
 *
 */
public final class ClusterSchemaParameters extends WatsonxParameters {
    private final List<ClusterSchemas> schemas;
    private final ClusterSchemaSemanticConfig semanticConfig;

    private ClusterSchemaParameters(Builder builder) {
        super(builder);
        this.schemas = isNull(builder.schemas) ? null : List.copyOf(builder.schemas);
        this.semanticConfig = builder.semanticConfig;
    }

    /**
     * Gets the list of document schemas to cluster.
     *
     * @return the list of cluster schemas
     */
    public List<ClusterSchemas> schemas() {
        return schemas;
    }

    /**
     * Gets the semantic configuration.
     *
     * @return the semantic configuration
     */
    public ClusterSchemaSemanticConfig semanticConfig() {
        return semanticConfig;
    }

    /**
     * Converts this parameters object to a {@link Parameters} record for API requests.
     *
     * @return a Parameters record containing the configuration
     */
    public Parameters toParameters() {
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
     * ClusterSchemaParameters.builder()
     *     .schemas(schema1, schema2)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ClusterSchemaParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private List<ClusterSchemas> schemas;
        private ClusterSchemaSemanticConfig semanticConfig;

        private Builder() {}

        /**
         * Sets the list of document schemas to cluster.
         *
         * @param schemas one or more cluster schema entries
         */
        public Builder schemas(ClusterSchemas... schemas) {
            this.schemas = Arrays.asList(schemas);
            return this;
        }

        /**
         * Sets the list of document schemas to cluster.
         *
         * @param schemas list of cluster schema entries
         */
        public Builder schemas(List<ClusterSchemas> schemas) {
            this.schemas = schemas;
            return this;
        }

        /**
         * Sets the semantic configuration.
         *
         * @param semanticConfig the semantic configuration
         */
        public Builder semanticConfig(ClusterSchemaSemanticConfig semanticConfig) {
            this.semanticConfig = semanticConfig;
            return this;
        }

        /**
         * Builds a {@link ClusterSchemaParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ClusterSchemaParameters}
         */
        public ClusterSchemaParameters build() {
            return new ClusterSchemaParameters(this);
        }
    }

    @Override
    public String toString() {
        return "ClusterSchemaParameters [" + super.toString() + ", schemas=" + schemas + ", semanticConfig=" + semanticConfig + "]";
    }
}
