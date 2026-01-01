/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Represents a user-supplied video content used in chat interactions.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * VideoContent.of("video/mp4", base64Data);
 * }</pre>
 *
 * @param type the type identifier for this content, always set to {@code video_url}
 * @param videoUrl the video content wrapped in a {@link Video} object
 */
public record VideoContent(String type, Video videoUrl) implements UserContent {

    public static final String TYPE = "video_url";

    public VideoContent {
        type = TYPE;
        requireNonNull(videoUrl);
    }

    /**
     * Creates a {@code VideoContent} from a file.
     *
     * @param file the video file to read
     * @return a new {@code VideoContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static VideoContent from(File file) throws IOException {
        requireNonNull(file);
        return from(file.toPath());
    }

    /**
     * Creates a {@code VideoContent} from a file path.
     *
     * @param path the path to the video file
     * @return a new {@code VideoContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static VideoContent from(Path path) throws IOException {
        requireNonNull(path);
        var mimetype = Files.probeContentType(path);
        try (var is = Files.newInputStream(path)) {
            return from(is, mimetype);
        }
    }

    /**
     * Creates a {@code VideoContent} from an input stream.
     *
     * @param is the input stream containing the video data
     * @param mimetype the MIME type of the video (e.g., {@code video/mp4})
     * @return a new {@code VideoContent} instance
     * @throws IOException if an I/O error occurs reading from the stream
     */
    public static VideoContent from(InputStream is, String mimetype) throws IOException {
        requireNonNull(is);
        var data = Base64.getEncoder().encodeToString(is.readAllBytes());
        return of(mimetype, data);
    }

    /**
     * Create a new {@code VideoContent} instance from media type and base64-encoded video data.
     *
     * @param mimeType the MIME type of the video (e.g., {@code video/mp4})
     * @param data the base64-encoded video data
     * @return a new {@link VideoContent} instance
     */
    public static VideoContent of(String mimeType, String data) {
        return new VideoContent(TYPE, Video.of(mimeType, data));
    }
}