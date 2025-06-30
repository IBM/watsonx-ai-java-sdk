/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.CosDataConnection;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.CosDataLocation;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.DataReference;

/**
 * Represents a reference to a Cloud Object Storage (COS) resource, defined by a connection identifier and a bucket name.
 *
 * @param connection the identifier of the COS connection
 * @param bucket the name of the COS bucket
 */
public record CosReference(String connection, String bucket) {
  public CosReference {
    requireNonNull(connection, "connection can't be null");
    requireNonNull(bucket, "bucket can't be null");
  }

  public DataReference toDataReference(String fileName) {
    requireNonNull(fileName, "fileName can't be null");
    return new DataReference(
      DataReference.TYPE_CONNECTION_ASSET,
      new CosDataConnection(connection),
      new CosDataLocation(fileName, bucket)
    );
  }

  public static CosReference of(String connection, String bucket) {
    return new CosReference(connection, bucket);
  }
};
