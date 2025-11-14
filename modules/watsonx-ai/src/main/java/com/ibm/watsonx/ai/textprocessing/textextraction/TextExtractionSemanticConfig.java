/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for text extraction.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * KvpFields fields = KvpFields.builder()
 *     .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
 *     .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
 *     .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
 *     .build();
 *
 * TextExtractionSemanticConfig.builder()
 *     .schemasMergeStrategy("replace")
 *     .schemas(
 *         Schema.builder()
 *             .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
 *             .documentType("Invoice")
 *             .fields(fields)
 *             .build()
 *     ).build();
 * }</pre>
 */
public final class TextExtractionSemanticConfig extends SemanticConfig {

    private TextExtractionSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     *
     * TextExtractionSemanticConfig.builder()
     *     .schemasMergeStrategy("replace")
     *     .schemas(
     *         Schema.builder()
     *             .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
     *             .documentType("Invoice")
     *             .fields(...)
     *             .build()
     *     ).build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionSemanticConfig} instance.
     */
    public final static class Builder extends SemanticConfig.Builder<Builder> {

        /**
         * Builds a {@link TextExtractionSemanticConfig} instance.
         *
         * @return a new instance of {@link TextExtractionSemanticConfig}
         */
        public TextExtractionSemanticConfig build() {
            return new TextExtractionSemanticConfig(this);
        }
    }
}
