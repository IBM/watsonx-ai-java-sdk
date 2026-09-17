/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

/**
 * Represents the {@code storage} object within a {@link Project} entity.
 *
 * @param type the storage backend type
 * @param guid the GUID of the Cloud Object Storage instance
 * @param properties the storage bucket properties including credentials
 */
public record ProjectStorage(String type, String guid, ProjectStorageProperties properties) {}
