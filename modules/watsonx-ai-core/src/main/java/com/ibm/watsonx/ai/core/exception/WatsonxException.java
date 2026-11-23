/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

import static java.util.Objects.nonNull;
import java.util.Optional;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Exception thrown when a watsonx api request results in an error response.
 * <p>
 * This exception captures the HTTP status code returned by the api and optionally includes detailed error information encapsulated in a
 * {@link WatsonxError} object.
 */
public class WatsonxException extends RuntimeException {

    /**
     * The HTTP status code returned by the API.
     */
    private final Integer statusCode;
    /**
     * The detailed error information returned by the API, if available.
     */
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
     * @param details the detailed error information from the API response
     */
    public WatsonxException(String message, Integer statusCode, WatsonxError details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }

    /**
     * Constructs a new {@code WatsonxException} retaining the failure encountered while reading or parsing the error response.
     *
     * @param message the original error response body, or a status description when no body is available
     * @param statusCode the HTTP status code of the error response
     * @param details the detailed error information from the API response, if available
     * @param cause the failure encountered while reading or parsing the error response, if any
     */
    public WatsonxException(String message, Integer statusCode, WatsonxError details, Throwable cause) {
        super(message, cause);
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

    /**
     * Copies the original error response and its diagnostic failures when mapping to a more specific exception.
     *
     * @param exception the existing exception to copy
     */
    protected WatsonxException(WatsonxException exception) {
        this(exception.getMessage(), exception.statusCode(), exception.details().orElse(null));
        if (nonNull(exception.getCause()))
            initCause(exception.getCause());
        for (var suppressed : exception.getSuppressed())
            addSuppressed(suppressed);
    }
}
