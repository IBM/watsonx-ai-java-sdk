/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exeception;

import java.util.Optional;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;

/**
 * Exception thrown when a watsonx api request results in an error response.
 */
public class WatsonxException extends RuntimeException {

  private final Integer statusCode;
  private final WatsonxError details;

  public WatsonxException(Integer statusCode) {
    this.statusCode = statusCode;
    this.details = null;
  }

  public WatsonxException(String message, Integer statusCode, WatsonxError details) {
    super(message);
    this.statusCode = statusCode;
    this.details = details;
  }

  public int statusCode() {
    return statusCode;
  }

  public Optional<WatsonxError> details() {
    return Optional.ofNullable(details);
  }
}
