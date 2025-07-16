/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents a paginated response from the Foundation Model API.
 * <p>
 * This response includes metadata about pagination and a list of resources (e.g., model tasks).
 *
 * @param <T> The type of the resources contained in the response (e.g., a list of {@link FoundationModel} or {@link FoundationModelTask}).
 *
 * @param limit The number of items returned per page. Must be between 1 and 200.
 * @param first Pagination metadata pointing to the first item in the current page.
 * @param totalCount The total number of available resources matching the request.
 * @param next Pagination metadata pointing to the first item of the next page, if available.
 * @param resources The list of returned resources (e.g., tasks or models).
 *
 * @see FoundationModel
 * @see FoundationModelTask
 */
public record FoundationModelResponse<T>(Integer limit, Pagination first, Integer totalCount, Pagination next, List<T> resources) {

    /**
     * Represents a hyperlink reference to a specific point in pagination.
     *
     * @param href The full URI of the referenced page (e.g., first or next).
     */
    public record Pagination(String href) {

        /**
         * Extracts the {@code limit} value from the pagination href.
         *
         * @return An optional integer representing the limit, or empty if not found.
         */
        public Optional<Integer> limit() {
            return extractValue("limit=(\\d+)");
        }

        /**
         * Extracts the {@code start} value from the pagination href.
         *
         * @return An optional integer representing the start index, or empty if not found.
         */
        public Optional<Integer> start() {
            return extractValue("start=(\\d+)");
        }

        /**
         * Extracts a specified value from the pagination href using a regular expression.
         *
         * @param regex The regular expression pattern to match.
         * @return An optional integer, or empty if no match is found.
         */
        private Optional<Integer> extractValue(String regex) {
            var matcher = Pattern.compile(regex).matcher(href);
            if (matcher.find()) {
                return Optional.of(Integer.valueOf(matcher.group(1)));
            } else
                return Optional.empty();
        }
    }
}
