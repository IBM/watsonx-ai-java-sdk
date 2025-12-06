/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

/**
 * Request for interacting with Cloud Pack for Data authentication endpoint.
 *
 * @param username username value
 * @param password password value
 * @param apiKey the API key used to authenticate with Cloud Pak for Data
 */
public record TokenRequest(String username, String password, String apiKey) {}
