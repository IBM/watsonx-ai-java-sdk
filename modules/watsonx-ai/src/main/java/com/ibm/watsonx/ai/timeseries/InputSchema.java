/*
 * Copyright 2025 IBM Corporation
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

    /**
     * Gets the timestamp column name.
     *
     * @return the timestamp column name
     */
    public String timestampColumn() {
        return timestampColumn;
    }

    /**
     * Gets the list of ID columns.
     *
     * @return the list of ID column names
     */
    public List<String> idColumns() {
        return idColumns;
    }

    /**
     * Gets the frequency of the time series.
     *
     * @return the frequency string
     */
    public String freq() {
        return freq;
    }

    /**
     * Gets the list of target columns.
     *
     * @return the list of target column names
     */
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
         * Sets the list of columns that define a unique key for each time series. This acts like a compound primary key in a database table. Replaces
         * any previously set id columns, use {@link #addIdColumn(String)} to append a single column.
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
         * Sets the list of columns that define a unique key for each time series. This acts like a compound primary key in a database table. Replaces
         * any previously set id columns, use {@link #addIdColumn(String)} to append a single column.
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
         */
        public Builder freq(String freq) {
            this.freq = freq;
            return this;
        }

        /**
         * Sets the names of the target columns-variables to be forecasted by the model. Replaces any previously set target columns; use
         * {@link #addTargetColumn(String)} to append a single column.
         *
         * @param targetColumns list of target column names (max 500 items, each 0–100 characters)
         * @return {@code Builder} instance for method chaining
         */
        public Builder targetColumns(List<String> targetColumns) {
            targetColumns = requireNonNullElse(targetColumns, new ArrayList<>());
            this.targetColumns = requireNonNullElse(targetColumns, this.targetColumns);
            return this;
        }

        /**
         * Sets the names of the target columns-variables to be forecasted by the model. Replaces any previously set target columns; use
         * {@link #addTargetColumn(String)} to append a single column.
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((timestampColumn == null) ? 0 : timestampColumn.hashCode());
        result = prime * result + ((idColumns == null) ? 0 : idColumns.hashCode());
        result = prime * result + ((freq == null) ? 0 : freq.hashCode());
        result = prime * result + ((targetColumns == null) ? 0 : targetColumns.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InputSchema other = (InputSchema) obj;
        if (timestampColumn == null) {
            if (other.timestampColumn != null)
                return false;
        } else if (!timestampColumn.equals(other.timestampColumn))
            return false;
        if (idColumns == null) {
            if (other.idColumns != null)
                return false;
        } else if (!idColumns.equals(other.idColumns))
            return false;
        if (freq == null) {
            if (other.freq != null)
                return false;
        } else if (!freq.equals(other.freq))
            return false;
        if (targetColumns == null) {
            if (other.targetColumns != null)
                return false;
        } else if (!targetColumns.equals(other.targetColumns))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "InputSchema [timestampColumn=" + timestampColumn + ", idColumns=" + idColumns + ", freq=" + freq + ", targetColumns="
            + targetColumns + "]";
    }
}