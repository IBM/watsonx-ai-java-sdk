/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents a user message content.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * UserMessage.of(
 *     TextContent.of("Tell me more about this image"),
 *     ImageContent.of("image/svg", base64Data)
 * );
 * }</pre>
 *
 * @see TextContent
 * @see ImageContent
 * @see VideoContent
 * @see AudioContent
 */
public sealed interface UserContent permits TextContent, ImageContent, VideoContent, AudioContent {}
