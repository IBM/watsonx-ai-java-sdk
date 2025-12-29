/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

/**
 * Represents a single foundation‑model task supported by the catalog.
 *
 * @param taskId The unique identifier of the task (e.g., {@code "summarization"}).
 * @param label A human‑readable label for UI display (e.g., {@code "Summarization"}).
 * @param rank An integer rank used primarily for UI ordering (e.g., {@code 1}).
 * @param description A brief description of the task’s purpose and capabilities.
 */
public record FoundationModelTask(
    String taskId,
    String label,
    Integer rank,
    String description) {}
