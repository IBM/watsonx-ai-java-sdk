/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.requireNonNull;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;

/**
 * Represents a semantic key-value pair slice within a page schema definition.
 * <p>
 * A {@code KvpSlice} defines a localized region on a document page where key-value fields are expected to appear. Each slice includes:
 * <ul>
 * <li>A mapping of field names to their corresponding {@link KvpFields.KvpField} definitions.</li>
 * <li>A normalized bounding box specifying the slice’s position on the page.</li>
 * </ul>
 *
 * <p>
 * This record is typically used within a {@link KvpPage} as part of a fixed custom schema, where the layout and content placement on the page are
 * consistent.
 *
 * @param fields a mapping of short-form field names to their {@link KvpFields.KvpField} definitions, each entry represents a key-value field to
 *            extract within this slice
 * @param normalizedBbox the normalized bounding box representing the slice location on the page, expressed as a list of four doubles in the format
 *            {@code [left, top, right, bottom]}, where values are percentages from 0.0 to 100.0
 */
public record KvpSlice(Map<String, KvpField> fields, List<Double> normalizedBbox) {

    public KvpSlice {
        requireNonNull(fields, "fields cannot be null");
        requireNonNull(normalizedBbox, "normalizedBbox cannot be null");
    }

    public static KvpSlice of(KvpFields fields, List<Double> normalizedBbox) {
        requireNonNull(fields, "fields cannot be null");
        return new KvpSlice(fields.fields(), normalizedBbox);
    }
}
