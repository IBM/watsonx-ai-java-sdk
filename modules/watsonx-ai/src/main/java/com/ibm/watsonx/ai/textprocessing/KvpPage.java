/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;

/**
 * Represents a semantic key-value pair page definition used in schema-based text extraction or classification.
 *
 * @param pageDescription a long-form description of the page corresponding to the schema in the included slices. Typically one or two sentences
 *            providing the model with context about the page layout and content
 * @param slices the list of {@link KvpSlice} instances defining the regions of interest (page slices) for extraction
 *
 * @see TextExtractionService
 * @see TextClassificationService
 */
public record KvpPage(String pageDescription, List<KvpSlice> slices) {

    public KvpPage {
        requireNonNull(pageDescription, "pageDescription cannot be null");
    }

    /**
     * Creates a new {@link KvpPage} instance with the given page description and a variable number of {@link KvpSlice} definitions.
     *
     * @param pageDescription the long-form description of the page
     * @param slices one or more {@link KvpSlice} instances representing page regions containing field mappings
     * @return a new {@link KvpPage} instance
     */
    public static KvpPage of(String pageDescription, KvpSlice... slices) {
        return of(pageDescription, List.of(slices));
    }

    /**
     * Creates a new {@link KvpPage} instance with the given page description and a variable number of {@link KvpSlice} definitions.
     *
     * @param pageDescription the long-form description of the page
     * @param slices one or more {@link KvpSlice} instances representing page regions containing field mappings
     * @return a new {@link KvpPage} instance
     */
    public static KvpPage of(String pageDescription, List<KvpSlice> slices) {
        return new KvpPage(pageDescription, slices);
    }
}
