/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth;

/**
 * Represents the response body returned by the IBM Cloud IAM token service.
 *
 * @param accessToken the IAM access token used to invoke IBM Cloud APIs.
 * @param refreshToken a token that may be used to obtain a new IAM access token.
 * @param tokenType the type of the token.
 * @param expiresIn number of seconds until the IAM access token expires.
 * @param expiration UNIX timestamp (in seconds since Jan 1, 1970) indicating when the IAM access token expires.
 * @param scope the scope granted to the token, if applicable.
 */
public record IdentityTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    long expiration,
    String scope) {}
