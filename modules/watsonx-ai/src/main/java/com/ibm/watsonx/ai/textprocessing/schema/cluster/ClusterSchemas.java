/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents a single schema entry used as input for the Cluster Schema API.
 *
 * @param documentName the name that identifies this document schema
 * @param schema the schema definition for the document
 */
public record ClusterSchemas(String documentName, Schema schema) {}
