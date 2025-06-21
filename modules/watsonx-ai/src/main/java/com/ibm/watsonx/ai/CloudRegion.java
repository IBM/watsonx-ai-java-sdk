/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

/**
 * Supported IBM Cloud regions.
 */
public enum CloudRegion {

  DALLAS("https://us-south.ml.cloud.ibm.com"),
  FRANKFURT("https://eu-de.ml.cloud.ibm.com"),
  LONDON("https://eu-gb.ml.cloud.ibm.com"),
  TOKYO("https://jp-tok.ml.cloud.ibm.com"),
  SYDNEY("https://au-syd.ml.cloud.ibm.com"),
  TORONTO("https://ca-tor.ml.cloud.ibm.com");

  private final String endpoint;

  CloudRegion(String endpoint) {
    this.endpoint = endpoint;
  }

  public String endpoint() {
    return endpoint;
  }
}
