/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

/**
 * Represents the {@code scope} object within a {@link Project} entity.
 *
 * @param bssAccountId the IBM Cloud BSS account ID
 * @param enforceMembers whether membership enforcement is enabled
 * @param samlInstanceName the SAML instance name
 */
public record ProjectScope(String bssAccountId, boolean enforceMembers, String samlInstanceName) {}
