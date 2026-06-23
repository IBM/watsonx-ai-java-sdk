/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

import static java.util.Objects.requireNonNull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

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
        this.type = requireNonNull(type, "type must not be null");
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
     *
     * @param <T> the element type of the list
     * @param elementType the class of the list elements
     * @return a {@code TypeToken} representing {@code List<T>}
     */
    public static <T> TypeToken<List<T>> listOf(Class<T> elementType) {
        requireNonNull(elementType, "elementType must not be null");
        return new TypeToken<>(new ParameterizedTypeImpl(List.class, null, elementType)) {};
    }

    /**
     * Creates a {@code TypeToken} for the parameterized type {@code rawType<typeArguments...>}.
     * <p>
     * Supports any number of type arguments, e.g. {@code parameterizedOf(Map.class, String.class, Integer.class)} for {@code Map<String, Integer>}.
     * <p>
     * <b>Note:</b> this method is inherently type-unsafe. The compiler cannot verify that the inferred type {@code T} matches {@code rawType} and the
     * supplied {@code typeArguments}; the caller is responsible for requesting a type that is consistent with the arguments passed here. Prefer
     * {@link #listOf(Class)} or an anonymous {@code TypeToken} subclass when full compile-time safety is required.
     *
     * @param <T> the type the resulting token is expected to represent (verified by the caller, not the compiler)
     * @param rawType the raw class
     * @param typeArguments the actual type arguments to use; must contain at least one element
     * @return a {@code TypeToken} representing {@code rawType<typeArguments...>}
     * @throws IllegalArgumentException if {@code typeArguments} is empty
     */
    public static <T> TypeToken<T> parameterizedOf(Class<?> rawType, Class<?>... typeArguments) {
        requireNonNull(rawType, "rawType must not be null");
        requireNonNull(typeArguments, "typeArguments must not be null");

        if (typeArguments.length == 0)
            throw new IllegalArgumentException("typeArguments must contain at least one element");

        for (Class<?> typeArgument : typeArguments)
            requireNonNull(typeArgument, "typeArguments must not contain null elements");

        return new TypeToken<>(new ParameterizedTypeImpl(rawType, null, typeArguments)) {};
    }

    @Override
    public String toString() {
        return type.getTypeName();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof TypeToken<?> other))
            return false;

        return Objects.equals(type, other.type);
    }

    private static final class ParameterizedTypeImpl implements ParameterizedType {
        private final Type rawType;
        private final Type ownerType;
        private final Type[] actualTypeArguments;

        ParameterizedTypeImpl(Type rawType, Type ownerType, Type... actualTypeArguments) {
            this.rawType = requireNonNull(rawType, "rawType must not be null");
            this.ownerType = ownerType;
            this.actualTypeArguments = requireNonNull(actualTypeArguments, "actualTypeArguments must not be null").clone();

        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(actualTypeArguments)
                ^ Objects.hashCode(ownerType)
                ^ Objects.hashCode(rawType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (!(obj instanceof ParameterizedType other))
                return false;

            return Objects.equals(ownerType, other.getOwnerType())
                && Objects.equals(rawType, other.getRawType())
                && Arrays.equals(actualTypeArguments, other.getActualTypeArguments());
        }

        @Override
        public String toString() {
            var joiner = new StringJoiner(", ", "<", ">");

            for (Type argument : actualTypeArguments)
                joiner.add(argument.getTypeName());

            return rawType.getTypeName() + joiner;
        }
    }
}