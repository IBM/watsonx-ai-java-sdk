/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/**
 * Represents a user-provided image input to be used in a chat interaction.
 * <p>
 * Example of a valid image URL:
 *
 * <pre>{@code
 * data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD...
 * }</pre>
 *
 * @param url the base64-encoded data URI of the image
 * @param detail the detail level for image processing: "low", "high", or "auto" (default: "auto")
 */
public record Image(String url, String detail) {

    public Image {
        requireNonNull(url);
        detail = requireNonNullElse(detail, "auto");
    }

    /**
     * Creates a new {@code Image} instance.
     *
     * @param mimeType the MIME type of the image (e.g., {@code image/png})
     * @param data the base64-encoded image data
     * @return a new {@code Image} instance
     */
    public static Image of(String mimeType, String data) {
        return of(mimeType, data, null);
    }

    /**
     * Creates a new {@code Image} instance.
     *
     * @param mimeType the MIME type of the image (e.g., {@code image/png})
     * @param data the base64-encoded image data
     * @param detail the level of detail to guide image processing
     * @return a new {@code Image} instance
     */
    public static Image of(String mimeType, String data, Detail detail) {
        requireNonNull(mimeType);
        requireNonNull(data);
        detail = requireNonNullElse(detail, Detail.AUTO);
        return new Image(
            "data:%s;base64,%s".formatted(mimeType, data),
            detail.value()
        );
    }

    /**
     * Enum representing the levels of image detail.
     */
    public static enum Detail {
        LOW("low"),
        HIGH("high"),
        AUTO("auto");

        private String detail;

        Detail(String detail) {
            this.detail = detail;
        }

        public String value() {
            return detail;
        }
    }
}
