/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Represents the location of data within a storage connection.
 * <p>
 * For {@code connection_asset} references: {@code fileName} and optionally {@code bucket} are used.
 * <p>
 * For {@code container} references: only {@code path} is used.
 *
 * @param fileName The name of the file or directory in the bucket. Used only for {@code connection_asset} type.
 * @param bucket Optional override for the bucket name defined in the connection asset. Used only for {@code connection_asset} type.
 * @param path The file path within the container. Used only for {@code container} type.
 */
public record CosDataLocation(String fileName, String bucket, String path) {}
