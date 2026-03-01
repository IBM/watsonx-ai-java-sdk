/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Request wrapper for deleting a file from Cloud Object Storage.
 *
 * @param requestTrackingId Optional identifier used internally by the SDK to trace requests
 * @param bucketName The name of the COS bucket containing the file.
 * @param fileName The name of the file to delete.
 */
public record DeleteFileRequest(String requestTrackingId, String bucketName, String fileName) {

    /**
     * Creates a new {@link DeleteFileRequest}.
     *
     * @param bucketName the name of the COS bucket containing the file
     * @param fileName the name of the file to delete
     * @return a new {@link DeleteFileRequest} instance
     */
    public static DeleteFileRequest of(String bucketName, String fileName) {
        return of(null, bucketName, fileName);
    }

    /**
     * Creates a new {@link DeleteFileRequest}.
     *
     * @param requestTrackingId optional identifier used internally by the SDK to trace requests
     * @param bucketName the name of the COS bucket containing the file
     * @param fileName the name of the file to delete
     * @return a new {@link DeleteFileRequest} instance
     */
    public static DeleteFileRequest of(String requestTrackingId, String bucketName, String fileName) {
        return new DeleteFileRequest(requestTrackingId, bucketName, fileName);
    }
}

