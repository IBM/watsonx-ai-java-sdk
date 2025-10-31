/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception.model;

import java.util.List;

/**
 * Represents a watsonx error response with detailed information.
 *
 * @param statusCode the HTTP status code of the response
 * @param trace the error trace or correlation ID
 * @param errors the list of individual error details
 */
public record WatsonxError(Integer statusCode, String trace, List<Error> errors) {

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
    public static enum Code {
        /** Authorization was rejected. */
        AUTHORIZATION_REJECTED("authorization_rejected"),

        /** Error due to JSON type mismatch. */
        JSON_TYPE_ERROR("json_type_error"),

        /** The requested model is not supported. */
        MODEL_NOT_SUPPORTED("model_not_supported"),

        /** The model does not support the requested function. */
        MODEL_NO_SUPPORT_FOR_FUNCTION("model_no_support_for_function"),

        /** User authorization failed. */
        USER_AUTHORIZATION_FAILED("user_authorization_failed"),

        /** JSON validation failed. */
        JSON_VALIDATION_ERROR("json_validation_error"),

        /** The request entity is invalid. */
        INVALID_REQUEST_ENTITY("invalid_request_entity"),

        /** An invalid input argument was provided. */
        INVALID_INPUT_ARGUMENT("invalid_input_argument"),

        /** Token quota has been reached. */
        TOKEN_QUOTA_REACHED("token_quota_reached"),

        /** Authentication token has expired. */
        AUTHENTICATION_TOKEN_EXPIRED("authentication_token_expired"),

        /** Text extraction event does not exist. */
        TEXT_EXTRACTION_EVENT_DOES_NOT_EXIST("text_extraction_event_does_not_exist"),

        /** Text classification event does not exist. */
        TEXT_CLASSIFICATION_EVENT_DOES_NOT_EXIST("text_classification_event_does_not_exist"),

        /** Access to Cloud Object Storage was denied. */
        COS_ACCESS_DENIED("AccessDenied"),

        /** Unclassified. */
        UNCLASSIFIED("Unclassified");

        private final String value;

        Code(String value) {
            this.value = value;
        }

        /**
         * Returns the string value associated with this error code.
         *
         * @return the string representation of the error code
         */
        public String value() {
            return value;
        }
    }
}