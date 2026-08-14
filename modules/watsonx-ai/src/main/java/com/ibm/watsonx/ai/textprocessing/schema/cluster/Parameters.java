/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents the configuration parameters used by the Cluster Schema API.
 *
 * @param schemas a list of document schemas to cluster
 * @param semanticConfig properties related to semantic config
 */
public record Parameters(List<ClusterSchemas> schemas, SemanticConfig semanticConfig) {

    public Parameters {
        schemas = isNull(schemas) ? null : List.copyOf(schemas);
    }

    /**
     * Represents the semantic configuration for the Cluster Schema API.
     *
     * @param defaultModelName the model name to use
     */
    public record SemanticConfig(String defaultModelName) {}
}
