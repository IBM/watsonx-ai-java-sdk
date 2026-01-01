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
 * Represents a user-supplied audio content used in chat interactions.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * AudioContent.of("audio/wav", base64Data);
 * }</pre>
 *
 * @param type the type identifier for this content, always set to {@code input_audio}
 * @param inputAudio the audio content wrapped in an {@link Audio} object
 */
public record AudioContent(String type, Audio inputAudio) implements UserContent {

    public static final String TYPE = "input_audio";

    public AudioContent {
        type = TYPE;
        requireNonNull(inputAudio);
    }

    /**
     * Creates an {@code AudioContent} from a file.
     *
     * @param file the audio file to read
     * @return a new {@code AudioContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static AudioContent from(File file) throws IOException {
        requireNonNull(file);
        return from(file.toPath());
    }

    /**
     * Creates an {@code AudioContent} from a file path.
     *
     * @param path the path to the audio file
     * @return a new {@code AudioContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static AudioContent from(Path path) throws IOException {
        requireNonNull(path);
        var mimetype = Files.probeContentType(path);
        try (var is = Files.newInputStream(path)) {
            return from(is, mimetype);
        }
    }

    /**
     * Creates an {@code AudioContent} from an input stream.
     *
     * @param is the input stream containing the audio data
     * @param mimetype the MIME type of the audio (e.g., {@code audio/wav}, {@code audio/mp3})
     * @return a new {@code AudioContent} instance
     * @throws IOException if an I/O error occurs reading from the stream
     */
    public static AudioContent from(InputStream is, String mimetype) throws IOException {
        requireNonNull(is);
        var data = Base64.getEncoder().encodeToString(is.readAllBytes());
        return of(mimetype, data);
    }

    /**
     * Creates an {@code AudioContent} from format and base64-encoded audio data.
     *
     * @param format the MIME type of the audio (e.g., {@code audio/wav}, {@code audio/mp3})
     * @param data the base64-encoded audio data
     * @return a new {@code AudioContent} instance
     */
    public static AudioContent of(String format, String data) {
        return new AudioContent(TYPE, new Audio(format, data));
    }
}