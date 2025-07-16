/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static java.util.Objects.requireNonNullElse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A data structure for building and storing columnar forecast data, where each key corresponds to a column name and maps to a list of values.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var data = ForecastData.create()
 *     .add("date", "2020-01-01T00:00:00")
 *     .add("date", "2020-01-01T01:00:00")
 *     .add("date", "2020-01-05T01:00:00")
 *     .add("ID1", "D1", 3)
 *     .addAll("TARGET1", 1.46, 2.34, 4.55);
 * }</pre>
 *
 */
public final class ForecastData {

    private final Map<String, List<Object>> data;

    /**
     * Constructs an ForecastData instance.
     */
    protected ForecastData(Map<String, List<Object>> data) {
        this.data = requireNonNullElse(data, new LinkedHashMap<>());
    }

    /**
     * Adds a single value to the list for the specified column key.
     *
     * @param key the column name
     * @param value the value to add
     * @return {@code ForecastData} instance for method chaining
     */
    public ForecastData add(String key, Object value) {
        data.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Adds the same value multiple times to the list for the specified column key.
     *
     * @param key the column name
     * @param value the value to repeat
     * @param times the number of times the value should be added
     * @return {@code ForecastData} instance for method chaining
     */
    public ForecastData add(String key, Object value, int times) {
        List<Object> list = data.computeIfAbsent(key, k -> new ArrayList<>());
        IntStream.range(0, times).forEach(i -> list.add(value));
        return this;
    }

    /**
     * Adds multiple values to the list for the specified column key.
     *
     * @param key the column name
     * @param values the collection of values to add
     * @return {@code ForecastData} instance for method chaining
     */
    public ForecastData addAll(String key, Collection<?> values) {
        data.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values);
        return this;
    }

    /**
     * Adds multiple values to the list for the specified column key.
     *
     * @param key the column name
     * @param values the collection of values to add
     * @return {@code ForecastData} instance for method chaining
     */
    public ForecastData addAll(String key, Object... values) {
        return addAll(key, new ArrayList<>(List.of(values)));
    }

    /**
     * Checks if the data contains the specified column key.
     *
     * @param key the column name to check
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * Retrieves the list of values associated with the specified column key.
     *
     * @param key the column name
     * @return the list of values, or null if the key is not present
     */
    public List<Object> get(String key) {
        return data.get(key);
    }

    /**
     * Returns the internal map representing the columnar data.
     *
     * @return a map from column names to lists of values
     */
    public Map<String, List<Object>> asMap() {
        return data;
    }

    /**
     * Creates a new instance of {@code ForecastData}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var data = ForecastData.create()
     *     .add("date", "2020-01-01T00:00:00")
     *     .add("date", "2020-01-01T01:00:00")
     *     .add("date", "2020-01-05T01:00:00")
     *     .add("ID1", "D1", 3)
     *     .addAll("TARGET1", 1.46, 2.34, 4.55);
     * }</pre>
     *
     * @return a new instance of the {@code ForecastData} class
     */
    public static ForecastData create() {
        return new ForecastData(null);
    }

    /**
     * Creates a new instance of {@code ForecastData} from the given map.
     *
     * @param data the columnar data map to wrap
     * @return a new {@code ForecastData} instance wrapping the given map
     */
    public static ForecastData from(Map<String, List<Object>> data) {
        return new ForecastData(data);
    }

    @Override
    public String toString() {
        return "ForecastData [data=" + data + "]";
    }
}
