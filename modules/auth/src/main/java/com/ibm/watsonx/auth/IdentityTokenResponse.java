/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.auth;

/**
 * Represents a response returned by the IBM Cloud IAM token service.
 */
public record IdentityTokenResponse(
  String accessToken,
  String refreshToken,
  String tokenType,
  long expiresIn,
  long expiration,
  String scope) {
}
