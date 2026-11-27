/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.exception.AuthenticationTokenExpiredException;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisabledInNativeImage
public class HttpUtilsTest {

    @Test
    void should_encode_resource_identifiers_as_single_path_segments() {
        assertEquals("model-v1_2.~", HttpUtils.encodePathSegment("model-v1_2.~"));
        assertEquals("a%2Fb%20c%3Fd%23e%25f%2Bg%2Ah", HttpUtils.encodePathSegment("a/b c?d#e%f+g*h"));
        assertEquals("%C3%A0%E6%BC%A2", HttpUtils.encodePathSegment("à漢"));
    }

    @Test
    void should_reject_identifiers_that_are_dot_segments() {
        assertThrows(IllegalArgumentException.class, () -> HttpUtils.encodePathSegment("."));
        assertThrows(IllegalArgumentException.class, () -> HttpUtils.encodePathSegment(".."));
    }

    @Test
    void should_preserve_dots_in_resource_names() {
        assertEquals(".hidden", HttpUtils.encodePathSegment(".hidden"));
        assertEquals("file..json", HttpUtils.encodePathSegment("file..json"));
        assertEquals("...", HttpUtils.encodePathSegment("..."));
        assertEquals("..%2Ffile.json", HttpUtils.encodePathSegment("../file.json"));
    }

    @Test
    void should_extract_body_as_string_from_string() {
        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn("test string body");
        String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
        assertEquals("test string body", result);
    }

    @Test
    void should_extract_body_as_string_from_byte_array() {
        HttpResponse<byte[]> mockHttpResponse = mock(HttpResponse.class);
        byte[] bytes = "test byte array body".getBytes(StandardCharsets.UTF_8);
        when(mockHttpResponse.body()).thenReturn(bytes);
        String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
        assertEquals("test byte array body", result);
    }

    @Test
    void should_extract_body_as_string_from_input_stream() {
        HttpResponse<InputStream> mockHttpResponse = mock(HttpResponse.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test input stream body".getBytes(StandardCharsets.UTF_8));
        when(mockHttpResponse.body()).thenReturn(inputStream);
        String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
        assertEquals("test input stream body", result);
    }

    @Test
    void should_extract_body_as_string_from_stream() {
        HttpResponse<Stream<String>> mockHttpResponse = mock(HttpResponse.class);
        Stream<String> stream = Stream.of("test", "stream", "body");
        when(mockHttpResponse.body()).thenReturn(stream);
        String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
        assertEquals("test\nstream\nbody", result);
    }

    @Test
    void should_extract_body_as_string_from_path(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "prefix-", ".txt");
        Files.write(tempFile, "test path body".getBytes(StandardCharsets.UTF_8));
        HttpResponse<Path> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn(tempFile);
        String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
        assertEquals("test path body", result);
    }

    @Test
    void should_return_empty_string_when_formatting_empty_headers_in_one_line() {
        Map<String, List<String>> headers = Map.of();
        String result = HttpUtils.inOneLine(headers);
        assertEquals("", result);
    }

    @Test
    void should_format_single_header_correctly_in_one_line() {
        Map<String, List<String>> headers = Map.of("Test-Header", Arrays.asList("Value"));
        String result = HttpUtils.inOneLine(headers);
        assertEquals("[Test-Header: Value]", result);
    }

    @Test
    void should_format_multiple_headers_correctly_in_one_line() {
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Test-Header-1", Arrays.asList("Value1"));
        headers.put("Test-Header-2", Arrays.asList("Value2"));
        String result = HttpUtils.inOneLine(headers);
        assertEquals("[Test-Header-1: Value1], [Test-Header-2: Value2]", result);
    }

    @Test
    void should_mask_authorization_header_value_when_formatting_in_one_line() {
        Map<String, List<String>> headers = Map.of("Authorization", Arrays.asList("Bearer abc123def456"));
        String result = HttpUtils.inOneLine(headers);
        assertEquals("[Authorization: ***]", result);

        headers = Map.of("Authorization", Arrays.asList("ZenApiKey abc123def456"));
        result = HttpUtils.inOneLine(headers);
        assertEquals("[Authorization: ***]", result);
    }

    @Test
    void should_mask_authorization_header_for_various_token_shapes() {
        assertEquals("[Authorization: ***]",
            HttpUtils.inOneLine(Map.of("Authorization", List.of("Bearer short"))));

        assertEquals("[Authorization: ***]",
            HttpUtils.inOneLine(Map.of("Authorization", List.of("abcdef123456"))));

        assertEquals("[Authorization: ***]",
            HttpUtils.inOneLine(Map.of("Authorization", List.of("Basic ABCD++++////WXYZ"))));

        assertEquals("[Authorization: ***]",
            HttpUtils.inOneLine(Map.of("Authorization", List.of("Bearer header.payload.signature"))));
    }

    @Test
    void should_parse_error_body_correctly_from_json() {
        WatsonxError watsonxError = new WatsonxError(400, "Internal Server Error", List.of(new Error("ERROR", "error", "error")));
        String jsonBody = Json.toJson(watsonxError);
        WatsonxError result = HttpUtils.parseErrorBody(400, jsonBody, "application/json");
        JSONAssert.assertEquals(jsonBody, Json.toJson(result), true);
    }

    @Test
    void should_parse_standard_json_error_without_trace() {
        String body = """
            {"errors":[{"code":"some_error","message":"boom","more_info":"info"}],"status_code":400}""";
        WatsonxError result = HttpUtils.parseErrorBody(400, body, "application/json");
        assertEquals(1, result.errors().size());
        assertEquals("some_error", result.errors().get(0).code());
        assertEquals("boom", result.errors().get(0).message());
    }

    @Test
    void should_parse_error_body_correctly_from_xml() {
        String xmlBody = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Error>
                <Code>NoSuchBucket</Code>
                <Message>The specified bucket does not exist.</Message>
                <Resource>/my-bucket-name/test.pdf</Resource>
                <RequestId>my-request-id</RequestId>
                <httpStatusCode>404</httpStatusCode>
            </Error>""";
        Error error = new WatsonxError.Error("NoSuchBucket", "The specified bucket does not exist.", "/my-bucket-name/test.pdf");
        WatsonxError expected = new WatsonxError(404, "my-request-id", List.of(error));
        WatsonxError result = HttpUtils.parseErrorBody(404, xmlBody, "application/xml");
        assertEquals(expected, result);
    }

    @Test
    void should_parse_iam_error_body_correctly_from_json() {
        String jsonBody = """
            {
                "errorCode": "BXNIM0415E",
                "errorMessage": "Provided API key could not be found",
                "errorDetails": "The API key provided in the request could not be found"
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(401, jsonBody, "application/json");
        assertEquals(401, result.statusCode());
        assertEquals(1, result.errors().size());
        assertEquals("BXNIM0415E", result.errors().get(0).code());
        assertEquals("Provided API key could not be found", result.errors().get(0).message());
        assertEquals("The API key provided in the request could not be found", result.errors().get(0).moreInfo());
    }

    @Test
    void should_parse_iam_error_body_without_error_details_correctly_from_json() {
        String jsonBody = """
            {
                "errorCode": "BXNIM0415E",
                "errorMessage": "Provided API key could not be found"
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(401, jsonBody, "application/json");
        assertEquals(401, result.statusCode());
        assertEquals(1, result.errors().size());
        assertEquals("BXNIM0415E", result.errors().get(0).code());
        assertEquals("Provided API key could not be found", result.errors().get(0).message());
        assertEquals(null, result.errors().get(0).moreInfo());
    }

    @Test
    void should_parse_tool_error_body_with_expired_jwt_correctly_from_json() {
        String jsonBody = """
            {
                "code": 401,
                "error": "Unauthorized",
                "reason": "Unauthorized",
                "message": "Unauthorized",
                "description": "jwt expired"
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(401, jsonBody, "application/json");
        assertEquals(401, result.statusCode());
        assertEquals(1, result.errors().size());
        assertEquals(WatsonxError.Code.AUTHENTICATION_TOKEN_EXPIRED.value(), result.errors().get(0).code());
        assertEquals("Unauthorized", result.errors().get(0).message());
        assertEquals("jwt expired", result.errors().get(0).moreInfo());
    }

    @Test
    void should_parse_tool_error_body_correctly_from_json() {
        String jsonBody = """
            {
                "code": 403,
                "message": "Forbidden",
                "description": "Access denied"
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(403, jsonBody, "application/json");
        assertEquals(403, result.statusCode());
        assertEquals(1, result.errors().size());
        assertEquals(WatsonxError.Code.UNCLASSIFIED.value(), result.errors().get(0).code());
        assertEquals("Forbidden", result.errors().get(0).message());
        assertEquals("Access denied", result.errors().get(0).moreInfo());
    }

    @Test
    void should_parse_file_service_error_body_correctly_from_json() {
        String jsonBody = """
            {
                "message": "401 Failed to authenticate the request due to an expired token",
                "status": 401,
                "error": {
                    "message": "Failed to authenticate the request due to an expired token",
                    "type": "authentication_error",
                    "param": null,
                    "code": "authentication_token_expired"
                },
                "code": "authentication_token_expired",
                "param": null,
                "type": "authentication_error",
                "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#upload_file",
                "trace": "c8d777a8fe6551b6069a8cef52c52956"
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(401, jsonBody, "application/json");
        assertEquals(401, result.statusCode());
        assertEquals("c8d777a8fe6551b6069a8cef52c52956", result.trace());
        assertEquals(1, result.errors().size());
        assertEquals("authentication_token_expired", result.errors().get(0).code());
        assertEquals("Failed to authenticate the request due to an expired token", result.errors().get(0).message());
    }

    @Test
    void should_parse_model_gateway_error_body_correctly_from_json() {
        String jsonBody = """
            {
                "error": {
                    "code": "model_not_found",
                    "message": "The model `foo` does not exist.",
                    "request_id": "req-12345"
                }
            }""";

        WatsonxError result = HttpUtils.parseErrorBody(404, jsonBody, "application/json");
        assertEquals(404, result.statusCode());
        assertEquals("req-12345", result.trace());
        assertEquals(1, result.errors().size());
        assertEquals("model_not_found", result.errors().get(0).code());
        assertEquals("The model `foo` does not exist.", result.errors().get(0).message());
        assertEquals(null, result.errors().get(0).moreInfo());
    }

    @Test
    void should_parse_error_body_as_unclassified_when_content_type_is_not_supported() {
        WatsonxError result = HttpUtils.parseErrorBody(500, "test body", "text/plain");
        assertEquals(500, result.statusCode());
        assertEquals(1, result.errors().size());
        assertEquals(WatsonxError.Code.UNCLASSIFIED.value(), result.errors().get(0).code());
        assertEquals("test body", result.errors().get(0).message());
        assertEquals(null, result.errors().get(0).moreInfo());
    }

    @Test
    void should_keep_status_code_when_gateway_returns_plain_text_error() {
        WatsonxError result = HttpUtils.parseErrorBody(502, "error code: 502", "text/plain");
        assertEquals(502, result.statusCode());
        assertEquals("error code: 502", result.errors().get(0).message());

        var ex = HttpUtils.mapWatsonxException(new WatsonxException("error code: 502", 502, result));
        assertEquals(502, ex.statusCode());
    }

    @Test
    void should_return_original_exception_when_mapping_with_no_details_or_empty_errors() {
        var withoutDetails = new WatsonxException("no_details", 500, null);
        assertSame(withoutDetails, HttpUtils.mapWatsonxException(withoutDetails));
        var withoutErrors = new WatsonxException("no_errors", 500, new WatsonxError(500, "", null));
        assertSame(withoutErrors, HttpUtils.mapWatsonxException(withoutErrors));
        var emptyErrors = new WatsonxException("empty_errors", 500, new WatsonxError(500, "", List.of()));
        assertSame(emptyErrors, HttpUtils.mapWatsonxException(emptyErrors));
    }

    @Test
    void should_preserve_downstream_request_failure() {
        String body = """
            {"errors":[{"code":"downstream_request_failed","message":"context deadline exceeded"}]}
            """;
        var details = HttpUtils.parseErrorBody(500, body, "application/json");
        var exception = new WatsonxException(body, 500, details);

        assertSame(exception, HttpUtils.mapWatsonxException(exception));
        assertEquals(500, exception.statusCode());
        assertEquals(WatsonxError.Code.DOWNSTREAM_REQUEST_FAILED.value(), details.errors().get(0).code());
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/json; charset=utf-8", "Application/JSON", " application/problem+json ; charset=UTF-8",
            "application/vnd.ibm.error+json" })
    void should_recognize_json_media_types(String contentType) {
        String body = """
            {"errors":[{"code":"invalid_input_argument","message":"invalid input"}]}""";
        var details = HttpUtils.parseErrorBody(400, body, contentType);
        var exception = HttpUtils.mapWatsonxException(new WatsonxException(body, 400, details));
        assertInstanceOf(InvalidInputArgumentException.class, exception);
        assertEquals(400, details.statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/xml; charset=utf-8", "Application/XML", "application/vnd.ibm.error+xml", "text/xml" })
    void should_use_http_status_for_xml_without_body_status(String contentType) {
        String body = "<Error><Code>AccessDenied</Code><Message>Access denied</Message><RequestId>request-id</RequestId></Error>";
        var details = HttpUtils.parseErrorBody(403, body, contentType);
        assertEquals(403, details.statusCode());
        assertEquals("request-id", details.trace());
        assertEquals("AccessDenied", details.errors().get(0).code());
    }

    @Test
    void should_ignore_conflicting_or_invalid_xml_status() {
        for (String bodyStatus : List.of("200", "not-a-status")) {
            var details = HttpUtils.parseErrorBody(403,
                "<Error><Code>AccessDenied</Code><httpStatusCode>" + bodyStatus + "</httpStatusCode></Error>", "application/xml");
            assertEquals(403, details.statusCode());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status_code\":200,\"errors\":[{\"code\":\"invalid_input_argument\"}]}",
            "{\"status\":200,\"trace\":\"trace\",\"error\":{\"code\":\"invalid_input_argument\"}}",
            "{\"code\":200,\"message\":\"error\",\"description\":\"details\"}",
            "{\"errorCode\":\"error\",\"errorMessage\":\"message\"}",
            "{\"error\":{\"code\":\"error\",\"request_id\":\"request-id\"}}" })
    void should_use_http_status_for_all_json_error_formats(String body) {
        assertEquals(403, HttpUtils.parseErrorBody(403, body, "application/json").statusCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "text/plain", "text/html", "application/jsonx", "unknown" })
    void should_keep_body_unclassified_for_unknown_or_missing_media_type(String contentType) {
        String body = "{\"errors\":[{\"code\":\"authentication_token_expired\"}]}";
        var details = HttpUtils.parseErrorBody(403, body, contentType);
        assertEquals(403, details.statusCode());
        assertEquals(WatsonxError.Code.UNCLASSIFIED.value(), details.errors().get(0).code());
        assertEquals(body, details.errors().get(0).message());
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "[]", "42", "\"error\"", "{\"error\":", "{\"errors\":{}}", "{\"errors\":[null]}",
            "{\"errors\":[42]}", "{\"errors\":[{\"code\":7}]}", "{\"errors\":[{\"message\":[]}]}", "{\"trace\":{}}",
            "{\"error\":false}", "{\"error\":{\"code\":{}}}", "{\"errorCode\":5}", "{\"code\":{},\"description\":\"jwt expired\"}" })
    void should_report_unexpected_json_shapes_as_parsing_failures(String body) {
        assertThrows(JsonException.class, () -> HttpUtils.parseErrorBody(400, body, "application/json"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"errors\":null}", "{\"trace\":null}", "{\"unexpected\":{\"nested\":true}}" })
    void should_accept_json_objects_without_error_details(String body) {
        var details = HttpUtils.parseErrorBody(403, body, "application/json");
        assertEquals(403, details.statusCode());
        assertNull(details.errors());
        var original = new WatsonxException(body, 403, details);
        assertSame(original, HttpUtils.mapWatsonxException(original));
    }

    @Test
    void should_map_error_codes_independently_of_default_locale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var expired = new WatsonxException("expired", 401,
                new WatsonxError(401, null, List.of(new Error("authentication_token_expired", "expired", null))));
            assertInstanceOf(AuthenticationTokenExpiredException.class, HttpUtils.mapWatsonxException(expired));
            var invalid = new WatsonxException("invalid", 400,
                new WatsonxError(400, null, List.of(new Error("invalid_input_argument", "invalid", null))));
            assertInstanceOf(InvalidInputArgumentException.class, HttpUtils.mapWatsonxException(invalid));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void should_preserve_first_error_mapping_precedence() {
        var errors = List.of(new Error("unknown_future_error", "first", null), new Error("authentication_token_expired", "second", null));
        var exception = new WatsonxException("multiple errors", 401, new WatsonxError(401, null, errors));
        assertSame(exception, HttpUtils.mapWatsonxException(exception));
        var missingCode = new WatsonxException("missing code", 403,
            new WatsonxError(403, null, List.of(new Error(null, "first", null), errors.get(1))));
        assertSame(missingCode, HttpUtils.mapWatsonxException(missingCode));
    }

    @Test
    void should_throw_runtime_exception_when_extracting_body_from_unsupported_type() {
        HttpResponse<Integer> resp = mock(HttpResponse.class);
        when(resp.body()).thenReturn(42);
        assertThrows(RuntimeException.class, () -> HttpUtils.extractBodyAsString(resp));
    }
}
