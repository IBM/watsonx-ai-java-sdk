/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static java.util.Objects.isNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for constructing multipart/form-data HTTP request bodies.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * HTTPRequestMultipartBody body = HTTPRequestMultipartBody.builder()
 *     .addPart("model", "openai/whisper-tiny")
 *     .addPart("language", "it")
 *     .addInputStream("file", is)
 *     .build();
 *
 * HttpRequest request = HttpRequest.newBuilder()
 *     .uri(URI.create(endpoint))
 *     .header("Content-Type", body.getContentType())
 *     .POST(BodyPublishers.ofByteArray(body.getBody()))
 *     .build();
 * }</pre>
 *
 */
public final class HttpRequestMultipartBody {

    private static final String BOUNDARY = "----watsonx-ai-sdk";

    private final byte[] bytes;

    private HttpRequestMultipartBody(byte[] bytes) {
        this.bytes = bytes;
    }

    public String boundary() {
        return BOUNDARY;
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    public byte[] body() {
        return bytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final String CRLF = "\r\n";
        private final List<Part> parts = new ArrayList<>();

        private record Part(String fieldName, String fileName, String contentType, byte[] content) {}

        public Builder addPart(String name, String value) {
            if (isNull(name) || isNull(value))
                return this;

            parts.add(new Part(name, null, "text/plain; charset=UTF-8",
                value.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        public Builder addInputStream(String name, InputStream is) {
            try {
                parts.add(new Part(name, null, "application/octet-stream", is.readAllBytes()));
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        public HttpRequestMultipartBody build() {
            if (parts.isEmpty()) {
                throw new IllegalStateException("Cannot build multipart body with no parts");
            }

            try (var out = new ByteArrayOutputStream()) {
                for (Part part : parts) {
                    writeBoundary(out, BOUNDARY);
                    writePart(out, part);
                }
                out.write(("--" + BOUNDARY + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
                return new HttpRequestMultipartBody(out.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void writeBoundary(OutputStream out, String boundary) throws IOException {
            out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        }

        private void writePart(OutputStream out, Part part) throws IOException {
            out.write(("Content-Disposition: form-data; name=\"" + part.fieldName() + "\"").getBytes(StandardCharsets.UTF_8));
            if (part.fileName() != null) {
                out.write(("; filename=\"" + part.fileName() + "\"").getBytes(StandardCharsets.UTF_8));
            }
            out.write((CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + part.contentType() + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(part.content());
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        }
    }
}
