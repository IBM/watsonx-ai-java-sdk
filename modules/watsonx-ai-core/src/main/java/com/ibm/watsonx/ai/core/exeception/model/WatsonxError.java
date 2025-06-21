/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exeception.model;

import java.util.List;

/**
 * Represents a watsonx error response with detailed information.
 *
 * @param statusCode the HTTP status code of the response
 * @param trace the error trace or correlation ID
 * @param errors the list of individual error details
 */
public record WatsonxError(int statusCode, String trace, List<Error> errors) {

  /**
   * Represents a single error item in the watsonx error response.
   *
   * @param code the string code of the error (e.g., "authorization_rejected")
   * @param message a human-readable message describing the error
   * @param moreInfo optional field with additional information
   */
  public static record Error(String code, String message, String moreInfo) {
    /**
     * Checks if this error matches the specified {@link Code} enum.
     *
     * @param code the {@link Code} to compare against
     * @return true if the code matches; false otherwise
     */
    public boolean is(Code code) {
      return this.code.equals(code.value);
    }
  }

  /**
   * Enumerates the known watsonx error codes.
   */
  public enum Code {
    AUTHORIZATION_REJECTED("authorization_rejected"),
    JSON_TYPE_ERROR("json_type_error"),
    MODEL_NOT_SUPPORTED("model_not_supported"),
    MODEL_NO_SUPPORT_FOR_FUNCTION("model_no_support_for_function"),
    USER_AUTHORIZATION_FAILED("user_authorization_failed"),
    JSON_VALIDATION_ERROR("json_validation_error"),
    INVALID_REQUEST_ENTITY("invalid_request_entity"),
    INVALID_INPUT_ARGUMENT("invalid_input_argument"),
    TOKEN_QUOTA_REACHED("token_quota_reached"),
    AUTHENTICATION_TOKEN_EXPIRED("authentication_token_expired"),
    TEXT_EXTRACTION_EVENT_DOES_NOT_EXIST("text_extraction_event_does_not_exist");

    private final String value;

    Code(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}