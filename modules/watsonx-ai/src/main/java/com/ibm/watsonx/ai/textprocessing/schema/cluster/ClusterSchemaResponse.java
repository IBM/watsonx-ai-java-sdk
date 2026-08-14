/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import static java.util.Objects.isNull;
import java.util.List;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.Metadata;

/**
 * Represents a response for the Cluster Schema API.
 *
 * @param metadata metadata associated with the response
 * @param entity the cluster schema entity
 */
public record ClusterSchemaResponse(Metadata metadata, Entity entity) {

    /**
     * Represents the full cluster schema entity.
     *
     * @param parameters the parameters used for this cluster schema process
     * @param results the current status and results of the cluster schema process
     */
    public record Entity(Parameters parameters, ClusterSchemaResult results) {}

    /**
     * Represents the result and status of a cluster schema process.
     *
     * @param status the status of the request
     * @param runningAt the time when processing started
     * @param completedAt the time when the request completed or failed
     * @param schemas the schemas after being clustered, grouped into semantically similar clusters
     * @param error optional error details in case of failure
     */
    public record ClusterSchemaResult(String status, String runningAt, String completedAt, List<List<ClusterSchemas>> schemas, Error error) {
        public ClusterSchemaResult {
            schemas = isNull(schemas) ? null : List.copyOf(schemas);
        }
    }
}
