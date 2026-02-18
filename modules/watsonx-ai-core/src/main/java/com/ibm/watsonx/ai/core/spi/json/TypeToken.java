/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

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
     * Constructs a new {@code TypeToken} with an explicitly provided type.
     * <p>
     * This constructor is used internally by factory methods to create {@code TypeToken} instances with programmatically constructed parameterized
     * types.
     *
     * @param type the type to be captured by this token
     */
    private TypeToken(Type type) {
        this.type = type;
    }

    /**
     * Returns the captured {@link Type} represented by this {@code TypeToken}.
     *
     * @return the generic type
     */
    public Type getType() {
        return type;
    }

    /**
     * Creates a {@code TypeToken} for {@code List<T>}.
     * <p>
     * Example usage:
     *
     * <pre>{@code
     * List<String> result = Json.fromJson(json, TypeToken.listOf(String.class));
     * }</pre>
     *
     * @param <T> the element type of the list
     * @param elementType the class of the list elements
     * @return a {@code TypeToken} representing {@code List<T>}
     */
    public static <T> TypeToken<List<T>> listOf(Class<T> elementType) {
        ParameterizedType listType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { elementType };
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        return new TypeToken<>(listType) {};
    }
}
