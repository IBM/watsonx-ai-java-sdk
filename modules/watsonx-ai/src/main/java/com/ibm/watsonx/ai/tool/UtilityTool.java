/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import java.util.Map;
import com.ibm.watsonx.ai.core.Experimental;

/**
 * Represents a utility tool resource.
 * <p>
 * This class holds metadata and JSON schemas for configuring and running a tool.
 *
 * @param name The name of the tool.
 * @param description A plain text description of what the tool is used for.
 * @param agentDescription Instruction for LLM agents that can be used as system prompt.
 * @param inputSchema JSON schema describing the input payload accepted when invoking the tool.
 * @param configSchema JSON schema describing configuration parameters accepted by the tool.
 */
@Experimental
public record UtilityTool(
    String name,
    String description,
    String agentDescription,
    Map<String, Object> inputSchema,
    Map<String, Object> configSchema) {}

