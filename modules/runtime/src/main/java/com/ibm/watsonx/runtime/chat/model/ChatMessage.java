/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.chat.model;

/**
 * Represents a generic message exchanged during a chat interaction.
 *
 * @see AssistantMessage
 * @see ControlMessage
 * @see SystemMessage
 * @see ToolMessage
 * @see UserMessage
 */
public sealed interface ChatMessage permits AssistantMessage, ControlMessage, SystemMessage, ToolMessage, UserMessage {}

