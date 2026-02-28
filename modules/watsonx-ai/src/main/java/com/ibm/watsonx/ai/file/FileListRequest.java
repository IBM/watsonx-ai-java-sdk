/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a request to list files from the watsonx.ai Files APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FileListRequest.builder()
 *     .limit(10)
 *     .purpose(Purpose.BATCH)
 *     .build();
 * }</pre>
 */
public class FileListRequest extends WatsonxParameters {
    private final String after;
    private final Integer limit;
    private final String order;
    private final String purpose;

    private FileListRequest(Builder builder) {
        super(builder);
        after = builder.after;
        limit = builder.limit;
        order = builder.order;
        purpose = builder.purpose;
    }

    /**
     * Returns the cursor for pagination.
     *
     * @return the cursor value for pagination
     */
    public String after() {
        return after;
    }

    /**
     * Returns the maximum number of files to return.
     *
     * @return the page size limit
     */
    public Integer limit() {
        return limit;
    }

    /**
     * Returns the sort order by {@code created_at} timestamp.
     *
     * @return the order value
     */
    public String order() {
        return order;
    }

    /**
     * Returns the purpose filter.
     *
     * @return the purpose value
     */
    public String purpose() {
        return purpose;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FileListRequest.builder()
     *     .limit(10)
     *     .purpose(Purpose.BATCH)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FileListRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String after;
        private Integer limit;
        private String order;
        private String purpose;

        private Builder() {}

        /**
         * Sets the cursor for pagination.
         * <p>
         * Use the last file ID from the previous response to retrieve the next page.
         *
         * @param after the file identifier to use as the pagination cursor
         */
        public Builder after(String after) {
            this.after = after;
            return this;
        }

        /**
         * Sets the maximum number of files to return.
         * <p>
         * Must be between 1 and 10,000. Defaults to {@code 10000}.
         *
         * @param limit the page size limit
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the sort order by {@code created_at} timestamp.
         *
         * @param order the sort order value
         */
        public Builder order(Order order) {
            requireNonNull(order, "order cannot be null");
            return order(order.value());
        }

        /**
         * Sets the sort order by {@code created_at} timestamp.
         *
         * @param order the sort order value
         */
        Builder order(String order) {
            this.order = order;
            return this;
        }

        /**
         * Sets the purpose filter.
         * <p>
         * Only files with the specified purpose will be returned.
         *
         * @param purpose the {@link Purpose} to filter by
         */
        public Builder purpose(Purpose purpose) {
            requireNonNull(purpose, "purpose cannot be null");
            return purpose(purpose.value());
        }

        /**
         * Sets the purpose filter.
         * <p>
         * Only files with the specified purpose will be returned.
         *
         * @param purpose the {@link Purpose} to filter by
         */
        Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        /**
         * Builds a {@link FileListRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link FileListRequest}
         */
        public FileListRequest build() {
            return new FileListRequest(this);
        }
    }
}
