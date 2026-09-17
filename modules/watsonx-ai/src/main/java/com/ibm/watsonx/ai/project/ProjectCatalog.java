/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

/**
 * Represents the {@code catalog} object within a {@link Project} entity.
 *
 * @param guid the catalog GUID
 * @param publicCatalog whether the catalog is public
 */
public record ProjectCatalog(String guid, boolean publicCatalog) {}
