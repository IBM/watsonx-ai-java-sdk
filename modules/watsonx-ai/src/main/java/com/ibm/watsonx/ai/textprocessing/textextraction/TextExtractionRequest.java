/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static java.util.Objects.requireNonNull;
import java.util.Map;

/**
 * Represents a request for the text extraction api.
 */
public record TextExtractionRequest(String projectId, String spaceId, DataReference documentReference, DataReference resultsReference,
    Parameters parameters, Map<String, Object> custom) {

    /**
     * Represents a reference to external or internal data used during text extraction.
     *
     * @param type The type of data source. Allowable value: {@code connection_asset}.
     * @param connection The connection details containing credentials and context needed to access the data.
     * @param location The specific location details of the data within the connection.
     */
    public record DataReference(String type, CosDataConnection connection, CosDataLocation location) {

        public static final String TYPE_CONNECTION_ASSET = "connection_asset";

        public DataReference {
            type = TYPE_CONNECTION_ASSET;
        }
    };

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
}
