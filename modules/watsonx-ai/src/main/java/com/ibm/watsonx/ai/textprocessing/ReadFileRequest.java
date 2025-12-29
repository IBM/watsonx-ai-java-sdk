/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Request wrapper for reading a file from Cloud Object Storage.
 *
 * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
 * @param bucketName The name of the COS bucket containing the file.
 * @param fileName The name of the file to read.
 */
public record ReadFileRequest(String requestTrackingId, String bucketName, String fileName) {
    public static ReadFileRequest of(String bucketName, String fileName) {
        return of(null, bucketName, fileName);
    }

    public static ReadFileRequest of(String requestTrackingId, String bucketName, String fileName) {
        return new ReadFileRequest(requestTrackingId, bucketName, fileName);
    }
}
