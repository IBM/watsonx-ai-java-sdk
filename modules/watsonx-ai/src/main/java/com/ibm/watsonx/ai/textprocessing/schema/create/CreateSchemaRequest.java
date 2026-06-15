/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import com.ibm.watsonx.ai.textprocessing.DataReference;

/**
 * Represents a request for the Create Schema API.
 *
 * @param projectId the project identifier
 * @param spaceId the space identifier
 * @param documentReference reference to the input document in COS
 * @param parameters parameters
 */
public record CreateSchemaRequest(String projectId, String spaceId, DataReference documentReference, Parameters parameters) {}
