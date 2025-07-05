/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.stream.Stream;

// Assisted by watsonx Code Assistant

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError.Error;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class HttpUtilsTest {

  @Test
  void test_extract_body_as_string_with_string() {
    HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.body()).thenReturn("test string body");
    String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
    assertEquals("test string body", result);
  }

  @Test
  void test_extract_body_as_string_with_byte_array() {
    HttpResponse<byte[]> mockHttpResponse = mock(HttpResponse.class);
    byte[] bytes = "test byte array body".getBytes(StandardCharsets.UTF_8);
    when(mockHttpResponse.body()).thenReturn(bytes);
    String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
    assertEquals("test byte array body", result);
  }

  @Test
  void test_extract_body_as_string_with_input_stream() {
    HttpResponse<InputStream> mockHttpResponse = mock(HttpResponse.class);
    ByteArrayInputStream inputStream = new ByteArrayInputStream("test input stream body".getBytes(StandardCharsets.UTF_8));
    when(mockHttpResponse.body()).thenReturn(inputStream);
    String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
    assertEquals("test input stream body", result);
  }

  @Test
  void test_extract_body_as_string_with_stream() {
    HttpResponse<Stream<String>> mockHttpResponse = mock(HttpResponse.class);
    Stream<String> stream = Stream.of("test", "stream", "body");
    when(mockHttpResponse.body()).thenReturn(stream);
    String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
    assertEquals("test\nstream\nbody", result);
  }

  @Test
  void test_extract_body_as_string_with_path(@TempDir Path tempDir) throws IOException {
    Path tempFile = Files.createTempFile(tempDir, "prefix-", ".txt");
    Files.write(tempFile, "test path body".getBytes(StandardCharsets.UTF_8));
    HttpResponse<Path> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.body()).thenReturn(tempFile);
    String result = HttpUtils.extractBodyAsString(mockHttpResponse).get();
    assertEquals("test path body", result);
  }

  @Test
  void test_in_one_line_empty_headers() {
    Map<String, List<String>> headers = Map.of();
    String result = HttpUtils.inOneLine(headers);
    assertEquals("", result);
  }

  @Test
  void test_in_one_line_single_header() {
    Map<String, List<String>> headers = Map.of("Test-Header", Arrays.asList("Value"));
    String result = HttpUtils.inOneLine(headers);
    assertEquals("[Test-Header: Value]", result);
  }

  @Test
  void test_in_one_line_multiple_headers() {
    LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
    headers.put("Test-Header-1", Arrays.asList("Value1"));
    headers.put("Test-Header-2", Arrays.asList("Value2"));
    String result = HttpUtils.inOneLine(headers);
    assertEquals("[Test-Header-1: Value1], [Test-Header-2: Value2]", result);
  }

  @Test
  void test_in_one_line_mask_authorization_header_value() {
    Map<String, List<String>> headers = Map.of("Authorization", Arrays.asList("Bearer abc123def456"));
    String result = HttpUtils.inOneLine(headers);
    assertEquals("[Authorization: Bearer abc1...f456]", result);
  }

  @Test
  void test_parse_error_body_json() {
    WatsonxError watsonxError = new WatsonxError(400, "Internal Server Error", List.of(new Error("ERROR", "error", "error")));
    String jsonBody = Json.toJson(watsonxError);
    WatsonxError result = HttpUtils.parseErrorBody(jsonBody, "application/json");
    JSONAssert.assertEquals(jsonBody, Json.toJson(result), true);
  }

  @Test
  void test_parse_error_body_xml() {
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
    WatsonxError result = HttpUtils.parseErrorBody(xmlBody, "application/xml");
    assertEquals(expected, result);
  }

  @Test
  void testParseErrorBody_UnsupportedContentType() {
    assertThrows(RuntimeException.class, () -> {
      HttpUtils.parseErrorBody("test body", "text/plain");
    });
  }
}