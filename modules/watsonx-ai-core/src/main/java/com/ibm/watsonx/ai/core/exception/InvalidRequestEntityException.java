/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Exception thrown when the request entity is invalid in the watsonx API.
 * <p>
 * This exception corresponds to the error code {@code invalid_request_entity}.
 */
public final class InvalidRequestEntityException extends WatsonxException {

    /**
     * Constructs a new {@code InvalidRequestEntityException} with the specified detail message, HTTP status code, and error details.
     *
     * @param message the detail message explaining the exception
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response
     */
    public InvalidRequestEntityException(String message, Integer statusCode, WatsonxError details) {
        super(message, statusCode, details);
    }

    /**
     * Constructs a new {@code InvalidRequestEntityException} from an existing {@code WatsonxException}.
     *
     * @param exception the existing {@code WatsonxException} to wrap
     */
    public InvalidRequestEntityException(WatsonxException exception) {
        super(exception.getMessage(), exception.statusCode(), exception.details().orElse(null));
    }
}