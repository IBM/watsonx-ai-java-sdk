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
import com.ibm.watsonx.ai.chat.model.Image.Detail;

/**
 * Represents a user-supplied image content used in chat interactions.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * ImageContent.of("image/svg", base64Data)
 * }</pre>
 *
 * @param type the type identifier for this content, always set to {@code image_url}
 * @param imageUrl the image payload to be used as input for the model
 */
public record ImageContent(String type, Image imageUrl) implements UserContent {

    public static final String TYPE = "image_url";

    public ImageContent {
        type = TYPE;
        requireNonNull(imageUrl);
    }

    /**
     * Creates an {@code ImageContent} from a file.
     *
     * @param file the image file to read
     * @return a new {@code ImageContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static ImageContent from(File file) throws IOException {
        requireNonNull(file);
        return from(file.toPath());
    }

    /**
     * Creates an {@code ImageContent} from a file path.
     *
     * @param path the path to the image file
     * @return a new {@code ImageContent} instance
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static ImageContent from(Path path) throws IOException {
        requireNonNull(path);
        var mimetype = Files.probeContentType(path);
        try (var is = Files.newInputStream(path)) {
            return from(is, mimetype);
        }
    }

    /**
     * Creates an {@code ImageContent} from an input stream.
     *
     * @param is the input stream containing the image data
     * @param mimetype the MIME type of the image (e.g., {@code image/png}, {@code image/jpeg})
     * @return a new {@code ImageContent} instance
     * @throws IOException if an I/O error occurs reading from the stream
     */
    public static ImageContent from(InputStream is, String mimetype) throws IOException {
        requireNonNull(is);
        var data = Base64.getEncoder().encodeToString(is.readAllBytes());
        return of(mimetype, data);
    }

    /**
     * Creates an {@code ImageContent} from an {@link Image}.
     *
     * @param imageUrl the image to include
     * @return a new {@code ImageContent}
     */
    public static ImageContent of(Image imageUrl) {
        return new ImageContent(TYPE, imageUrl);
    }

    /**
     * Creates an {@code ImageContent}.
     *
     * @param mimeType the MIME type of the image (e.g., {@code image/png}, {@code image/jpeg})
     * @param image the base64-encoded image data (without the data URI prefix)
     * @return a new {@code ImageContent}
     */
    public static ImageContent of(String mimeType, String image) {
        return of(Image.of(mimeType, image, null));
    }

    /**
     * Creates an {@code ImageContent}.
     *
     * @param mimeType the MIME type of the image (e.g., {@code image/png}, {@code image/jpeg})
     * @param image the base64-encoded image data (without the data URI prefix)
     * @param detail the level of detail for how the model should process the image
     * @return a new {@code ImageContent}
     */
    public static ImageContent of(String mimeType, String image, Detail detail) {
        return of(Image.of(mimeType, image, detail));
    }
}
