/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents an audio input used in chat interactions.
 * <p>
 * The audio data must be provided as base64-encoded content along with its format.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * Audio audio = new Audio("audio/wav", base64EncodedData);
 * }</pre>
 *
 * @param format the MIME type of the audio (e.g., {@code audio/wav}, {@code audio/mp3})
 * @param data the base64-encoded audio data
 */
public record Audio(String format, String data) {}
