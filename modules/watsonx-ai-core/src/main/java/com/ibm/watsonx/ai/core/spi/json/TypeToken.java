/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A utility class that captures and retains generic type information at runtime.
 */
public abstract class TypeToken<T> {
  private final Type type;

  protected TypeToken() {
    Type superType = getClass().getGenericSuperclass();
    if (!(superType instanceof ParameterizedType p))
      throw new IllegalStateException("TypeToken must be created with generics.");
    this.type = p.getActualTypeArguments()[0];
  }

  /**
   * Returns the captured {@link Type} represented by this {@code TypeToken}.
   *
   * @return the generic type
   */
  public Type getType() {
    return type;
  }
}
