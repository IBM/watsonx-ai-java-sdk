/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;

/**
 * Utility class for working with Http objects.
 */
public final class HttpUtils {

    private static final Pattern AUTHORIZATION_PATTERN =
        Pattern.compile("(\\w+\\s)(\\w{4})(\\w+)(\\w{4})");

    protected HttpUtils() {}

    /**
     * Converts the body of an {@link HttpResponse} to a {@link String}, handling various response body types.
     * <p>
     * Supported types include:
     * <ul>
     * <li>{@code String}</li>
     * <li>{@code byte[]}</li>
     * <li>{@code InputStream}</li>
     * <li>{@code Stream<String>}</li>
     * <li>{@code Path}</li>
     * </ul>
     * If the response body is none of the above types, the method throws a {@link IllegalArgumentException}.
     * <p>
     * Throws a {@link RuntimeException} if any I/O error occurs during the conversion.
     *
     * @param response the HTTP response to convert
     * @return the response body as a string
     * @param <T> the type of the response body contained in the {@link HttpResponse}
     * @throws IllegalArgumentException if the body doesn't match one of the supported types
     * @throws RuntimeException if an exception occurs while reading the body
     */
    public static <T> Optional<String> extractBodyAsString(HttpResponse<T> response) {

        T body = response.body();

        if (isNull(body))
            return Optional.empty();

        try {
            if (body instanceof String str) {
                return str.isBlank() ? Optional.empty() : Optional.of(str);
            } else if (body instanceof byte[] bytes) {
                return Optional.of(new String(bytes, StandardCharsets.UTF_8));
            } else if (body instanceof InputStream inputStream) {
                return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            } else if (body instanceof Stream<?> stream) {
                return Optional.of(stream.map(Object::toString).collect(Collectors.joining("\n")));
            } else if (body instanceof Path path) {
                return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
            } else {
                throw new IllegalArgumentException("Unsupported body type: " + body.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert HTTP response body to string", e);
        }
    }

    /**
     * Formats the given map of headers into a single string, where each header is represented as "[key: value]".
     *
     * @param headers a map containing HTTP headers, where keys are header names and values are lists of header values
     * @return a string representation of the headers
     */
    public static String inOneLine(Map<String, List<String>> headers) {
        return headers.entrySet().stream().map(header -> {
            String headerKey = header.getKey();
            String headerValues = header.getValue().stream().collect(Collectors.joining(" "));
            if ("Authorization".equals(headerKey)) {
                headerValues = maskAuthorizationHeaderValue(headerValues);
            }
            return String.format("[%s: %s]", headerKey, headerValues);
        }).collect(joining(", "));
    }

    /**
     * Parses error body based on content type.
     *
     * @param statusCode status code of the http response.
     * @param body The error response body as a String.
     * @param contentType The content type of the error response.
     * @return An instance of WatsonxError parsed from the body.
     */
    public static WatsonxError parseErrorBody(int statusCode, String body, String contentType) {
        if (isNull(contentType))
            throw new IllegalArgumentException("Missing content type");

        if (contentType.contains("application/json")) {

            var error = fromJson(body, WatsonxError.class);
            var genericError = fromJson(body, Map.class);

            if (isNull(error.trace())) {
                if (genericError.containsKey("errorCode"))
                    // IAM Authentication API errors use a non-standard format with "errorCode" and "errorMessage" fields.
                    return parseIAMError(statusCode, genericError);
                else if (genericError.containsKey("code"))
                    // Agent Tool APIs (beta) return errors with a "code" field instead of the standard "errors" array.
                    // TODO: verify if this behavior persists once the Agent Tool APIs are no longer in beta.
                    return parseToolError(genericError);
            } else {
                // File Service API errors use a non-standard format: they include a "trace" field but use "status"
                // instead of "status_code", and nest the error details under an "error" object instead of "errors" array.
                return isNull(error.statusCode()) && genericError.containsKey("error") ? parseFileError(genericError) : error;
            }
        }

        if (contentType.contains("application/xml"))
            return parseXmlError(body);

        throw new RuntimeException(body);
    }

    /**
     * Parses an error response from the File Service APIs into a {@link WatsonxError} with a standardized format.
     *
     * @param body the raw JSON error response as a String.
     * @return An instance of {@link WatsonxError} parsed from the body.
     */
    private static WatsonxError parseFileError(Map<?, ?> body) {
        var error = (Map<?, ?>) body.get("error");
        return new WatsonxError(
            (Integer) body.get("status"),
            (String) body.get("trace"),
            List.of(new Error((String) error.get("code"), (String) error.get("message"), null)));
    }

    /**
     * Parses an error response from the IAM Authentication API into a {@link WatsonxError} with a standardized format.
     *
     * @param body the raw JSON error response as a String.
     * @return An instance of WatsonxError parsed from the body.
     */
    private static WatsonxError parseIAMError(Integer statusCode, Map<?, ?> body) {

        var errorCode = (String) body.get("errorCode");
        var errorMessage = (String) body.get("errorMessage");
        var errorDetails = body.containsKey("errorDetails") ? (String) body.get("errorDetails") : null;

        Error error = new Error(errorCode, errorMessage, errorDetails);
        return new WatsonxError(statusCode, "", List.of(error));
    }

    /**
     * Parses an error response from the beta Agent Tool APIs into a {@link WatsonxError} with a standardized format.
     *
     * @param body the raw JSON error response as a String.
     * @return An instance of WatsonxError parsed from the body.
     */
    private static WatsonxError parseToolError(Map<?, ?> body) {

        var statusCode = (Integer) body.get("code");
        var message = (String) body.get("message");
        var description = (String) body.get("description");

        Error error;
        if (nonNull(description) && description.contains("jwt expired"))
            error = new Error(WatsonxError.Code.AUTHENTICATION_TOKEN_EXPIRED.value(), message, description);
        else
            error = new Error(WatsonxError.Code.UNCLASSIFIED.value(), message, description);

        return new WatsonxError(statusCode, "", List.of(error));
    }

    /**
     * Parses XML error body.
     *
     * @param body The XML error response body as a String.
     * @return An instance of WatsonxError parsed from the XML body.
     */
    private static WatsonxError parseXmlError(String body) {
        Document doc = XmlUtils.parse(body);

        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();

        String codeStr = getTextContent(root, "Code");
        String message = getTextContent(root, "Message");
        String resource = getTextContent(root, "Resource");
        String requestId = getTextContent(root, "RequestId");
        int httpStatusCode = Integer.parseInt(getTextContent(root, "httpStatusCode"));

        Error error = new Error(codeStr, message, resource);
        return new WatsonxError(httpStatusCode, requestId, List.of(error));
    }

    //
    // Masks the sensitive part of a Bearer token in an authorization header.
    //
    private static String maskAuthorizationHeaderValue(String authorizationHeaderValue) {

        Matcher matcher = AUTHORIZATION_PATTERN.matcher(authorizationHeaderValue);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + "..." + matcher.group(4));
        }

        return sb.toString();
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list == null || list.getLength() == 0)
            return null;
        return list.item(0).getTextContent();
    }
}
