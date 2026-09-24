/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Represents a reference to a document used as input or output in a text processing operation.
 *
 * @see CosReference
 * @see ContainerReference
 */
public sealed interface DocumentReference permits CosReference, ContainerReference {

    /**
     * Produces a {@link DataReference} suitable for sending to the watsonx.ai API.
     *
     * @param objectName the file path or object key used at call time
     * @return a fully formed {@link DataReference}
     */
    DataReference toDataReference(String objectName);
}
