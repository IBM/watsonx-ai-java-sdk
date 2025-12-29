/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel.filter;

import static java.util.Objects.requireNonNull;
import java.util.List;
import java.util.StringJoiner;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;

/**
 * The {@code Filter} class provides a way to construct filter expressions used to query models within a {@link FoundationModelService}.
 * <p>
 * Filter expressions support logical composition using {@code and} and {@code or} operators. Filters are built from one or more
 * {@link FilterExpression} instances that match model attributes such as {@code modelId}, {@code provider}, {@code tier}, {@code function},
 * {@code task}, etc.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.*;
 *
 * // Builds a filter expression that matches all models with the rerank function,
 * // excluding any models that also support the embedding function.
 * var filter = Filter.and(
 *     function("rerank"),
 *     not(Expression.function("embedding"))
 * );
 * }</pre>
 *
 * @see Expression
 */
public final class Filter {

    private final StringJoiner filterExpression;

    private Filter(String logicalOperator, List<FilterExpression> expressions) {
        filterExpression = new StringJoiner(",", "", logicalOperator);
        expressions.stream()
            .map(FilterExpression::expression)
            .forEach(filterExpression::add);
    }

    /**
     * Creates a Filter using {@code and} logical operator.
     *
     * @param expressions The filter expressions to combine with {@code and}.
     * @return A new Filter object.
     */
    public static Filter and(FilterExpression... expressions) {
        requireNonNull(expressions, "The expressions cannot be null");
        return new Filter(":and", List.of(expressions));
    }

    /**
     * Creates a Filter using {@code or} logical operator.
     *
     * @param expressions The filter expressions to combine with {@code or}.
     * @return A new Filter object.
     */
    public static Filter or(FilterExpression... expressions) {
        requireNonNull(expressions, "The expressions cannot be null");
        return new Filter(":or", List.of(expressions));
    }

    /**
     * Creates a Filter using the default logical operator ({@code and}).
     *
     * @param expressions The filter expressions to combine with {@code and}.
     * @return A new Filter object.
     */
    public static Filter of(FilterExpression... expressions) {
        requireNonNull(expressions, "The expressions cannot be null");
        return new Filter("", List.of(expressions));
    }

    /**
     * Converts the {@link Filter} to a string representation.
     *
     * @return a string representation of this filter.
     */
    public String expression() {
        return toString();
    }

    @Override
    public String toString() {
        return filterExpression.toString();
    }

    /**
     * A utility class containing factory methods for creating {@link FilterExpression} instances.
     * <p>
     * These methods correspond to supported filter types such as {@code modelId}, {@code provider}, {@code tier}, {@code task}, {@code lifecycle},
     * {@code source}, and {@code function}. It also provides logical negation using {@code not()}.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.*;
     *
     * // Constructs a filter expression that includes all models which support the summarization task
     * // but explicitly excludes the model with id ibm/granite-13b-instruct-v1.
     * Filter.and(
     *    not(modelId("ibm/granite-13b-instruct-v1")),
     *    task("summarization")
     * );
     * }</pre>
     */
    public static class Expression {

        private Expression() {}

        /**
         * Negates the given {@link FilterExpression}.
         *
         * @param expression the expression to negate.
         * @return a negated expression.
         */
        public static FilterExpression not(FilterExpression expression) {
            requireNonNull(expression, "The expression cannot be null");
            return () -> "!".concat(expression.expression());
        }

        /**
         * Creates a filter expression that matches a specific model id.
         *
         * @param modelId the model id to filter by.
         * @return a {@link FilterExpression} for the specified model id.
         */
        public static FilterExpression modelId(String modelId) {
            requireNonNull(modelId, "The modelId cannot be null");

            if (modelId.isBlank())
                throw new IllegalArgumentException("The modelId cannot be blank");

            return () -> "modelid_".concat(modelId);
        }

        /**
         * Creates a filter expression that matches the given provider.
         *
         * @param provider the provider name to filter by.
         * @return a {@link FilterExpression} for the specified provider.
         */
        public static FilterExpression provider(String provider) {
            requireNonNull(provider, "The provider cannot be null");

            if (provider.isBlank())
                throw new IllegalArgumentException("The provider cannot be blank");

            return () -> "provider_".concat(provider);
        }

        /**
         * Creates a filter expression that matches a specific source.
         *
         * @param source the source name to filter by.
         * @return a {@link FilterExpression} for the specified source.
         */
        public static FilterExpression source(String source) {
            requireNonNull(source, "The source cannot be null");

            if (source.isBlank())
                throw new IllegalArgumentException("The source cannot be blank");

            return () -> "source_".concat(source);
        }

        /**
         * Creates a filter expression that matches a given input tier.
         *
         * @param inputTier the input tier to filter by.
         * @return a {@link FilterExpression} for the specified input tier.
         */
        public static FilterExpression inputTier(String inputTier) {
            requireNonNull(inputTier, "The inputTier cannot be null");

            if (inputTier.isBlank())
                throw new IllegalArgumentException("The inputTier cannot be blank");

            return () -> "input_tier_".concat(inputTier);
        }

        /**
         * Creates a filter expression that matches a given output or input tier.
         *
         * @param tier the tier to filter by.
         * @return a {@link FilterExpression} for the specified tier.
         */
        public static FilterExpression tier(String tier) {
            requireNonNull(tier, "The tier cannot be null");

            if (tier.isBlank())
                throw new IllegalArgumentException("The tier cannot be blank");

            return () -> "tier_".concat(tier);
        }

        /**
         * Creates a filter expression that matches a specific task.
         *
         * @param task the task to filter by.
         * @return a {@link FilterExpression} for the specified task.
         */
        public static FilterExpression task(String task) {
            requireNonNull(task, "The task cannot be null");

            if (task.isBlank())
                throw new IllegalArgumentException("The task cannot be blank");

            return () -> "task_".concat(task);
        }

        /**
         * Creates a filter expression that matches a specific lifecycle state.
         *
         * @param lifecycle the lifecycle state to filter by.
         * @return a {@link FilterExpression} for the specified lifecycle state.
         */
        public static FilterExpression lifecycle(String lifecycle) {
            requireNonNull(lifecycle, "The lifecycle cannot be null");

            if (lifecycle.isBlank())
                throw new IllegalArgumentException("The lifecycle cannot be blank");

            return () -> "lifecycle_".concat(lifecycle);
        }

        /**
         * Creates a filter expression that matches a specific function.
         *
         * @param function the function name to filter by.
         * @return a {@link FilterExpression} for the specified function.
         * @throws NullPointerException if {@code function} is null.
         */
        public static FilterExpression function(String function) {
            requireNonNull(function, "The function cannot be null");

            if (function.isBlank())
                throw new IllegalArgumentException("The function cannot be blank");

            return () -> "function_".concat(function);
        }
    }
}
