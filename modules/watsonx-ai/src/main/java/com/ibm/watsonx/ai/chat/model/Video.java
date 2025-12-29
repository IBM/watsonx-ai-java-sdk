/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a user-supplied video input used in chat interactions.
 * <p>
 * The video must be provided as a base64-encoded.
 * <p>
 * Example of a valid video URL:
 *
 * <pre>{@code
 * data:video/mp4;base64,AAAAHGZ0eXBN...
 * }</pre>
 *
 * @param url the base64-encoded video (e.g., {@code data:video/mp4;base64,...})
 */
public record Video(String url) {

    public Video {
        requireNonNull(url);
    }

    /**
     * Create a {@code Video} instance from media type and base64-encoded video data.
     *
     * @param mimeType the MIME type of the video (e.g., {@code video/mp4})
     * @param data the base64-encoded video data
     * @return a new {@link Video} instance
     */
    public static Video of(String mimeType, String data) {
        requireNonNull(mimeType);
        return new Video("data:%s;base64,%s".formatted(mimeType, data));
    }
}

