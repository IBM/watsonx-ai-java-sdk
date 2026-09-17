/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the {@code properties} sub-object within a {@link ProjectStorage} object.
 *
 * @param bucketName the name of the Cloud Object Storage bucket
 * @param bucketRegion the region of the bucket
 * @param endpointUrl the endpoint URL for the bucket
 * @param credentials a map of role name to {@link ProjectStorageCredential}
 */
public record ProjectStorageProperties(
    String bucketName,
    String bucketRegion,
    String endpointUrl,
    Map<String, ProjectStorageCredential> credentials) {

    public ProjectStorageProperties {
        credentials = credentials == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(credentials));
    }
}
