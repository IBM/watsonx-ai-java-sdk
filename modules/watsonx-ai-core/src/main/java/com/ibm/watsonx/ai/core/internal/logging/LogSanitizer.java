/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.internal.logging;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies sanitization policy to HTTP and streaming diagnostics.
 */
public final class LogSanitizer {

    private static final String MASK = "***";
    private static final String OMITTED = "[body omitted: unable to sanitize]";
    private static final int MAX_DEPTH = 32;
    private static final TextResult OMITTED_TEXT = new TextResult(OMITTED, true, false);
    private static final Set<String> SENSITIVE_HEADERS =
        Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "iam-token", "x-api-key");
    private static final Set<String> TOKEN_KEYS = Set.of("token", "idtoken", "iamtoken", "bearertoken");
    private static final Pattern AUTHENTICATION =
        Pattern.compile("(?i)\\b(Bearer|Basic|ZenApiKey)(\\s++)([A-Za-z0-9_+/.=-]{8,}+)");
    private static final Pattern BASE64 = Pattern.compile("(data:[\\w/+.-]++;base64,)([^\\s\"']++)");
    private static final boolean[] BASE64_ALPHABET = new boolean[128];

    static {
        for (char character : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/-_".toCharArray())
            BASE64_ALPHABET[character] = true;
    }

    private LogSanitizer() {}

    /**
     * Sanitizes a diagnostic body while preserving ordinary text and JSON syntax outside replaced values.
     *
     * @param value the diagnostic body
     * @return the processed body, or {@code null} for absent input
     */
    public static String body(String value) {
        try {
            return body(value, 0);
        } catch (RuntimeException | StackOverflowError e) {
            return OMITTED;
        }
    }

    /**
     * Creates an immutable header snapshot with sensitive values masked in full.
     *
     * @param values the original headers
     * @return the processed headers, or {@code null} for absent input
     */
    public static Map<String, List<String>> headers(Map<String, List<String>> values) {
        if (isNull(values))
            return null;
        var result = new LinkedHashMap<String, List<String>>();
        values.forEach((key, entries) -> result.put(key,
            entries.stream().map(value -> SENSITIVE_HEADERS.contains(key.toLowerCase(Locale.ROOT)) ? MASK : body(value)).toList()));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates a sanitized HTTP header snapshot.
     *
     * @param values the original headers
     * @return the processed headers, or {@code null} for absent input
     */
    public static HttpHeaders headers(HttpHeaders values) {
        return isNull(values) ? null : HttpHeaders.of(headers(values.map()), (key, value) -> true);
    }

    /**
     * Removes URI user information and fragments and masks sensitive query parameters.
     *
     * @param value the original URI
     * @return the processed URI, or {@code null} for absent input
     */
    public static URI uri(URI value) {
        if (isNull(value))
            return null;
        if (value.isOpaque())
            return URI.create("about:blank");
        try {
            var result = new StringBuilder();
            if (nonNull(value.getScheme()))
                result.append(value.getScheme()).append(':');
            if (nonNull(value.getRawAuthority())) {
                String authority = value.getRawAuthority();
                result.append("//").append(authority.substring(authority.lastIndexOf('@') + 1));
            }
            if (nonNull(value.getRawPath()))
                result.append(value.getRawPath());
            if (nonNull(value.getRawQuery()))
                result.append('?').append(query(value.getRawQuery()));
            return URI.create(result.toString());
        } catch (IllegalArgumentException e) {
            return URI.create("about:blank");
        }
    }

    /**
     * Copies the sanitized message and code locations without retaining original causes, suppressed errors or specialized accessors.
     *
     * @param value the original error
     * @return the diagnostic error, or {@code null} for absent input
     */
    public static Throwable error(Throwable value) {
        if (isNull(value))
            return null;
        String message = body(value.getMessage());
        var snapshot = new SanitizedLogException(value.getClass().getSimpleName() + (isNull(message) ? "" : ": " + message));
        snapshot.setStackTrace(value.getStackTrace());
        return snapshot;
    }

    private static String body(String value, int depth) {
        if (isNull(value) || value.isBlank())
            return value;
        if (depth > MAX_DEPTH)
            return OMITTED;
        if (plainText(value))
            return value;
        var result = new StringBuilder();
        int offset = 0;
        while (offset < value.length()) {
            int first = whitespaceEnd(value, offset);
            if (first == value.length()) {
                result.append(value, offset, value.length());
                break;
            }
            int start = first;
            while (start < value.length() && value.charAt(start) != '{' && value.charAt(start) != '[')
                start++;
            if (start == value.length())
                start = -1;
            if (value.charAt(first) == '"')
                start = first;
            if (start < 0) {
                result.append(text(value.substring(offset)).value());
                break;
            }
            try {
                var json = new JsonScanner(value, start, depth);
                json.value(true, false, 0);
                int end = json.position;
                boolean trailing = whitespaceEnd(value, end) != value.length();
                if (start == first && trailing && (json.masked || value.charAt(start) == '"')) {
                    var fallback = text(value.substring(offset));
                    return json.masked || fallback.masked() ? OMITTED : result.append(fallback.value()).toString();
                }
                if (offset == 0 && start == first && !trailing) {
                    if (!json.masked)
                        return value;
                    result.append(value, 0, start);
                    json.appendTo(result);
                    return result.append(value, end, value.length()).toString();
                }
                String prefix = value.substring(offset, start);
                result.append(text(prefix).value());
                if (start > first && text(prefix + "x").masked())
                    result.append(MASK);
                else
                    json.appendTo(result);
                offset = end;
            } catch (InvalidJsonException e) {
                String remaining = value.substring(offset);
                var fallback = text(remaining);
                boolean encodedSecret = false;
                if (value.charAt(first) == '"') {
                    try {
                        String partial = new JsonScanner(value, first, depth).string(true);
                        encodedSecret = text(partial).masked();
                    } catch (InvalidJsonException invalid) {
                        encodedSecret = true;
                    }
                }
                return e.tooDeep || fallback.masked() || encodedSecret ? OMITTED : result.append(fallback.value()).toString();
            }
        }
        return result.toString();
    }

    private static boolean plainText(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '{' || character == '[' || character == '"' || character == '\'' || character == ':' || character == '=')
                return false;
            if ((character == 'b' || character == 'B')
                && (value.regionMatches(true, index, "Bearer", 0, 6) || value.regionMatches(true, index, "Basic", 0, 5)))
                return false;
            if ((character == 'z' || character == 'Z') && value.regionMatches(true, index, "ZenApiKey", 0, 9))
                return false;
        }
        return true;
    }

    private static boolean authenticationScheme(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character == 'b' || character == 'B')
                && (value.regionMatches(true, index, "Bearer", 0, 6) || value.regionMatches(true, index, "Basic", 0, 5)))
                return true;
            if ((character == 'z' || character == 'Z') && value.regionMatches(true, index, "ZenApiKey", 0, 9))
                return true;
        }
        return false;
    }

    private static TextResult text(String value) {
        return text(value, 0);
    }

    private static TextResult text(String value, int depth) {
        if (depth > MAX_DEPTH)
            return OMITTED_TEXT;
        if (value.isEmpty() || plainText(value))
            return new TextResult(value, false, false);
        var result = new StringBuilder();
        int copied = 0;
        boolean masked = false;
        boolean pendingAssignment = false;
        for (int index = 0; index < value.length();) {
            char character = value.charAt(index);
            boolean quoted = character == '"' || character == '\'' && !wordApostrophe(value, index);
            int end;
            String key;
            if (quoted) {
                int closing = closingQuote(value, index);
                boolean closed = closing >= 0;
                end = closed ? closing + 1 : value.length();
                if (character == '"') {
                    try {
                        key = new JsonScanner(value, index, 0).string(true);
                    } catch (InvalidJsonException e) {
                        return OMITTED_TEXT;
                    }
                } else {
                    key = value.substring(index + 1, closed ? closing : value.length());
                }
                TextResult contents = text(key, depth + 1);
                if (contents.pendingAssignment() && whitespaceEnd(value, end) < value.length())
                    return OMITTED_TEXT;
                if (contents.masked()) {
                    if (OMITTED.equals(contents.value()) || character == '"' && !closed)
                        return OMITTED_TEXT;
                    result.append(value, copied, index);
                    if (character == '"')
                        result.append(quote(contents.value()));
                    else {
                        result.append(character).append(contents.value());
                        if (closed)
                            result.append(character);
                    }
                    copied = end;
                    masked = true;
                }
            } else if (keyCharacter(character) && (index == 0 || !keyCharacter(value.charAt(index - 1)))) {
                end = index + 1;
                while (end < value.length() && keyCharacter(value.charAt(end)))
                    end++;
                key = value.substring(index, end);
            } else {
                index++;
                continue;
            }
            int separator = whitespaceEnd(value, end);
            if (separator == value.length() || (value.charAt(separator) != ':' && value.charAt(separator) != '=')) {
                index = end;
                continue;
            }
            String recognizedKey = decodeKey(key);
            if (!sensitive(recognizedKey))
                recognizedKey = lastSegment(recognizedKey, quoted);
            int valueStart = whitespaceEnd(value, separator + 1);
            if (!sensitive(recognizedKey)) {
                index = separator + 1;
                continue;
            }
            if (valueStart == value.length()) {
                pendingAssignment = true;
                index = valueStart;
                continue;
            }
            int valueEnd = assignmentEnd(value, valueStart);
            if (value.charAt(separator) == ':' && sensitiveHeader(recognizedKey, quoted)) {
                valueEnd = value.indexOf('\n', valueStart);
                if (valueEnd < 0)
                    valueEnd = value.length();
            }
            result.append(value, copied, valueStart).append(MASK);
            copied = valueEnd;
            index = valueEnd;
            masked = true;
        }
        String sanitized = value;
        if (copied > 0) {
            result.append(value, copied, value.length());
            sanitized = result.toString();
        }
        if (authenticationScheme(sanitized)) {
            Matcher authentication = AUTHENTICATION.matcher(sanitized);
            var authenticated = new StringBuilder();
            while (authentication.find()) {
                String token = authentication.group(3);
                boolean titleCase = Character.isUpperCase(token.charAt(0)) && token.substring(1).chars().allMatch(Character::isLowerCase);
                boolean mixedCase = token.chars().anyMatch(Character::isUpperCase) && token.chars().anyMatch(Character::isLowerCase);
                boolean plausible = token.chars().anyMatch(c -> !Character.isLetter(c)) || mixedCase && !titleCase;
                if (plausible) {
                    authentication.appendReplacement(authenticated,
                        Matcher.quoteReplacement(authentication.group(1) + authentication.group(2) + MASK));
                    masked = true;
                }
            }
            authentication.appendTail(authenticated);
            sanitized = authenticated.toString();
        }
        if (sanitized.contains(";base64,")) {
            Matcher media = BASE64.matcher(sanitized);
            var truncated = new StringBuilder();
            while (media.find())
                media.appendReplacement(truncated, Matcher.quoteReplacement(media.group(1) + truncate(media.group(2))));
            media.appendTail(truncated);
            sanitized = truncated.toString();
        }
        return new TextResult(sanitized, masked, pendingAssignment);
    }

    private static int assignmentEnd(String value, int start) {
        char character = value.charAt(start);
        if (character == '"' || character == '\'')
            return quotedEnd(value, start);
        if (character == '{' || character == '[') {
            int depth = 0;
            for (int index = start; index < value.length(); index++) {
                char item = value.charAt(index);
                if (item == '"' || item == '\'') {
                    index = quotedEnd(value, index) - 1;
                } else if (item == '{' || item == '[') {
                    depth++;
                } else if ((item == '}' || item == ']') && --depth == 0) {
                    return index + 1;
                }
            }
            return value.length();
        }
        int end = start;
        while (end < value.length() && !valueDelimiter(value.charAt(end)))
            end++;
        String scheme = value.substring(start, end);
        if (scheme.equalsIgnoreCase("Bearer") || scheme.equalsIgnoreCase("Basic") || scheme.equalsIgnoreCase("ZenApiKey")) {
            end = whitespaceEnd(value, end);
            while (end < value.length() && !valueDelimiter(value.charAt(end)))
                end++;
        }
        return end;
    }

    private static int quotedEnd(String value, int start) {
        int closing = closingQuote(value, start);
        return closing < 0 ? value.length() : closing + 1;
    }

    private static int closingQuote(String value, int start) {
        char quote = value.charAt(start);
        int index = start + 1;
        while (index < value.length()) {
            char character = value.charAt(index++);
            if (character == '\\' && index < value.length())
                index++;
            else if (character == quote && (quote != '\'' || !wordApostrophe(value, index - 1)))
                return index - 1;
        }
        return -1;
    }

    private static boolean wordApostrophe(String value, int index) {
        return index > 0 && index + 1 < value.length() && Character.isLetterOrDigit(value.charAt(index - 1))
            && Character.isLetterOrDigit(value.charAt(index + 1));
    }

    private static int whitespaceEnd(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index)))
            index++;
        return index;
    }

    private static boolean keyCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '%' || value == '+' || value == '.' || value == '-' || value == ' ';
    }

    private static boolean valueDelimiter(char value) {
        return Character.isWhitespace(value) || value == '&' || value == ',' || value == ';' || value == '}' || value == ']';
    }

    private static String decodeKey(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return key;
        }
    }

    private static String truncate(String value) {
        return value.length() > 15 && !value.endsWith("...") ? value.substring(0, 15) + "..." : value;
    }

    private static String query(String value) {
        var result = new ArrayList<String>();
        for (String parameter : value.split("&", -1)) {
            int separator = parameter.indexOf('=');
            String key = separator < 0 ? parameter : parameter.substring(0, separator);
            result.add(sensitive(decodeKey(key)) ? key + "=" + MASK : parameter);
        }
        return String.join("&", result);
    }

    private static boolean sensitive(String key) {
        String normalized = key.replace("_", "").replace("-", "").replace(" ", "").replace("\\", "").toLowerCase(Locale.ROOT);
        return normalized.contains("apikey") || normalized.contains("password") || normalized.contains("secret")
            || normalized.contains("credential") || normalized.contains("accesstoken") || normalized.contains("refreshtoken")
            || normalized.equals("authorization") || normalized.equals("proxyauthorization") || normalized.equals("cookie")
            || normalized.equals("setcookie") || TOKEN_KEYS.contains(normalized);
    }

    private static boolean sensitiveHeader(String key, boolean quoted) {
        String normalized = key.strip().replace("\\", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_HEADERS.contains(lastSegment(normalized, quoted).strip());
    }

    private static String lastSegment(String key, boolean quoted) {
        String stripped = key.stripTrailing();
        int separator = stripped.lastIndexOf('.');
        if (!quoted)
            separator = Math.max(separator, stripped.lastIndexOf(' '));
        return stripped.substring(separator + 1);
    }

    private static String quote(String value) {
        var result = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char item = value.charAt(index);
            switch(item) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (item < 0x20)
                        result.append("\\u00").append(Character.forDigit(item >>> 4, 16)).append(Character.forDigit(item & 15, 16));
                    else
                        result.append(item);
                }
            }
        }
        return result.append('"').toString();
    }

    private record TextResult(String value, boolean masked, boolean pendingAssignment) {}

    private static final class JsonScanner {
        private final String input;
        private StringBuilder output;
        private int copied;
        private final int origin;
        private final int bodyDepth;
        private int position;
        private boolean masked;

        private JsonScanner(String input, int position, int bodyDepth) {
            this.input = input;
            this.position = position;
            this.origin = position;
            this.copied = position;
            this.bodyDepth = bodyDepth;
        }

        private void replace(int from, String replacement) {
            if (isNull(output))
                output = new StringBuilder();
            output.append(input, copied, from).append(replacement);
            copied = position;
            masked = true;
        }

        private void appendTo(StringBuilder result) {
            if (nonNull(output))
                result.append(output);
            result.append(input, nonNull(output) ? copied : origin, position);
        }

        private void value(boolean emit, boolean media, int depth) {
            if (depth > MAX_DEPTH)
                throw new InvalidJsonException(true);
            whitespace();
            if (position == input.length())
                throw new InvalidJsonException(false);
            char character = input.charAt(position);
            if (character == '"') {
                if (emit && bodyDepth < MAX_DEPTH && mediaString(media))
                    return;
                int start = position;
                String decoded = string();
                if (emit) {
                    String sanitized = body(decoded, bodyDepth + 1);
                    if (media)
                        sanitized = truncate(sanitized);
                    if (!decoded.equals(sanitized))
                        replace(start, quote(sanitized));
                }
            } else if (character == '{') {
                position++;
                whitespace();
                if (accept('}'))
                    return;
                while (true) {
                    String key = string();
                    whitespace();
                    expect(':');
                    whitespace();
                    boolean secret = sensitive(key);
                    int valueStart = position;
                    value(emit && !secret, "data".equals(key), depth + 1);
                    if (emit && secret)
                        replace(valueStart, "\"***\"");
                    whitespace();
                    if (accept('}'))
                        return;
                    expect(',');
                    whitespace();
                }
            } else if (character == '[') {
                position++;
                whitespace();
                if (accept(']'))
                    return;
                while (true) {
                    value(emit, false, depth + 1);
                    whitespace();
                    if (accept(']'))
                        return;
                    expect(',');
                }
            } else {
                int start = position;
                while (position < input.length() && !literalDelimiter(input.charAt(position)))
                    position++;
                if (!literal(start, position))
                    throw new InvalidJsonException(false);
            }
        }

        private boolean mediaString(boolean media) {
            int start = position;
            int payloadStart = start + 1;
            if (!media) {
                if (!input.startsWith("data:", payloadStart))
                    return false;
                int mimeStart = payloadStart + 5;
                payloadStart = mimeStart;
                while (payloadStart < input.length() && mediaTypeCharacter(input.charAt(payloadStart)))
                    payloadStart++;
                if (payloadStart == mimeStart || !input.startsWith(";base64,", payloadStart))
                    return false;
                payloadStart += 8;
            }
            int end = payloadStart;
            int padding = 0;
            while (end < input.length() && input.charAt(end) != '"') {
                char character = input.charAt(end++);
                if (character == '=') {
                    if (++padding > 2)
                        return false;
                } else if (padding > 0 || character >= BASE64_ALPHABET.length || !BASE64_ALPHABET[character]) {
                    return false;
                }
            }
            if (end == input.length() || padding > 1 && assignmentKey(payloadStart, end))
                return false;
            position = end + 1;
            if (end - payloadStart > 15)
                replace(start, "\"" + input.substring(start + 1, payloadStart + 15) + "...\"");
            return true;
        }

        private boolean assignmentKey(int from, int end) {
            int runStart = end - 2;
            while (runStart > from && input.charAt(runStart - 1) != '/')
                runStart--;
            return text(input.substring(runStart, end)).masked();
        }

        private static boolean mediaTypeCharacter(char character) {
            return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9'
                || character == '_' || character == '+' || character == '/' || character == '.' || character == '-';
        }

        private String string() {
            return string(false);
        }

        private String string(boolean partial) {
            expect('"');
            int start = position;
            int appended = start;
            StringBuilder result = null;
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"')
                    return isNull(result) ? input.substring(start, position - 1) : result.append(input, appended, position - 1).toString();
                if (character < 0x20 && !partial)
                    throw new InvalidJsonException(false);
                if (character != '\\')
                    continue;
                if (isNull(result))
                    result = new StringBuilder();
                result.append(input, appended, position - 1);
                if (position == input.length()) {
                    if (partial) {
                        result.append('\\');
                        return result.toString();
                    }
                    throw new InvalidJsonException(false);
                }
                char escape = input.charAt(position++);
                switch(escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        int decoded = 0;
                        boolean valid = position + 4 <= input.length();
                        for (int count = 0; valid && count < 4; count++) {
                            char hex = input.charAt(position + count);
                            int digit = hex >= '0' && hex <= '9' ? hex - '0'
                                : hex >= 'a' && hex <= 'f' ? hex - 'a' + 10
                                    : hex >= 'A' && hex <= 'F' ? hex - 'A' + 10 : -1;
                            valid = digit >= 0;
                            if (valid)
                                decoded = decoded * 16 + digit;
                        }
                        if (valid) {
                            position += 4;
                            result.append((char) decoded);
                        } else if (partial) {
                            result.append("\\u");
                        } else {
                            throw new InvalidJsonException(false);
                        }
                    }
                    default -> {
                        if (!partial)
                            throw new InvalidJsonException(false);
                        result.append('\\').append(escape);
                    }
                }
                appended = position;
            }
            if (partial)
                return isNull(result) ? input.substring(start, position) : result.append(input, appended, position).toString();
            throw new InvalidJsonException(false);
        }

        private void whitespace() {
            while (position < input.length() && whitespaceCharacter(input.charAt(position)))
                position++;
        }

        private boolean accept(char character) {
            if (position == input.length() || input.charAt(position) != character)
                return false;
            position++;
            return true;
        }

        private void expect(char character) {
            if (!accept(character))
                throw new InvalidJsonException(false);
        }

        private static boolean whitespaceCharacter(char character) {
            return character == ' ' || character == '\t' || character == '\r' || character == '\n';
        }

        private static boolean literalDelimiter(char character) {
            return character == ',' || character == ']' || character == '}' || whitespaceCharacter(character);
        }

        private boolean literal(int start, int end) {
            int length = end - start;
            if (length == 4 && input.startsWith("true", start) || length == 4 && input.startsWith("null", start)
                || length == 5 && input.startsWith("false", start))
                return true;
            return number(start, end);
        }

        private boolean number(int start, int end) {
            int index = start;
            if (index < end && input.charAt(index) == '-')
                index++;
            if (index == end)
                return false;
            if (input.charAt(index) == '0')
                index++;
            else {
                if (input.charAt(index) < '1' || input.charAt(index) > '9')
                    return false;
                while (index < end && input.charAt(index) >= '0' && input.charAt(index) <= '9')
                    index++;
            }
            if (index < end && input.charAt(index) == '.') {
                int fraction = ++index;
                while (index < end && input.charAt(index) >= '0' && input.charAt(index) <= '9')
                    index++;
                if (index == fraction)
                    return false;
            }
            if (index < end && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                int exponent = ++index;
                if (index < end && (input.charAt(index) == '+' || input.charAt(index) == '-'))
                    exponent = ++index;
                while (index < end && input.charAt(index) >= '0' && input.charAt(index) <= '9')
                    index++;
                if (index == exponent)
                    return false;
            }
            return index == end;
        }
    }

    private static final class InvalidJsonException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final boolean tooDeep;

        private InvalidJsonException(boolean tooDeep) {
            super(null, null, false, false);
            this.tooDeep = tooDeep;
        }
    }

    private static final class SanitizedLogException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private SanitizedLogException(String message) {
            super(message, null, false, true);
        }
    }
}
