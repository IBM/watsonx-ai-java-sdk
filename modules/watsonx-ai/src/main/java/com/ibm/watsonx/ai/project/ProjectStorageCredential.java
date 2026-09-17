/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

/**
 * Represents a single credential entry within {@link ProjectStorageProperties#credentials()}.
 *
 * @param apiKey the IAM API key for this credential role
 * @param serviceId the IAM service ID for this credential role
 * @param accessKeyId the HMAC access key ID for this credential role
 * @param secretAccessKey the HMAC secret access key for this credential role
 * @param resourceKeyCrn the CRN of the resource key for this credential role
 */
public record ProjectStorageCredential(
    String apiKey,
    String serviceId,
    String accessKeyId,
    String secretAccessKey,
    String resourceKeyCrn) {}
