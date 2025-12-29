/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

/**
 * Request for interacting with Cloud Pack for Data authentication endpoint.
 *
 * @param username username value
 * @param password password value
 * @param apiKey api-key value
 */
public record TokenRequest(String username, String password, String apiKey) {}
