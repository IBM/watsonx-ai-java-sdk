/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

/**
 * Service Provider Interface (SPI) for JSON serialization and deserialization.
 */
public interface JsonAdapter {

  /**
   * Deserializes a JSON string into an instance of the specified class.
   *
   * @param json the JSON content.
   * @param type the target class.
   * @param <T> the result type.
   * @return the deserialized object.
   * @throws RuntimeException if deserialization fails.
   */
  <T> T fromJson(String json, Class<T> clazz);

  /**
   * Serializes an object to its JSON representation.
   *
   * @param object the object to serialize.
   * @return the JSON string.
   * @throws RuntimeException if serialization fails.
   */
  String toJson(Object object);

  /**
   * Pretty-prints the given object into a JSON string.
   *
   * @param object the object to pretty-print
   */
  String prettyPrint(Object object);
}
