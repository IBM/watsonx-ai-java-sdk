/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Represents a reference to the default container managed by the watsonx.ai.
 *
 * @param path the fixed path of the file within the container, or {@code null} when the path is supplied at call time via
 *            {@link #toDataReference(String)}
 */
public record ContainerReference(String path) implements DocumentReference {

    /**
     * Produces a {@link DataReference} of type {@code container}.
     *
     * @param objectName the file path within the container; used when this reference was created via {@link #container()}
     * @return a {@link DataReference} with {@code type = "container"} and {@code location.path} set
     */
    @Override
    public DataReference toDataReference(String objectName) {
        return new DataReference(
            DataReference.TYPE_CONTAINER,
            null,
            new CosDataLocation(null, null, path != null ? path : objectName)
        );
    }

    /**
     * Creates a {@link ContainerReference}.
     *
     * @return a new path-free {@link ContainerReference}
     */
    public static ContainerReference container() {
        return new ContainerReference(null);
    }

}
