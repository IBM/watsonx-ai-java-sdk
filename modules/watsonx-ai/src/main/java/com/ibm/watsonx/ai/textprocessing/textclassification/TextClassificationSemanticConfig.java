/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

/**
 * Represents the semantic configuration for text classification.
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
 * TextClassificationSemanticConfig.builder()
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
public final class TextClassificationSemanticConfig extends com.ibm.watsonx.ai.textprocessing.SemanticConfig {

    private TextClassificationSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     *
     * TextClassificationSemanticConfig.builder()
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
     * Builder class for constructing {@link TextClassificationSemanticConfig} instance.
     */
    public final static class Builder extends com.ibm.watsonx.ai.textprocessing.SemanticConfig.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link TextClassificationSemanticConfig} instance.
         *
         * @return a new instance of {@link TextClassificationSemanticConfig}
         */
        public TextClassificationSemanticConfig build() {
            return new TextClassificationSemanticConfig(this);
        }
    }
}