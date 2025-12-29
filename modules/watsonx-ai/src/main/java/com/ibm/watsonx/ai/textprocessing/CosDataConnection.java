/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.requireNonNull;

/**
 * Represents the connection asset that holds credentials and configuration to access a COS (Cloud Object Storage) location.
 *
 * @param id The unique identifier of the connection asset.
 */
public record CosDataConnection(String id) {
    public CosDataConnection {
        requireNonNull(id, "id cannot be null");
    }
};
