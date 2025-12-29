/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import java.io.InputStream;

/**
 * Request wrapper for uploading a file to Cloud Object Storage.
 *
 * @param requestTrackingId Optional identifier used internally by the SDK to trace requests.
 * @param bucketName The name of the COS bucket where the file will be uploaded.
 * @param is The {@link InputStream} containing the file content.
 * @param fileName The name of the file to store in the bucket.
 */
public record UploadRequest(String requestTrackingId, String bucketName, InputStream is, String fileName) {

    public static UploadRequest of(String requestTrackingId, String bucketName, InputStream is, String fileName) {
        return new UploadRequest(requestTrackingId, bucketName, is, fileName);
    }
}
