/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the input schema definition for a time series forecast request.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * InputSchema schema = InputSchema.builder()
 *     .timestampColumn("date")
 *     .addIdColumn("ID1")
 *     .build();
 * }</pre>
 */
public final class InputSchema {
    private final String timestampColumn;
    private final List<String> idColumns;
    private final String freq;
    private final List<String> targetColumns;

    /**
     * Constructs an InputSchema instance using the provided builder.
     *
     * @param builder the builder instance
     */
    private InputSchema(Builder builder) {
        timestampColumn = requireNonNull(builder.timestampColumn, "The timestampColumn must be provided");
        idColumns = builder.idColumns.isEmpty() ? null : builder.idColumns;
        freq = builder.freq;
        targetColumns = builder.targetColumns.isEmpty() ? null : builder.targetColumns;
    }

    public String timestampColumn() {
        return timestampColumn;
    }

    public List<String> idColumns() {
        return idColumns;
    }


    public String freq() {
        return freq;
    }

    public List<String> targetColumns() {
        return targetColumns;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * InputSchema schema = InputSchema.builder()
     *     .timestampColumn("date")
     *     .addIdColumn("ID1")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder class for constructing InputSchema instances.
     */
    public final static class Builder {
        private String timestampColumn;
        private List<String> idColumns;
        private String freq;
        private List<String> targetColumns;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        protected Builder() {
            this.idColumns = new ArrayList<>();
            this.targetColumns = new ArrayList<>();
        }

        /**
         * Sets the name of the column to be treated as the timestamp.
         * <p>
         * Although not strictly required, it is strongly recommended to use an ISO 8601 format with a UTC offset (e.g.,
         * {@code 2024-10-18T01:09:21.454746+00:00}) to avoid ambiguities such as those caused by daylight saving time.
         *
         * @param timestampColumn the timestamp column name (1–100 characters, must match {@code ^\S.*\S$|^\S$})
         * @return {@code Builder} instance for method chaining
         */
        public Builder timestampColumn(String timestampColumn) {
            this.timestampColumn = timestampColumn;
            return this;
        }

        /**
         * Adds the list of columns that define a unique key for each time series. This acts like a compound primary key in a database table.
         *
         * @param idColumns list of id column names (max 10 items, each 0–100 characters)
         * @return {@code Builder} instance for method chaining
         */
        public Builder idColumns(List<String> idColumns) {
            idColumns = requireNonNullElse(idColumns, new ArrayList<>());
            this.idColumns = requireNonNullElse(idColumns, this.idColumns);
            return this;
        }

        /**
         * Adds the list of columns that define a unique key for each time series. This acts like a compound primary key in a database table.
         *
         * @param idColumns list of id column names (max 10 items, each 0–100 characters)
         * @return {@code Builder} instance for method chaining
         */
        public Builder idColumns(String... idColumns) {
            return idColumns(new ArrayList<>(List.of(idColumns)));
        }

        /**
         * Adds a id column to the list.
         *
         * @param idColumn the name of the id column
         * @return {@code Builder} instance for method chaining
         */
        public Builder addIdColumn(String idColumn) {
            idColumns.add(idColumn);
            return this;
        }

        /**
         * Sets the frequency of the time series data for the given timestamp column. If not provided, we will attempt to infer it from the data.
         *
         * @param freq a valid frequency string (max 100 characters, must match {@code ^\d*\.?\d*(B|D|W|M|Q|Y|h|min|s|ms|us|ns)?$})
         * @return {@code Builder} instance for method chaining
         * @see https://pandas.pydata.org/pandas-docs/stable/user_guide/timeseries.html#period-aliases for a description of the allowed values.
         */
        public Builder freq(String freq) {
            this.freq = freq;
            return this;
        }

        /**
         * Adds the names of the target columns—variables to be forecasted by the model.
         *
         * @param targetColumns list of target column names (max 500 items, each 0–100 characters)
         * @return {@code Builder} instance for method chaining
         */
        public Builder targetColumns(List<String> targetColumns) {
            targetColumns = requireNonNullElse(targetColumns, new ArrayList<>());
            this.targetColumns.addAll(targetColumns);
            return this;
        }

        /**
         * Adds the names of the target columns—variables to be forecasted by the model.
         *
         * @param targetColumns list of target column names (max 500 items, each 0–100 characters)
         * @return {@code Builder} instance for method chaining
         */
        public Builder targetColumns(String... targetColumns) {
            return targetColumns(new ArrayList<>(List.of(targetColumns)));
        }

        /**
         * Adds a target column to the list.
         *
         * @param targetColumn the name of the target column
         * @return {@code Builder} instance for method chaining
         */
        public Builder addTargetColumn(String targetColumn) {
            targetColumns.add(targetColumn);
            return this;
        }

        /**
         * Builds and returns an InputSchema instance.
         *
         * @return {@link InputSchema} instance
         */
        public InputSchema build() {
            return new InputSchema(this);
        }
    }
}