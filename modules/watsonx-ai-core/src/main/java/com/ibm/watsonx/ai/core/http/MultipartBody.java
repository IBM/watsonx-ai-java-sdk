/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a fully serialized {@code multipart/form-data} HTTP request body.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MultipartBody body = MultipartBody.builder()
 *     .addPart("model", "openai/whisper-tiny")
 *     .addPart("language", "it")
 *     .addInputStream("file", "audio.mp3", inputStream)
 *     .build();
 * }</pre>
 */
public final class MultipartBody {

    private static final String BOUNDARY = "----watsonx-ai-sdk";
    private final byte[] bytes;

    private MultipartBody(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns the value to be used as the {@code Content-Type} HTTP header, including the boundary parameter.
     *
     * @return a string of the form {@code multipart/form-data; boundary=<boundary>}
     */
    public String contentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    /**
     * Returns the raw bytes of the serialized multipart body.
     *
     * @return the body as a byte array
     */
    public byte[] body() {
        return bytes;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * MultipartBody body = MultipartBody.builder()
     *     .addPart("model", "openai/whisper-tiny")
     *     .addPart("language", "it")
     *     .addInputStream("file", "audio.mp3", inputStream)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link MultipartBody} instance.
     */
    public static final class Builder {
        private record Part(String fieldName, String fileName, String contentType, byte[] content) {}

        private static final String CRLF = "\r\n";
        private final List<Part> parts = new ArrayList<>();

        /**
         * Adds a plain-text form field.
         *
         * @param name the form field name.
         * @param value the field value.
         */
        public Builder addPart(String name, String value) {
            requireNonNull(name, "The name must be provided");
            requireNonNull(value, "The value must be provided");

            parts.add(new Part(name, null, "text/plain; charset=UTF-8",
                value.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        /**
         * Adds a binary form field whose content is read from the given {@link InputStream}.
         *
         * @param name the form field name.
         * @param fileName the filename to advertise in the {@code Content-Disposition} header.
         * @param is the input stream to read.
         */
        public Builder addInputStream(String name, String fileName, InputStream is) {
            requireNonNull(name, "The name must be provided");
            requireNonNull(fileName, "The fileName must be provided");
            requireNonNull(is, "The inputstream must be provided");

            try {
                parts.add(new Part(name, fileName, "application/octet-stream", is.readAllBytes()));
                return this;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Builds a {@link MultipartBody} instance using the configured parameters.
         *
         * @return a new instance of {@link MultipartBody}
         */
        public MultipartBody build() {
            if (parts.isEmpty())
                throw new IllegalStateException("Cannot build multipart body with no parts");

            try (var out = new ByteArrayOutputStream()) {
                for (Part part : parts) {
                    out.write(("--" + BOUNDARY + CRLF).getBytes(StandardCharsets.UTF_8));
                    out.write(("Content-Disposition: form-data; name=\"" + part.fieldName() + "\"").getBytes(StandardCharsets.UTF_8));

                    if (nonNull(part.fileName()))
                        out.write(("; filename=\"" + part.fileName() + "\"").getBytes(StandardCharsets.UTF_8));

                    out.write((CRLF).getBytes(StandardCharsets.UTF_8));
                    out.write(("Content-Type: " + part.contentType() + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
                    out.write(part.content());
                    out.write(CRLF.getBytes(StandardCharsets.UTF_8));
                }
                out.write(("--" + BOUNDARY + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
                return new MultipartBody(out.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
