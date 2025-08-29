/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.IOException;
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
     * Creates an {@code ImageContent} from a {@link File}.
     *
     * @param file the image file to load
     * @return a new {@code ImageContent}
     * @throws IOException if the file cannot be read
     */
    public static ImageContent from(File file) throws IOException {
        return from(file, null);
    }

    /**
     * Creates an {@code ImageContent} from a {@link File}, with a specified detail level.
     *
     * @param file the image file to load
     * @param detail the level of detail for how the model should process the image
     * @return a new {@code ImageContent}
     * @throws IOException if the file cannot be read
     */
    public static ImageContent from(File file, Detail detail) throws IOException {
        return from(file.toPath(), detail);
    }

    /**
     * Creates an {@code ImageContent} from a file {@link Path}.
     *
     * @param path the path to the image file
     * @return a new {@code ImageContent}
     * @throws IOException if the file cannot be read
     */
    public static ImageContent from(Path path) throws IOException {
        return from(path, null);
    }

    /**
     * Creates an {@code ImageContent} from a file {@link Path}, with a specified detail level.
     *
     * @param path the path to the image file
     * @param detail the level of detail for how the model should process the image
     * @return a new {@code ImageContent}
     * @throws IOException if the file cannot be read
     */
    public static ImageContent from(Path path, Detail detail) throws IOException {
        requireNonNull(path);
        var bytes = Files.readAllBytes(path);
        var mimeType = Files.probeContentType(path);
        var base64Data = Base64.getEncoder().encodeToString(bytes);
        return of(Image.of(mimeType, base64Data));
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
