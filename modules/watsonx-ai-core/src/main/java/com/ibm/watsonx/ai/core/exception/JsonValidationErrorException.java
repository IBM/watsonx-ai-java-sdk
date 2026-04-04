/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Exception thrown when JSON validation fails in the watsonx API.
 * <p>
 * This exception corresponds to the error code {@code json_validation_error}.
 */
public final class JsonValidationErrorException extends WatsonxException {

    /**
     * Constructs a new {@code JsonValidationErrorException} with the specified detail message, HTTP status code, and error details.
     *
     * @param message the detail message explaining the exception
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response
     */
    public JsonValidationErrorException(String message, Integer statusCode, WatsonxError details) {
        super(message, statusCode, details);
    }

    /**
     * Constructs a new {@code JsonValidationErrorException} from an existing {@code WatsonxException}.
     *
     * @param exception the existing {@code WatsonxException} to wrap
     */
    public JsonValidationErrorException(WatsonxException exception) {
        super(exception.getMessage(), exception.statusCode(), exception.details().orElse(null));
    }
}