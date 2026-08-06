/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

/**
 * Represents a request for the Cluster Schema API.
 *
 * @param projectId the project identifier
 * @param spaceId the space identifier
 * @param parameters the parameters containing the schemas to cluster
 */
public record ClusterSchemaRequest(String projectId, String spaceId, Parameters parameters) {}
