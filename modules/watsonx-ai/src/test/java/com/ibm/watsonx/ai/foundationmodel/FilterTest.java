/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.function;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.inputTier;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.lifecycle;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.not;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.provider;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.source;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.task;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.tier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression;

public class FilterTest {

    @Test
    void should_create_and_filter_with_multiple_expressions() {
        var filter = Filter.and(modelId("testModel"), task("summarization"));
        assertEquals("modelid_testModel,task_summarization:and", filter.toString());
    }

    @Test
    void should_create_or_filter_with_multiple_expressions() {
        var filter = Filter.or(modelId("testModel"), task("summarization"));
        assertEquals("modelid_testModel,task_summarization:or", filter.expression());
    }

    @Test
    void should_create_not_filter_with_expression() {
        var filter = Filter.of(not(modelId("testModel")));
        assertEquals("!modelid_testModel", filter.toString());
    }

    @Test
    void should_throw_exception_when_creating_not_filter_with_null_expression() {
        assertThrows(NullPointerException.class, () -> Expression.not(null));
    }

    @Test
    void should_throw_exception_when_creating_filter_with_empty_value() {
        assertThrows(IllegalArgumentException.class, () -> modelId(""));
        assertThrows(IllegalArgumentException.class, () -> provider(""));
        assertThrows(IllegalArgumentException.class, () -> source(""));
        assertThrows(IllegalArgumentException.class, () -> inputTier(""));
        assertThrows(IllegalArgumentException.class, () -> tier(""));
        assertThrows(IllegalArgumentException.class, () -> task(""));
        assertThrows(IllegalArgumentException.class, () -> lifecycle(""));
        assertThrows(IllegalArgumentException.class, () -> function(""));
    }

    @Test
    void should_create_filter_for_each_expression_type() {
        assertEquals("modelid_test", Filter.of(modelId("test")).toString());
        assertEquals("provider_test", Filter.of(provider("test")).toString());
        assertEquals("source_test", Filter.of(source("test")).toString());
        assertEquals("input_tier_test", Filter.of(inputTier("test")).toString());
        assertEquals("tier_test", Filter.of(tier("test")).toString());
        assertEquals("task_test", Filter.of(task("test")).toString());
        assertEquals("lifecycle_test", Filter.of(lifecycle("test")).toString());
        assertEquals("function_test", Filter.of(function("test")).toString());
    }
}

