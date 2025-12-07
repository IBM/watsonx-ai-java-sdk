/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

/**
 * Represents the response body returned by the Cloud Pak for Data authenticatin endpoint.
 *
 * @param accessToken the IAM access token.
 * @param refreshToken a token that may be used to obtain a new IAM access token.
 * @param tokenType the type of the token.
 * @param expiresIn number of seconds until the IAM access token expires.
 * @param expiration UNIX timestamp (in seconds since Jan 1, 1970) indicating when the IAM access token expires.
 * @param scope the scope granted to the token, if applicable.
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    Long expiration,
    String scope) {}
