/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

/**
 * Represents cryptographic configuration for encrypting inference requests.
 * <p>
 * This record encapsulates the key reference identifier from a keys management service (e.g., IBM Key Protect) used to enable encryption of requests.
 *
 * @param keyRef the key reference identifier from a keys management service (e.g., CRN format for IBM Key Protect)
 * @see <a href=
 *      "https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-generation.html?context=wx&audience=wdp#inf-encrypt">Encrypting
 *      inference requests</a>
 */
public record Crypto(String keyRef) {}
