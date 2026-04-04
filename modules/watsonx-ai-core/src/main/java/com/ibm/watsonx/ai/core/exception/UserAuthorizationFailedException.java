/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Exception thrown when user authorization fails in the watsonx API.
 * <p>
 * This exception corresponds to the error code {@code user_authorization_failed}.
 */
public final class UserAuthorizationFailedException extends WatsonxException {

    /**
     * Constructs a new {@code UserAuthorizationFailedException} with the specified detail message, HTTP status code, and error details.
     *
     * @param message the detail message explaining the exception
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response
     */
    public UserAuthorizationFailedException(String message, Integer statusCode, WatsonxError details) {
        super(message, statusCode, details);
    }

    /**
     * Constructs a new {@code UserAuthorizationFailedException} from an existing {@code WatsonxException}.
     *
     * @param exception the existing {@code WatsonxException} to wrap
     */
    public UserAuthorizationFailedException(WatsonxException exception) {
        super(exception.getMessage(), exception.statusCode(), exception.details().orElse(null));
    }
}