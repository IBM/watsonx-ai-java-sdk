/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;
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
 *   TextContent.of("Tell me more about this image"),
 *   ImageContent.of("image/svg", base64Data)
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
 */
public final record UserMessage(String role, List<UserContent> content, String name) implements ChatMessage {

  public static final String ROLE = "user";

  public UserMessage {
    role = ROLE;
    requireNonNull(content);
  }

  /**
   * Creates a new {@link UserMessage} with a participant name.
   *
   * @param name an optional participant name to help differentiate users
   * @param contents the list of user content (must not be null)
   * @return a new {@link UserMessage}
   */
  public static UserMessage of(String name, List<UserContent> contents) {
    return new UserMessage(ROLE, contents, name);
  }

  /**
   * Creates a new {@link UserMessage}.
   *
   * @param contents the list of user content
   * @return a new {@link UserMessage}
   * @see TextContent
   * @see ImageContent
   * @see VideoContent
   */
  public static UserMessage of(List<UserContent> contents) {
    return of(null, contents);
  }

  /**
   * Creates a new {@link UserMessage} from a variable number of {@link UserContent} elements.
   *
   * @param contents one or more user content elements
   * @return a new {@link UserMessage}
   * @see TextContent
   * @see ImageContent
   * @see VideoContent
   */
  public static UserMessage of(UserContent... contents) {
    return of(null, Arrays.asList(contents));
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
}
