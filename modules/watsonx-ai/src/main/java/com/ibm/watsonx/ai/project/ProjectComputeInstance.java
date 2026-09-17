/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single compute instance entry within a {@link Project} entity.
 *
 * @param name the compute instance name
 * @param guid the compute instance GUID
 * @param type the compute instance type
 * @param crn the CRN of the compute instance
 * @param credentials the credentials map for this compute instance
 */
public record ProjectComputeInstance(String name, String guid, String type, String crn, Map<String, Object> credentials) {

    public ProjectComputeInstance {
        credentials = credentials == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(credentials));
    }
}
