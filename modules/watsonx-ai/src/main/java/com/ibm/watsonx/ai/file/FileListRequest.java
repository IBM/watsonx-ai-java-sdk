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
public final class FileListRequest extends WatsonxParameters {
    private final String after;
    private final Integer limit;
    private final Order order;
    private final Purpose purpose;

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
     * @return the {@link Order}, or {@code null} if not set
     */
    public Order order() {
        return order;
    }

    /**
     * Returns the purpose filter.
     *
     * @return the {@link Purpose}, or {@code null} if not set
     */
    public Purpose purpose() {
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
        private Order order;
        private Purpose purpose;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((after == null) ? 0 : after.hashCode());
        result = prime * result + ((limit == null) ? 0 : limit.hashCode());
        result = prime * result + ((order == null) ? 0 : order.hashCode());
        result = prime * result + ((purpose == null) ? 0 : purpose.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        FileListRequest other = (FileListRequest) obj;
        if (after == null) {
            if (other.after != null)
                return false;
        } else if (!after.equals(other.after))
            return false;
        if (limit == null) {
            if (other.limit != null)
                return false;
        } else if (!limit.equals(other.limit))
            return false;
        if (order != other.order)
            return false;
        if (purpose != other.purpose)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FileListRequest [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", after=" + after
            + ", limit=" + limit + ", order=" + order + ", purpose=" + purpose + "]";
    }
}
