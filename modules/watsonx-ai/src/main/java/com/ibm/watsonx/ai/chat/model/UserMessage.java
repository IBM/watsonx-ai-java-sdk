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
import java.util.Arrays;
import java.util.List;

/**
 * Represents a user message within a chat interaction.
 * <p>
 * A {@code UserMessage} contains content sent by a user, and can include text, image, or video content.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * UserMessage.text("Hello!");
 * }</pre>
 *
 * Example usage with {@code UserContent}:
 *
 * <pre>{@code
 * UserMessage.of(
 *     TextContent.of("Tell me more about this image"),
 *     ImageContent.of("image/svg", base64Data)
 * );
 * }</pre>
 *
 * @param role the role of the message's author, always {@code user}
 * @param content the list of content blocks in this message (must not be null, size 1–100)
 * @param name an optional name to distinguish between users sharing the same role
 *
 * @see TextContent
 * @see ImageContent
 * @see VideoContent
 * @see AudioContent
 */
public record UserMessage(String role, List<UserContent> content, String name) implements ChatMessage {

    public static final String ROLE = "user";

    public UserMessage {
        role = ROLE;
        requireNonNull(content, "content must not be null");
    }

    /**
     * Creates a new {@link UserMessage} with a participant name.
     *
     * @param name an optional participant name to help differentiate users
     * @param contents the list of user content (must not be null)
     * @return a new {@link UserMessage}
     *
     * @see TextContent
     * @see ImageContent
     * @see VideoContent
     * @see AudioContent
     */
    public static UserMessage of(String name, List<UserContent> contents) {
        return new UserMessage(ROLE, contents, name);
    }

    /**
     * Creates a new {@link UserMessage}.
     *
     * @param contents the list of user content
     * @return a new {@link UserMessage}
     *
     * @see TextContent
     * @see ImageContent
     * @see VideoContent
     * @see AudioContent
     */
    public static UserMessage of(List<UserContent> contents) {
        return of(null, contents);
    }

    /**
     * Creates a new {@link UserMessage} from a variable number of {@link UserContent} elements.
     *
     * @param contents one or more user content elements
     * @return a new {@link UserMessage}
     *
     * @see TextContent
     * @see ImageContent
     * @see VideoContent
     * @see AudioContent
     */
    public static UserMessage of(UserContent... contents) {
        return of(Arrays.asList(contents));
    }

    /**
     * Creates a new {@link UserMessage} containing plain text content.
     *
     * @param text the text content
     * @return a new {@link UserMessage} containing a single {@link TextContent}
     */
    public static UserMessage text(String text) {
        var content = TextContent.of(text);
        return of(null, List.of(content));
    }

    /**
     * Creates a new {@link UserMessage} containing text instructions and an image from a file.
     *
     * @param text the text instructions or prompt describing what to do with the image
     * @param file the file containing the image data
     * @return a new {@link UserMessage} containing {@link TextContent} and {@link ImageContent}
     */
    public static UserMessage image(String text, File file) {
        return image(text, file.toPath());
    }

    /**
     * Creates a new {@link UserMessage} containing text instructions and an image from a path.
     *
     * @param text the text instructions or prompt describing what to do with the image
     * @param path the path to the file containing the image data
     * @return a new {@link UserMessage} containing {@link TextContent} and {@link ImageContent}
     */
    public static UserMessage image(String text, Path path) {
        try (var is = Files.newInputStream(path)) {
            var mimeType = Files.probeContentType(path);
            return image(text, is, mimeType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new {@link UserMessage} containing text instructions and an image from an input stream.
     *
     * @param text the text instructions or prompt describing what to do with the image
     * @param is the input stream containing the image data
     * @param mimetype the MIME type of the image (e.g., "image/png", "image/jpeg")
     * @return a new {@link UserMessage} containing {@link TextContent} and {@link ImageContent}
     */
    public static UserMessage image(String text, InputStream is, String mimetype) {
        try {
            return of(null, List.of(TextContent.of(text), ImageContent.from(is, mimetype)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the text content.
     * <p>
     * This method is a convenience accessor for messages containing a single {@link TextContent} element.
     *
     * @return the text content of this message
     * @throws IllegalStateException if the message does not contain exactly one {@link TextContent} element
     */
    public String text() {
        if (content.isEmpty())
            throw new IllegalStateException("Message contains no content");

        if (content.size() > 1) {
            throw new IllegalStateException(
                "Message contains multiple content elements (" + content.size() + "). " +
                    "Use content() to access all elements."
            );
        }

        UserContent firstContent = content.get(0);
        if (firstContent instanceof TextContent textContent)
            return textContent.text();

        throw new IllegalStateException("Message does not contain text content.");
    }
}
