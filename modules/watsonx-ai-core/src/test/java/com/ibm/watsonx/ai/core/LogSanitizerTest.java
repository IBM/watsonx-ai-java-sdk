/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;

class LogSanitizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "Authorization", "authorization", "AUTHORIZATION", "aUtHoRiZaTiOn", "Proxy-Authorization", "Cookie", "Set-Cookie",
            "iam-token", "IAM-TOKEN", "X-Api-Key", "x-api-key" })
    void should_mask_entire_sensitive_header_values(String key) {
        List<String> values = List.of("Bearer synthetic-header-secret", "another-header-secret");
        Map<String, List<String>> input = Map.of(key, values);

        var result = LogSanitizer.headers(input);

        assertEquals(List.of("***", "***"), result.get(key));
        assertEquals(values, input.get(key));
        assertFalse(HttpUtils.inOneLine(input).contains("secret"));
    }

    @Test
    void should_copy_header_collections_and_preserve_order() {
        var values = new ArrayList<>(List.of("visible"));
        var headers = new LinkedHashMap<String, List<String>>();
        headers.put("first", values);
        headers.put("second", List.of("also-visible"));

        var result = LogSanitizer.headers(headers);
        values.add("later");
        headers.put("third", List.of("later"));

        assertEquals(List.of("first", "second"), new ArrayList<>(result.keySet()));
        assertEquals(List.of("visible"), result.get("first"));
        assertThrows(UnsupportedOperationException.class, () -> result.put("added", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> result.get("first").add("added"));
    }

    @Test
    void should_create_sanitized_http_headers() {
        HttpHeaders input = HttpHeaders.of(Map.of("Set-Cookie", List.of("session=synthetic-cookie"), "Content-Type", List.of("application/json")),
            (key, value) -> true);

        HttpHeaders result = LogSanitizer.headers(input);

        assertEquals("***", result.firstValue("set-cookie").orElseThrow());
        assertEquals("application/json", result.firstValue("Content-Type").orElseThrow());
        assertEquals("session=synthetic-cookie", input.firstValue("Set-Cookie").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = { "api_key", "api-key", "apiKey", "APIKEY", "password", "passWord", "secret", "client_secret", "clientSecret",
            "secret_access_key", "secretAccessKey", "access_token", "accessToken", "refresh_token", "refreshToken", "id_token", "token", "iam_token",
            "credentials", "authorization", "cookie", "user_access_token", "google_access_token", "my_refresh_token" })
    void should_mask_sensitive_body_aliases_and_encoded_form_keys(String key) {
        String json = "{\"" + key + "\":\"synthetic-body-secret\",\"visible\":42}";
        String masked = LogSanitizer.body(json);
        JSONAssert.assertEquals("{\"" + key + "\":\"***\",\"visible\":42}", masked, true);
        assertEquals(key + "=***&visible=42", LogSanitizer.body(key + "=synthetic-body-secret&visible=42"));
    }

    @Test
    void should_sanitize_escaped_json_keys_nested_objects_arrays_and_credential_maps() {
        var cases = Map.of("""
            {"items":[{"pass\\u0077ord":"synthetic-escaped\\\"secret"},{"accessToken":["synthetic-array-secret"]}],
             "credentials":{"unknown_vendor_key":"synthetic-map-secret"},"visible":"hello"}
            """, """
            {"items":[{"password":"***"},{"accessToken":"***"}],"credentials":"***","visible":"hello"}
            """, """
            {
              "nested": {"\\u0061pi_key": "synthetic-key-12345", "password": "synthetic-escaped-\\\"value\\\"\\\\tail"},
              "items": [{"access_token": "synthetic-token-12345"}, "Bearer synthetic-bearer-12345", {"visible": "quoted \\\"value\\\""}],
              "api_keys": ["synthetic-array-12345"],
              "credentials": {"arbitrary_field": "synthetic-credential-12345"},
              "visible": "kept"
            }
            """, """
            {
              "nested": {"api_key": "***", "password": "***"},
              "items": [{"access_token": "***"}, "Bearer ***", {"visible": "quoted \\\"value\\\""}],
              "api_keys": "***",
              "credentials": "***",
              "visible": "kept"
            }
            """);

        for (var entry : cases.entrySet()) {
            String input = entry.getKey();
            String result = LogSanitizer.body(input);

            JSONAssert.assertEquals(entry.getValue(), result, true);
            assertFalse(result.contains("synthetic"));
            assertTrue(input.contains("synthetic"));
            assertEquals(result, LogSanitizer.body(result));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "api\\_key", "pass\\word", "to\\ken", "auth\\orization" })
    void should_mask_sensitive_json_keys_containing_literal_backslashes(String key) {
        String input = Json.toJson(Map.of(key, "synthetic-obfuscated-secret"));
        assertEquals(Json.toJson(Map.of(key, "***")), LogSanitizer.body(input));
        String diagnostic = "Error \"" + key + "\":synthetic-obfuscated-secret";
        assertEquals("Error \"" + key + "\":***", LogSanitizer.body(diagnostic));
    }

    @ParameterizedTest
    @ValueSource(strings = { "api\\_key", "pass\\word" })
    void should_omit_malformed_json_with_unknown_escapes_in_sensitive_keys(String key) {
        assertEquals("[body omitted: unable to sanitize]", LogSanitizer.body("{\"" + key + "\":\"synthetic-obfuscated-secret\"}"));
    }

    @Test
    void should_mask_backslash_obfuscated_query_names_and_preserve_ordinary_json_keys() {
        assertEquals(URI.create("https://example.com/?api%5C_key=***&visible=ok"),
            LogSanitizer.uri(URI.create("https://example.com/?api%5C_key=synthetic-obfuscated-secret&visible=ok")));
        String ordinary = Json.toJson(Map.of("folder\\name", "ordinary value"));
        assertEquals(ordinary, LogSanitizer.body(ordinary));
    }

    @ParameterizedTest
    @ValueSource(strings = { "pass\\word.cookie", "pass\\word.set-cookie", "pass\\word.authorization", "coo\\kie", "set-coo\\kie",
            "auth\\orization", "prefix.coo\\kie" })
    void should_mask_entire_sensitive_header_lines_with_backslashes_and_dotted_prefixes(String key) {
        String input = "Error \"" + key + "\":session=synthetic-first-secret; other=synthetic-second-secret\nvisible";
        assertEquals("Error \"" + key + "\":***\nvisible", LogSanitizer.body(input));
        String json = Json.toJson(Map.of("message", input));
        assertEquals(Json.toJson(Map.of("message", "Error \"" + key + "\":***\nvisible")), LogSanitizer.body(json));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Cookie", "Set-Cookie", "Authorization", "Proxy-Authorization" })
    void should_mask_entire_header_lines_after_obfuscated_unquoted_prefixes(String header) {
        String prefix = "Error pass%5Cword " + header + ":";
        String input = prefix + "session=synthetic-first-secret; other=synthetic-second-secret\nvisible";
        assertEquals(prefix + "***\nvisible", LogSanitizer.body(input));
        assertEquals(Json.toJson(Map.of("message", prefix + "***\nvisible")), LogSanitizer.body(Json.toJson(Map.of("message", input))));
    }

    @Test
    void should_mask_encoded_form_keys_and_quoted_values_in_error_messages() {
        assertEquals("api%5Fkey=***&scope=openid", LogSanitizer.body("api%5Fkey=synthetic-encoded-secret&scope=openid"));
        assertEquals("api+key=***&scope=openid", LogSanitizer.body("api+key=synthetic-encoded-secret&scope=openid"));
        assertEquals("api%20key=***&scope=openid", LogSanitizer.body("api%20key=synthetic-encoded-secret&scope=openid"));
        assertEquals("Invalid password=***", LogSanitizer.body("Invalid password='synthetic secret with spaces'"));
        assertEquals("Invalid Bearer ***", LogSanitizer.body("Invalid Bearer synthetic-bearer-secret"));
        assertEquals("Authorization: ***", LogSanitizer.body("Authorization: Bearer synthetic-header-secret"));
        assertEquals("access_token: ***", LogSanitizer.body("access_token: Bearer synthetic-token-secret"));
        assertEquals("Failed: password=***&apikey=***",
            LogSanitizer.body("Failed: password=synthetic-password-secret&apikey=synthetic-api-secret"));
        assertEquals("com.ibm.watsonx.ai.core.exception.WatsonxException: password=***&apikey=***",
            LogSanitizer.body("com.ibm.watsonx.ai.core.exception.WatsonxException: password=synthetic-password-secret&apikey=synthetic-api-secret"));
    }

    @Test
    void should_sanitize_json_embedded_in_messages_and_encoded_json_strings() {
        String input = "Error payload: {\"password\":\"synthetic-message-secret\",\"visible\":\"ok\"}";
        String result = LogSanitizer.body(input);
        assertFalse(result.contains("synthetic"));
        assertTrue(result.startsWith("Error payload: "));
        String encoded = Json.toJson("{\"password\":\"synthetic-encoded-secret\"}");
        String sanitizedEncoded = LogSanitizer.body(encoded);
        assertFalse(sanitizedEncoded.contains("synthetic"));
        JSONAssert.assertEquals("{\"password\":\"***\"}", Json.fromJson(sanitizedEncoded, String.class), true);
        assertEquals("credentials=***", LogSanitizer.body("credentials={\"vendor\":\"synthetic-vendor-secret\"}"));
        assertEquals("password: ***", LogSanitizer.body("password: [\"synthetic-array-secret\"]"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Failed: \"password\":\"synthetic-fragment-secret\"", "Failed: 'api_key':'synthetic-fragment-secret'",
            "Invalid \"api key\":\"synthetic-fragment-secret\"", "Invalid 'secret access key':'synthetic-fragment-secret'",
            "Failed: \"credentials\": {\"vendor\":\"synthetic-fragment-secret\"}",
            "Failed: 'credentials': [\"synthetic-fragment-secret\"]" })
    void should_mask_quoted_sensitive_keys_in_diagnostic_fragments(String input) {
        String sanitized = LogSanitizer.body(input);
        assertFalse(sanitized.contains("synthetic"));
        assertTrue(sanitized.contains("***"));
        assertEquals(sanitized, LogSanitizer.body(sanitized));
    }

    @Test
    void should_mask_quoted_sensitive_keys_inside_json_message_strings() {
        String input = Json.toJson(Map.of("message", "Invalid \"api key\":\"synthetic-message-secret\""));
        String sanitized = LogSanitizer.body(input);
        assertFalse(sanitized.contains("synthetic"));
        JSONAssert.assertEquals(Json.toJson(Map.of("message", "Invalid \"api key\":***")), sanitized, true);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"password\":\"synthetic-malformed-secret", "[{\"token\":\"synthetic-malformed-secret\"}",
            "{\"password\":\"synthetic-password-12345\"", "[{\"user_access_token\":\"synthetic-secret\"}] extra",
            "Error: {\"password\":\"synthetic-malformed-secret", "{\"password\":\"synthetic-malformed-secret\"} trailing",
            "data:{password:synthetic-malformed-secret}",
            "failed: {password:synthetic-malformed-secret}" })
    void should_omit_input_when_sanitization_cannot_parse_it(String input) {
        String result = LogSanitizer.body(input);
        assertEquals("[body omitted: unable to sanitize]", result);
        assertFalse(result.contains("synthetic"));
        assertEquals(result, LogSanitizer.body(result));
    }

    @Test
    void should_truncate_media_and_keep_sanitization_idempotent() {
        String input = """
            {"image_url":"data:image/png;base64,AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBB", "input_audio":{"data":"AAAAAAAAAAAAAAAABBBBBBBBBBBBBBB"}}
            """;
        String result = LogSanitizer.body(input);
        JSONAssert.assertEquals("""
            {"image_url":"data:image/png;base64,AAAAAAAAAAAAAAA...", "input_audio":{"data":"AAAAAAAAAAAAAAA..."}}
            """, result, true);
        assertEquals(result, LogSanitizer.body(result));
        assertEquals("data:image/png;base64,AAAAAAAAAAAAAAA...",
            LogSanitizer.body("data:image/png;base64,AAAAAAAAAAAAAAAABBBBBBBBBBBBBBB"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "secret==|secret=***", "cookie==|cookie=***", "credentials==|credentials=***",
            "password=synthetic-secret|password=***", "Bearer aBcDeFgH1234|Bearer ***", "AAA+token==|AAA+token=***",
            "AAA/se+cret==|AAA/se+cret=***", "AAA/se_cret==|AAA/se_cret=***", "AAA/se-cret==|AAA/se-cret=***",
            "AAA/secret/AA==|AAA/secret/AA==", "secret==suffix|secret=***" })
    void should_sanitize_assignments_and_authentication_inside_media_fields(String value, String expected) {
        JSONAssert.assertEquals(Json.toJson(Map.of("data", expected)), LogSanitizer.body(Json.toJson(Map.of("data", value))), true);
        String uri = "data:image/png;base64," + value;
        JSONAssert.assertEquals(Json.toJson(Map.of("image_url", "data:image/png;base64," + expected)),
            LogSanitizer.body(Json.toJson(Map.of("image_url", uri))), true);
    }

    @Test
    void should_truncate_large_media_without_losing_secrets_or_surrounding_json_layout() {
        String payload = "A".repeat(1_048_576);
        for (String suffix : List.of("", "=", "==")) {
            String input = " {\"data\":\"" + payload + suffix + "\", \"password\":\"synthetic-secret\", \"visible\":1.00e+03} ";
            assertEquals(" {\"data\":\"AAAAAAAAAAAAAAA...\", \"password\":\"***\", \"visible\":1.00e+03} ", LogSanitizer.body(input));
        }
        assertEquals("{\"data\":\"AAAAAAAAAAAAAAA...\",\"visible\":true}",
            LogSanitizer.body("{\"data\":\"AAAAAAAAAAAAAA\\u0041AAAAAA\",\"visible\":true}"));
    }

    @Test
    void should_truncate_large_data_uris_with_double_padding_and_no_slash_in_the_payload() {
        String prefix = "data:image/png;base64,";
        String input = "{\"image_url\":\"" + prefix + "A".repeat(3_145_728) + "==\",\"password\":\"synthetic-secret\"}";
        assertEquals("{\"image_url\":\"" + prefix + "AAAAAAAAAAAAAAA...\",\"password\":\"***\"}", LogSanitizer.body(input));
    }

    @Test
    void should_preserve_control_characters_when_reencoding_sanitized_json_strings() {
        var controls = new StringBuilder();
        for (char character = 0; character < 32; character++)
            controls.append(character);
        String input = Json.toJson(Map.of("content", controls + " password=synthetic-secret"));
        JSONAssert.assertEquals(Json.toJson(Map.of("content", controls + " password=***")), LogSanitizer.body(input), true);
    }

    @Test
    void should_preserve_sse_prefix_and_completion_marker() {
        assertEquals("data: [DONE]", LogSanitizer.body("data: [DONE]"));
        assertEquals("[DONE]", LogSanitizer.body("[DONE]"));
        String result = LogSanitizer.body("SSE metadata omitted\ndata: {\"password\":\"synthetic-sse-secret\"}");
        assertTrue(result.startsWith("SSE metadata omitted\ndata: "));
        assertFalse(result.contains("synthetic"));
        assertEquals(result, LogSanitizer.body(result));
    }

    @Test
    void should_omit_excessively_nested_values() {
        String input = "[".repeat(50) + "\"synthetic-nested-secret\"" + "]".repeat(50);
        assertFalse(LogSanitizer.body(input).contains("synthetic"));
    }

    @Test
    void should_sanitize_uri_without_changing_non_sensitive_encoding_or_source() {
        URI source = URI
            .create("https://user:synthetic-user-secret@example.com/a%2Fb?api%5Fkey=synthetic-query-secret&visible=a%2Bb#synthetic-fragment-secret");

        URI result = LogSanitizer.uri(source);

        assertEquals("https://example.com/a%2Fb?api%5Fkey=***&visible=a%2Bb", result.toString());
        assertTrue(source.toString().contains("synthetic"));
        assertEquals(result, LogSanitizer.uri(result));
        assertEquals(URI.create("about:blank"), LogSanitizer.uri(URI.create("mailto:synthetic-secret@example.com")));
    }

    @Test
    void should_remove_raw_exception_references_causes_and_suppressed_but_preserve_code_locations() {
        var source = new IllegalArgumentException("Invalid apikey=synthetic-top-secret", new IllegalStateException("synthetic-cause-secret"));
        source.addSuppressed(new RuntimeException("synthetic-suppressed-secret"));
        source.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.example.Client", "send", "Client.java", 42) });

        Throwable result = LogSanitizer.error(source);
        var printed = new StringWriter();
        result.printStackTrace(new PrintWriter(printed));

        assertFalse(result instanceof IllegalArgumentException);
        assertEquals("IllegalArgumentException: Invalid apikey=***", result.getMessage());
        assertNull(result.getCause());
        assertEquals(0, result.getSuppressed().length);
        assertArrayEquals(source.getStackTrace(), result.getStackTrace());
        assertTrue(printed.toString().contains("com.example.Client.send(Client.java:42)"));
        assertFalse(printed.toString().contains("synthetic"));
        assertTrue(source.getMessage().contains("synthetic-top-secret"));
    }

    @Test
    void should_use_locale_independent_key_matching() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(List.of("***"), LogSanitizer.headers(Map.of("AUTHORIZATION", List.of("synthetic-secret"))).get("AUTHORIZATION"));
            assertFalse(LogSanitizer.body("{\"APIKEY\":\"synthetic-secret\"}").contains("synthetic"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void should_preserve_absent_and_ordinary_values() {
        assertNull(LogSanitizer.body(null));
        assertNull(LogSanitizer.headers((HttpHeaders) null));
        assertNull(LogSanitizer.uri(null));
        assertNull(LogSanitizer.error(null));
        assertEquals(" ", LogSanitizer.body(" "));
        assertEquals("ordinary diagnostic", LogSanitizer.body("ordinary diagnostic"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Growth was 5% year over year: strong", "[x for x in range(3)]", "[docs](url)", "[INST] prompt [/INST]",
            "{name}", "Unknown field [foo]", "Explain the basic concepts of Java", "Explain the Basic Concepts of Java",
            "Bearer Of Good News", "[1] Smith et al. (2020) citation", "{\"na", "{\"arguments\":\"{\\\"query\\\":" })
    void should_preserve_ordinary_text_and_partial_tool_arguments(String value) {
        assertEquals(value, LogSanitizer.body(value));
        String json = Json.toJson(Map.of("content", value));
        assertEquals(json, LogSanitizer.body(json));
    }

    @Test
    void should_preserve_null_values_number_spelling_property_order_and_json_spacing() {
        for (String json : List.of(" { \"finish_reason\": null, \"number\": 1.000e+03, \"items\": [null, {\"password\": \"synthetic-secret\"}] } ",
            "{ \"finish_reason\": null, \"choice\": {\"content\": null}, \"values\": [null, 1.00, 1e+2, -0], \"access_token\": \"synthetic-secret\" }"))
            assertEquals(json.replace("\"synthetic-secret\"", "\"***\""), LogSanitizer.body(json));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-0", "0.0", "-0.25", "1.000e+03", "1E-2", "123456789012345678901234567890", "true", "false", "null" })
    void should_preserve_json_literals_when_masking_adjacent_fields(String literal) {
        String input = "{\"value\":" + literal + ",\"password\":\"synthetic-secret\"}";
        assertEquals("{\"value\":" + literal + ",\"password\":\"***\"}", LogSanitizer.body(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "-", "+1", "00", "01", "-01", ".1", "1.", "1e", "1e+", "1e-", "NaN", "Infinity", "TRUE", "nul",
            "truefalse", "0x10", "1_000", "1\u000b" })
    void should_omit_malformed_json_literals_when_sensitive_fields_are_present(String literal) {
        assertEquals("[body omitted: unable to sanitize]", LogSanitizer.body("{\"value\":" + literal + ",\"password\":\"synthetic-secret\"}"));
    }

    @Test
    void should_preserve_many_sequential_citations_without_treating_them_as_nested_json() {
        String text = "[1] Smith et al. (2020) ordinary citation ".repeat(25_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(text, LogSanitizer.body(text));
            assertEquals(Json.toJson(Map.of("content", text)), LogSanitizer.body(Json.toJson(Map.of("content", text))));
        });
    }

    @Test
    void should_handle_long_quoted_values_and_plain_text_in_bounded_time() {
        String quoted = "Testo: \"" + "a".repeat(20_000) + "\"";
        String prose = "ordinary words with no punctuation ".repeat(2_000);
        String longKey = "prefix_".repeat(2_000) + "access_token=synthetic-long-key-secret";
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(quoted, LogSanitizer.body(quoted));
            assertEquals(prose, LogSanitizer.body(prose));
            assertFalse(LogSanitizer.body(longKey).contains("synthetic"));
            String json = Json.toJson(Map.of("password", "synthetic".repeat(20_000), "content", quoted));
            assertFalse(LogSanitizer.body(json).contains("synthetic"));
        });
    }

    @Test
    void should_ignore_invalid_percent_encoding_outside_known_sensitive_key_names() {
        assertEquals("api%ZZkey=ordinary-value", LogSanitizer.body("api%ZZkey=ordinary-value"));
        assertEquals("password%ZZ=***", LogSanitizer.body("password%ZZ=synthetic-invalid-key-secret"));
    }

    @Test
    void should_omit_unterminated_encoded_json_strings_with_sensitive_fields() {
        String encoded = Json.toJson("{\"password\":\"synthetic-encoded-secret\"}");
        String malformed = encoded.substring(0, encoded.length() - 1);
        for (String value : List.of(malformed, "prefix " + malformed, Json.toJson(Map.of("message", malformed))))
            assertFalse(LogSanitizer.body(value).contains("synthetic"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Don't forget: password=synthetic-quoted-secret", "Can't proceed: apikey=synthetic-quoted-secret",
            "Server said 'password=synthetic-quoted-secret' while rejecting input",
            "Error \"password=synthetic-quoted-secret\" while rejecting input",
            "Error 'authorization=synthetic-quoted-secret' later",
            "Server said 'password=synthetic-quoted-secret': rejected",
            "Don't forget: password='synthetic-quoted-secret'", "Can't proceed: apikey='synthetic-quoted-secret'",
            "Server said 'Don't forget: password=synthetic-quoted-secret'" })
    void should_mask_assignments_inside_quoted_messages_and_contractions(String value) {
        String result = LogSanitizer.body(value);
        assertFalse(result.contains("synthetic"));
        assertTrue(result.contains("***"));
        assertEquals(result, LogSanitizer.body(result));
        String json = Json.toJson(Map.of("message", value));
        String sanitizedJson = LogSanitizer.body(json);
        assertFalse(sanitizedJson.contains("synthetic"));
        assertEquals(sanitizedJson, LogSanitizer.body(sanitizedJson));
        assertTrue(Json.fromJson(sanitizedJson, Map.class).containsKey("message"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "'", "users'", "('", "='", "print('", "['", "don'", "What do the users'",
            "Don't remove this user's ordinary text", "Server said 'try again' and left", "Quoted \"ordinary\nmultiline\" text" })
    void should_preserve_apostrophes_and_unterminated_quoted_tool_fragments(String value) {
        assertEquals(value, LogSanitizer.body(value));
        String json = Json.toJson(Map.of("content", value));
        assertEquals(json, LogSanitizer.body(json));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Invalid token=synthetic-exact-secret", "Invalid id_token: synthetic-exact-secret",
            "Invalid iam_token=synthetic-exact-secret", "Error bearer_token=synthetic-exact-secret",
            "Invalid Authorization: Bearer synthetic-exact-secret",
            "Invalid Cookie: session=synthetic-exact-secret; refresh=synthetic-second-secret",
            "Invalid Set-Cookie: session=synthetic-exact-secret; refresh=synthetic-second-secret" })
    void should_recognize_exact_aliases_after_an_ordinary_message_prefix(String value) {
        String result = LogSanitizer.body(value);
        assertFalse(result.contains("synthetic"));
        assertTrue(result.contains("***"));
        assertEquals(result, LogSanitizer.body(result));
    }

    @ParameterizedTest
    @ValueSource(strings = { "auth.token=synthetic-dotted-secret", "config.token: synthetic-dotted-secret",
            "request.cookie=synthetic-dotted-secret", "Request failed auth.response.token=synthetic-dotted-secret",
            "config.IAM_TOKEN=synthetic-dotted-secret", "prefix%2Etoken=synthetic-dotted-secret",
            "Response request.Cookie: session=synthetic-dotted-secret; other=synthetic-second-secret",
            "Response request.Set-Cookie: session=synthetic-dotted-secret; other=synthetic-second-secret",
            "Error request.Authorization: Bearer synthetic-dotted-secret", "Error \"auth.token\"=synthetic-dotted-secret",
            "Error 'request.cookie': synthetic-dotted-secret" })
    void should_recognize_exact_aliases_after_dotted_text_prefixes(String value) {
        String result = LogSanitizer.body(value);
        assertFalse(result.contains("synthetic"));
        assertTrue(result.contains("***"));
        assertEquals(result, LogSanitizer.body(result));
        String json = Json.toJson(Map.of("message", value));
        String sanitized = LogSanitizer.body(json);
        assertFalse(sanitized.contains("synthetic"));
        assertTrue(Json.fromJson(sanitized, Map.class).containsKey("message"));
    }

    @Test
    void should_keep_dotted_name_matching_limited_to_textual_assignments() {
        for (String value : List.of("auth.tokenizer=visible", "request.cookies=visible", "auth.idtoken.extra=visible",
            "{\"auth.token\":\"visible\",\"request.cookie\":\"visible\"}"))
            assertEquals(value, LogSanitizer.body(value));
        URI uri = URI.create("https://example.com/?auth.token=visible");
        assertEquals(uri, LogSanitizer.uri(uri));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Use regex \"\\d+\\s*\"", "Use path \"C:\\Users\\alice\\work\"",
            "Use LaTeX \"\\alpha + \\frac{1}{2}\"", "Price \"\\$10\"", "Code \"\\x41\\u12\\uGGGG\"",
            "\"\\d+\"", "Trailing \"C:\\folder\\" })
    void should_preserve_non_json_backslash_sequences_inside_ordinary_quotes(String value) {
        assertEquals(value, LogSanitizer.body(value));
        String json = Json.toJson(Map.of("content", value));
        assertEquals(json, LogSanitizer.body(json));
    }

    @Test
    void should_preserve_unknown_text_escapes_while_masking_nearby_secrets() {
        String value = "Use regex \"\\d+\" then password=synthetic-nearby-secret";
        String result = LogSanitizer.body(value);
        assertEquals("Use regex \"\\d+\" then password=***", result);
        String json = Json.toJson(Map.of("message", value));
        String sanitized = LogSanitizer.body(json);
        assertFalse(sanitized.contains("synthetic"));
        assertEquals(result, Json.fromJson(sanitized, Map.class).get("message"));
        JSONAssert.assertEquals("{\"password\":\"***\"}",
            LogSanitizer.body("{\"pass\\u0077ord\":\"synthetic-valid-escape-secret\"}"), true);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"password\":\"synthetic-strict-secret\\q\"}",
            "{\"password\":\"synthetic-strict-secret\\u12\"}", "{\"password\":\"synthetic-strict-secret\\uGGGG\"}" })
    void should_keep_structured_json_escape_validation_strict(String value) {
        String result = LogSanitizer.body(value);
        assertEquals("[body omitted: unable to sanitize]", result);
        assertFalse(result.contains("synthetic"));
        assertEquals(result, LogSanitizer.body(result));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Please type \"Password:\"", "Use label 'Password:'", "Label \"token=\"   ", "\"Password:\"",
            "'Password:'" })
    void should_preserve_quoted_sensitive_labels_without_a_following_value(String value) {
        assertEquals(value, LogSanitizer.body(value));
        String json = Json.toJson(Map.of("content", value));
        assertEquals(json, LogSanitizer.body(json));
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"Password:\" synthetic-label-secret", "Label 'Password:' synthetic-label-secret",
            "Server said 'password='synthetic-label-secret''" })
    void should_omit_ambiguous_sensitive_labels_with_a_following_value(String value) {
        assertEquals("[body omitted: unable to sanitize]", LogSanitizer.body(value));
        String json = Json.toJson(Map.of("message", value));
        String sanitized = LogSanitizer.body(json);
        assertFalse(sanitized.contains("synthetic"));
        assertTrue(Json.fromJson(sanitized, Map.class).containsKey("message"));
    }

    @Test
    void should_handle_many_non_json_text_escapes_in_bounded_time() {
        String value = "Regex \"" + "\\d".repeat(500_000) + "\"";
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(value, LogSanitizer.body(value));
            String json = Json.toJson(Map.of("content", value));
            assertEquals(json, LogSanitizer.body(json));
        });
    }

    @Test
    void should_mask_entire_quoted_values_containing_word_apostrophes() {
        assertEquals("password=***", LogSanitizer.body("password='don't tell anyone'"));
        assertEquals("Don't forget: password=***", LogSanitizer.body("Don't forget: password='don't tell anyone'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Server said 'password='synthetic-ambiguous-secret'",
            "Server said 'password='synthetic-ambiguous-secret'' while retrying",
            "Error \"password=\"synthetic-ambiguous-secret\"", "Error \"token:\"synthetic-ambiguous-secret\"",
            "Don't forget: 'password='synthetic-ambiguous-secret''" })
    void should_omit_ambiguous_quote_boundaries_after_sensitive_assignments(String value) {
        String sanitized = LogSanitizer.body(value);
        assertEquals("[body omitted: unable to sanitize]", sanitized);
        String json = Json.toJson(Map.of("message", value));
        String sanitizedJson = LogSanitizer.body(json);
        assertFalse(sanitizedJson.contains("synthetic"));
        assertTrue(Json.fromJson(sanitizedJson, Map.class).containsKey("message"));
        assertEquals(sanitizedJson, LogSanitizer.body(sanitizedJson));
    }

    @Test
    void should_keep_quoted_text_scanning_bounded_without_losing_sensitive_assignments() {
        String escaped = "\\\"".repeat(250_000);
        String alternating = "\"'".repeat(250_000);
        String quoted = "Server said '" + "ordinary words ".repeat(70_000) + "password=synthetic-long-quoted-secret'";
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(escaped, LogSanitizer.body(escaped));
            assertEquals(alternating, LogSanitizer.body(alternating));
            String sanitized = LogSanitizer.body(quoted);
            assertFalse(sanitized.contains("synthetic"));
            assertTrue(sanitized.endsWith("***'"));
            String adversarial = escaped + "': password=synthetic-long-quoted-secret";
            assertFalse(LogSanitizer.body(adversarial).contains("synthetic"));
        });
    }

    @Test
    @DisabledInNativeImage
    void should_sanitize_json_without_a_json_provider() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        URL coreClasses = LogSanitizer.class.getProtectionDomain().getCodeSource().getLocation();
        try (var isolated = new URLClassLoader(new URL[] { coreClasses }, ClassLoader.getPlatformClassLoader())) {
            Thread.currentThread().setContextClassLoader(isolated);
            var sanitize = isolated.loadClass(LogSanitizer.class.getName()).getMethod("body", String.class);
            assertEquals("{\"password\":\"***\"}", sanitize.invoke(null, "{\"password\":\"synthetic-secret\"}"));
            assertEquals("{\"password\":\"***\"}", sanitize.invoke(null, "{\"password\":\"synthetic-secret\"}"));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
