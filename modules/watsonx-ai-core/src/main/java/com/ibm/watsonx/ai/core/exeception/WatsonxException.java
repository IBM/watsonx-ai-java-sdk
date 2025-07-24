/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exeception;

import java.util.Optional;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;

/**
 * Exception thrown when a watsonx api request results in an error response.
 * <p>
 * This exception captures the HTTP status code returned by the api and optionally includes detailed error information encapsulated in a
 * {@link WatsonxError} object.
 */
public final class WatsonxException extends RuntimeException {

    private final Integer statusCode;
    private final WatsonxError details;

    /**
     * Constructs a new {@code WatsonxException} with the specified status code.
     *
     * @param statusCode the HTTP status code of the error response
     */
    public WatsonxException(Integer statusCode) {
        this.statusCode = statusCode;
        this.details = null;
    }

    /**
     * Constructs a new {@code WatsonxException} with the specified detail message, HTTP status code, and error details.
     *
     * @param message the detail message explaining the exception
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response, may be {@code null}
     */
    public WatsonxException(String message, Integer statusCode, WatsonxError details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns an {@link Optional} containing the detailed error information, if available.
     *
     * @return an {@code Optional} with the error details, or empty if none are present
     */
    public Optional<WatsonxError> details() {
        return Optional.ofNullable(details);
    }
}
