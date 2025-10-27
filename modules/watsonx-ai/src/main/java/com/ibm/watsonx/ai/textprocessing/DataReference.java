/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;


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
