/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import java.net.http.HttpHeaders;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

/**
 * Translates HTTP failures independently of their optional error representation.
 */
final class HttpErrorMapper {

    private HttpErrorMapper() {}

    static WatsonxException toException(int statusCode, HttpHeaders headers, String body) {
        if (body == null || body.isEmpty())
            return new WatsonxException("Status code: " + statusCode, statusCode, null);

        WatsonxError details;
        try {
            details = HttpUtils.parseErrorBody(statusCode, body, headers.firstValue("Content-Type").orElse(null));
        } catch (RuntimeException cause) {
            return new WatsonxException(body, statusCode, null, cause);
        }
        return HttpUtils.mapWatsonxException(new WatsonxException(body, statusCode, details));
    }
}
