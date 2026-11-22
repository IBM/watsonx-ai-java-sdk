/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.parallel.Isolated;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;

@Isolated("Temporarily changes the shared Log4j logger configuration.")
public class LogSanitizerBindingTest {

    @Test
    void should_sanitize_json_encoded_in_strings_and_remain_idempotent() {
        var payload = Json.toJson(Map.of("password", "synthetic-password-12345", "visible", "kept"));
        var encoded = Json.toJson(payload);
        var enclosing = Json.toJson(Map.of("payload", payload));

        var sanitizedEncoded = LogSanitizer.body(encoded);
        var sanitizedEnclosing = LogSanitizer.body(enclosing);
        var enclosedPayload = Json.fromJson(sanitizedEnclosing, Map.class).get("payload");

        for (var sanitized : List.of(sanitizedEncoded, sanitizedEnclosing)) {
            assertFalse(sanitized.contains("synthetic-"));
            assertEquals(sanitized, LogSanitizer.body(sanitized));
        }
        JSONAssert.assertEquals("{\"password\":\"***\",\"visible\":\"kept\"}", Json.fromJson(sanitizedEncoded, String.class), true);
        JSONAssert.assertEquals("{\"password\":\"***\",\"visible\":\"kept\"}", (String) enclosedPayload, true);
    }

    @Test
    void should_preserve_prose_markdown_and_tool_fragments_inside_and_outside_json() {
        for (String text : List.of(
            "[1] Smith et al. (2020) describes this result.",
            "[x for x in range(3)]", "[docs](url)", "[INST] Explain this [/INST]", "{name}", "Unknown field [foo]",
            "Growth was 5% year over year: strong", "Explain the basic concepts of Java", "A bearer of good news",
            "{\"na", "me\":", "\"value", "What do the users'", "print('", "['", "don'")) {
            assertEquals(text, LogSanitizer.body(text), text);
            String enclosing = Json.toJson(Map.of("content", text));
            assertEquals(enclosing, LogSanitizer.body(enclosing), text);
        }
    }

    @Test
    void should_sanitize_quoted_diagnostics_inside_json_strings() {
        for (String message : List.of("Don't forget: password=synthetic-quoted-secret",
            "Server said 'access_token=synthetic-quoted-secret'", "Invalid token=synthetic-quoted-secret")) {
            String input = Json.toJson(Map.of("message", message));

            String sanitized = LogSanitizer.body(input);
            String diagnostic = (String) Json.fromJson(sanitized, Map.class).get("message");

            assertFalse(diagnostic.contains("synthetic-quoted-secret"), diagnostic);
            assertTrue(diagnostic.contains("***"), diagnostic);
            assertEquals(sanitized, LogSanitizer.body(sanitized));
        }
    }

    @Test
    void should_preserve_literal_backslashes_and_terminal_labels_in_json_messages() {
        for (String message : List.of("Use regex \"\\d+\\s+\\w\"", "Path \"C:\\Users\\alice\\project\"",
            "LaTeX \"\\alpha + \\$x + \\x + \\u12 + \\uZZZZ\"", "Please type \"Password:\"", "The label is 'Token:'")) {
            assertEquals(message, LogSanitizer.body(message));
            String json = Json.toJson(Map.of("message", message));
            assertEquals(json, LogSanitizer.body(json));
        }
    }

    @Test
    void should_mask_dotted_aliases_in_json_message_text() {
        for (String message : List.of("auth.token=synthetic-dotted-secret", "config.token: synthetic-dotted-secret",
            "request.cookie=synthetic-dotted-secret", "Response request.cookie: session=synthetic-dotted-secret; other=synthetic-dotted-secret")) {
            String json = Json.toJson(Map.of("message", message));
            String sanitized = LogSanitizer.body(json);
            String diagnostic = (String) Json.fromJson(sanitized, Map.class).get("message");

            assertFalse(diagnostic.contains("synthetic-dotted-secret"), diagnostic);
            assertTrue(diagnostic.contains("***"), diagnostic);
            assertEquals(sanitized, LogSanitizer.body(sanitized));
        }
    }

    @Test
    @DisabledInNativeImage
    @SuppressWarnings("unchecked")
    void should_preserve_nulls_and_citation_suffixes_in_default_http_logs() throws Exception {
        var interceptor = new LoggerInterceptor();
        Logger logger = (Logger) LogManager.getLogger(LoggerInterceptor.class);
        Level previousLevel = logger.getLevel();
        var messages = new CopyOnWriteArrayList<String>();
        var appender = new AbstractAppender("binding-sanitizer-review", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            for (String body : List.of("[1] Smith et al. (2020) binding-review", "{\"finish_reason\":null,\"content\":\"binding-review\"}")) {
                var request = HttpRequest.newBuilder(URI.create("https://binding-review.example/"))
                    .header("Content-Type", "application/json").POST(BodyPublishers.ofString(body)).build();
                var response = (HttpResponse<String>) mock(HttpResponse.class);
                when(response.body()).thenReturn(body);
                when(response.uri()).thenReturn(request.uri());
                when(response.statusCode()).thenReturn(200);
                when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (key, value) -> true));
                var handler = BodyHandlers.ofString();
                var chain = mock(LoggerInterceptor.Chain.class);
                when(chain.proceed(request, handler)).thenReturn(response);
                messages.clear();

                interceptor.intercept(request, handler, 0, chain);

                var matching = messages.stream().filter(message -> message.contains("https://binding-review.example/")).toList();
                assertEquals(2, matching.size());
                for (String message : matching) {
                    if (body.startsWith("["))
                        assertTrue(message.contains(body), message);
                    else {
                        assertTrue(message.contains("finish_reason"), message);
                        assertTrue(message.contains("null"), message);
                    }
                }
            }
        } finally {
            logger.setLevel(previousLevel);
            logger.removeAppender(appender);
            appender.stop();
        }
    }

}
