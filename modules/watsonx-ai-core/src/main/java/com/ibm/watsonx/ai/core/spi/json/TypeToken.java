/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A utility class that captures and retains generic type information at runtime.
 *
 * @param <T> the generic type to be captured and stored by this token
 */
public abstract class TypeToken<T> {
    private final Type type;

    /**
     * Constructs a new {@code TypeToken} and captures the generic type information provided by the subclass at runtime.
     * <p>
     * This constructor extracts the actual type parameter {@code T} from the subclass's generic superclass. If the generic type information is not
     * present, it throws an {@link IllegalStateException}.
     *
     * @throws IllegalStateException if the {@code TypeToken} is not created with generic type information
     */
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
