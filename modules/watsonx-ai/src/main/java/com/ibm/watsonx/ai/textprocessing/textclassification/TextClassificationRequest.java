/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.DataReference;

/**
 * Represents a request for the Text Classification API.
 *
 * @param projectId the project identifier
 * @param spaceId the space identifier
 * @param documentReference reference to the input document in COS
 * @param parameters classification parameters
 * @param custom user-defined custom properties
 */
public record TextClassificationRequest(String projectId, String spaceId, DataReference documentReference, Parameters parameters,
    Map<String, Object> custom) {}
