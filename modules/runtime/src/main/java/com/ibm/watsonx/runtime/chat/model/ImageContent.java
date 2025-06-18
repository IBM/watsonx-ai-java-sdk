/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.chat.model;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.runtime.chat.model.Image.Detail;

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
public final record ImageContent(String type, Image imageUrl) implements UserContent {

  public static final String TYPE = "image_url";

  public ImageContent {
    type = TYPE;
    requireNonNull(imageUrl);
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
   * @param mediaType the MIME type of the image (e.g., {@code image/png}, {@code image/jpeg})
   * @param image the base64-encoded image data (without the data URI prefix)
   * @return a new {@code ImageContent}
   */
  public static ImageContent of(String mediaType, String image) {
    return of(Image.of(mediaType, image, null));
  }

  /**
   * Creates an {@code ImageContent}.
   *
   * @param mediaType the MIME type of the image (e.g., {@code image/png}, {@code image/jpeg})
   * @param image the base64-encoded image data (without the data URI prefix)
   * @param detail the level of detail for how the model should process the image
   * @return a new {@code ImageContent}
   */
  public static ImageContent of(String mediaType, String image, Detail detail) {
    return of(Image.of(mediaType, image, detail));
  }
}
