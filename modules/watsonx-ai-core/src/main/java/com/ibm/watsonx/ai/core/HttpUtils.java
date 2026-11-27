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
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.ibm.watsonx.ai.core.exception.AuthenticationTokenExpiredException;
import com.ibm.watsonx.ai.core.exception.AuthorizationRejectedException;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.InvalidRequestEntityException;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.exception.JsonTypeErrorException;
import com.ibm.watsonx.ai.core.exception.JsonValidationErrorException;
import com.ibm.watsonx.ai.core.exception.ModelNoSupportForFunctionException;
import com.ibm.watsonx.ai.core.exception.ModelNotSupportedException;
import com.ibm.watsonx.ai.core.exception.TokenQuotaReachedException;
import com.ibm.watsonx.ai.core.exception.UserAuthorizationFailedException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Code;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;

/**
 * Utility class for working with Http objects.
 */
public final class HttpUtils {

    /**
     * Prevents direct instantiation of this utility class.
     */
    protected HttpUtils() {}

    /**
     * Encodes a raw resource identifier as one URI path segment using UTF-8.
     * <p>
     * Reserved characters such as slashes, question marks, and percent signs are encoded rather than interpreted as URI structure.
     *
     * @param value the unencoded resource identifier
     * @return the encoded path segment
     * @throws IllegalArgumentException if the identifier is {@code .} or {@code ..}
     */
    public static String encodePathSegment(String value) {
        if (".".equals(value) || "..".equals(value))
            throw new IllegalArgumentException("Resource identifier must not be \".\" or \"..\"");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

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
        return LogSanitizer.headers(headers).entrySet().stream().map(header -> {
            String headerKey = header.getKey();
            String headerValues = header.getValue().stream().collect(Collectors.joining(" "));
            return String.format("[%s: %s]", headerKey, headerValues);
        }).collect(joining(", "));
    }

    /**
     * Parses error details based on the declared content type. The supplied HTTP status takes precedence over status fields in the body.
     *
     * @param statusCode status code of the http response.
     * @param body The error response body as a String.
     * @param contentType The content type of the error response, or {@code null} when absent.
     * @return An instance of WatsonxError parsed from the body, or an unclassified error carrying the status code when the content type is neither
     *         JSON nor XML.
     * @throws JsonException if a JSON body is malformed or its error fields have unexpected types
     */
    public static WatsonxError parseErrorBody(int statusCode, String body, String contentType) {
        String mediaType = normalizeMediaType(contentType);
        if (isMediaType(mediaType, "json")) {
            Map<?, ?> error = fromJson(body, Map.class);
            if (isNull(error))
                throw new JsonException("Expected an error JSON object");
            return parseJsonError(statusCode, error);
        }

        if (isMediaType(mediaType, "xml"))
            return parseXmlError(statusCode, body);

        return new WatsonxError(statusCode, "", List.of(new Error(Code.UNCLASSIFIED.value(), body, null)));
    }

    private static String normalizeMediaType(String contentType) {
        if (isNull(contentType))
            return "";
        int parameters = contentType.indexOf(';');
        return (parameters < 0 ? contentType : contentType.substring(0, parameters)).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isMediaType(String mediaType, String subtype) {
        int separator = mediaType.indexOf('/');
        if (separator <= 0 || separator == mediaType.length() - 1)
            return false;
        String actualSubtype = mediaType.substring(separator + 1);
        return actualSubtype.equals(subtype) || actualSubtype.endsWith("+" + subtype);
    }

    private static WatsonxError parseJsonError(int statusCode, Map<?, ?> body) {
        String trace = stringField(body, "trace");
        if (body.containsKey("errors")) {
            Object errors = body.get("errors");
            if (isNull(errors))
                return new WatsonxError(statusCode, trace, null);
            if (!(errors instanceof List<?> items))
                throw new JsonException("Expected an error array");
            List<Error> details = new ArrayList<>(items.size());
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> error))
                    throw new JsonException("Expected an error object in the error array");
                details.add(new Error(stringField(error, "code"), stringField(error, "message"), stringField(error, "more_info")));
            }
            return new WatsonxError(statusCode, trace, details);
        }

        if (body.containsKey("errorCode"))
            return parseIAMError(statusCode, body);

        if (body.get("code") instanceof Number)
            return parseToolError(statusCode, body);

        if (body.get("error") instanceof Map<?, ?> error)
            return parseGatewayError(statusCode, trace, error);

        if (body.size() == 1 && body.get("error") instanceof String message)
            return new WatsonxError(statusCode, "", List.of(new Error(Code.SCHEMA_EVENT_DOES_NOT_EXIST.value(), message, "")));

        if (nonNull(body.get("error")))
            throw new JsonException("Expected a nested error object");

        if (body.containsKey("code"))
            return parseToolError(statusCode, body);

        return new WatsonxError(statusCode, trace, null);
    }

    private static String stringField(Map<?, ?> object, String field) {
        Object value = object.get(field);
        if (isNull(value) || value instanceof String)
            return (String) value;
        throw new JsonException("Expected a string for error field " + field);
    }

    /**
     * Maps a generic {@link WatsonxException} to a more specific exception based on the {@link WatsonxError.Code}.
     *
     * @param exception the generic {@link WatsonxException} to map
     * @return the mapped specific {@link WatsonxException}, or the original exception if it cannot be classified
     */
    public static WatsonxException mapWatsonxException(WatsonxException exception) {
        var details = exception.details().orElse(null);
        if (isNull(details) || isNull(details.errors()) || details.errors().isEmpty())
            return exception;

        Error first = details.errors().get(0);

        if (isNull(first) || isNull(first.code()))
            return exception;

        Code code;

        try {
            code = Code.valueOf(first.code().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return exception;
        }

        return switch(code) {
            case AUTHENTICATION_TOKEN_EXPIRED -> new AuthenticationTokenExpiredException(exception);
            case AUTHORIZATION_REJECTED -> new AuthorizationRejectedException(exception);
            case INVALID_INPUT_ARGUMENT -> new InvalidInputArgumentException(exception);
            case INVALID_REQUEST_ENTITY -> new InvalidRequestEntityException(exception);
            case JSON_TYPE_ERROR -> new JsonTypeErrorException(exception);
            case JSON_VALIDATION_ERROR -> new JsonValidationErrorException(exception);
            case MODEL_NOT_SUPPORTED -> new ModelNotSupportedException(exception);
            case MODEL_NO_SUPPORT_FOR_FUNCTION -> new ModelNoSupportForFunctionException(exception);
            case TOKEN_QUOTA_REACHED -> new TokenQuotaReachedException(exception);
            case USER_AUTHORIZATION_FAILED -> new UserAuthorizationFailedException(exception);
            case DOWNSTREAM_REQUEST_FAILED, COS_ACCESS_DENIED, COS_FILE_NOT_FOUND, SCHEMA_EVENT_DOES_NOT_EXIST,
                TEXT_CLASSIFICATION_EVENT_DOES_NOT_EXIST,
                TEXT_EXTRACTION_EVENT_DOES_NOT_EXIST, UNCLASSIFIED -> exception;
        };
    }

    /**
     * Parses a nested Model Gateway or File Service error into a {@link WatsonxError} with a standardized format.
     *
     * @param statusCode the HTTP status code
     * @param trace the top-level trace, if available
     * @param error the deserialized {@code "error"} object from the response body
     * @return An instance of {@link WatsonxError} parsed from the body.
     */
    private static WatsonxError parseGatewayError(int statusCode, String trace, Map<?, ?> error) {
        return new WatsonxError(
            statusCode,
            nonNull(trace) ? trace : stringField(error, "request_id"),
            List.of(new Error(stringField(error, "code"), stringField(error, "message"), null)));
    }

    /**
     * Parses an error response from the IAM Authentication API into a {@link WatsonxError} with a standardized format.
     *
     * @param body the raw JSON error response as a String.
     * @return An instance of WatsonxError parsed from the body.
     */
    private static WatsonxError parseIAMError(Integer statusCode, Map<?, ?> body) {

        var errorCode = stringField(body, "errorCode");
        var errorMessage = stringField(body, "errorMessage");
        var errorDetails = stringField(body, "errorDetails");

        Error error = new Error(errorCode, errorMessage, errorDetails);
        return new WatsonxError(statusCode, "", List.of(error));
    }

    /**
     * Parses an error response from the beta Agent Tool APIs into a {@link WatsonxError} with a standardized format.
     *
     * @param body the raw JSON error response as a String.
     * @return An instance of WatsonxError parsed from the body.
     */
    private static WatsonxError parseToolError(int statusCode, Map<?, ?> body) {
        if (nonNull(body.get("code")) && !(body.get("code") instanceof Number))
            throw new JsonException("Expected a numeric Tool API error code");

        var message = stringField(body, "message");
        var description = stringField(body, "description");

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
    private static WatsonxError parseXmlError(int statusCode, String body) {
        Document doc = XmlUtils.parse(body);

        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();

        String codeStr = getTextContent(root, "Code");
        String message = getTextContent(root, "Message");
        String resource = getTextContent(root, "Resource");
        String requestId = getTextContent(root, "RequestId");
        Error error = new Error(codeStr, message, resource);
        return new WatsonxError(statusCode, requestId, List.of(error));
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list == null || list.getLength() == 0)
            return null;
        return list.item(0).getTextContent();
    }
}
