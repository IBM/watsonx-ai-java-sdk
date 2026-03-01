/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

/**
 * Represents the response returned by the watsonx.ai File delete API.
 *
 * @param id the identifier of the deleted file
 * @param deleted whether the file was successfully deleted
 * @param object the object type, always
 */
public record FileDeleteResponse(String id, boolean deleted, String object) {}
