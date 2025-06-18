/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core;

import static java.util.Objects.isNull;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for working with Http objects.
 */
public class HttpUtils {

  /**
   * Converts the body of an {@link HttpResponse} to a {@link String}, handling various response body types.
   * <p>
   * Supported types include:
   * <ul>
   * <li>{@code String}</li>
   * <li>{@code byte[]}</li>
   * <li>{@code InputStream}</li>
   * <li>{@code Stream<String>}</li>
   * <li>{@code Path}</li>
   * </ul>
   * If the response body is none of the above types, the method throws a {@link IllegalArgumentException}.
   * <p>
   * Throws a {@link RuntimeException} if any I/O error occurs during the conversion.
   *
   * @param response the HTTP response to convert
   * @return the response body as a string
   * @throws IllegalArgumentException if the body doesn't match one of the supported types
   * @throws RuntimeException if an exception occurs while reading the body
   */
  public static <T> Optional<String> extractBodyAsString(HttpResponse<T> response) {

    T body = response.body();

    if (isNull(body))
      return Optional.empty();

    try {
      if (body instanceof String str) {
        return Optional.of(str);
      } else if (body instanceof byte[] bytes) {
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
      } else if (body instanceof InputStream inputStream) {
        return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
      } else if (body instanceof Stream<?> stream) {
        return Optional.of(stream.map(Object::toString).collect(Collectors.joining("\n")));
      } else if (body instanceof Path path) {
        return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
      } else {
        throw new IllegalArgumentException("Unsupported body type: " + body.getClass().getName());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert HTTP response body to string", e);
    }
  }
}
