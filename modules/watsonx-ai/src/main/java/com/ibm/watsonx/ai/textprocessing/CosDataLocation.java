/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.requireNonNull;

/**
 * Represents the location of the data within a Cloud Object Storage (COS) connection.
 *
 * @param fileName The name of the file or directory in the bucket.
 * @param bucket Optional override for the bucket name defined in the connection asset.
 */
public record CosDataLocation(String fileName, String bucket) {
    public CosDataLocation {
        requireNonNull(fileName, "fileName cannot be null");
    }
}
