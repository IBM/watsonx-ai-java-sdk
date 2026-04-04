/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Exception thrown when the authentication token has expired in the watsonx API.
 * <p>
 * This exception corresponds to the error code {@code authentication_token_expired}.
 */
public final class AuthenticationTokenExpiredException extends WatsonxException {

    /**
     * Constructs a new {@code AuthenticationTokenExpiredException} with the specified detail message, HTTP status code, and error details.
     *
     * @param message the detail message explaining the exception
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response
     */
    public AuthenticationTokenExpiredException(String message, Integer statusCode, WatsonxError details) {
        super(message, statusCode, details);
    }

    /**
     * Constructs a new {@code AuthenticationTokenExpiredException} from an existing {@code WatsonxException}.
     *
     * @param exception the existing {@code WatsonxException} to wrap
     */
    public AuthenticationTokenExpiredException(WatsonxException exception) {
        super(exception.getMessage(), exception.statusCode(), exception.details().orElse(null));
    }
}