/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

/**
 * Specifies the authentication mode to use when connecting to watsonx.ai services.
 * <p>
 * This enumeration defines the supported mechanisms for acquiring access tokens or authenticating requests. The selected mode influences how
 * credentials are interpreted and which authentication flow is executed.
 */
public enum AuthMode {
    LEGACY,
    IAM,
    ZEN_API_KEY,
}
