/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.DataReference;

/**
 * Represents a request for the Text Classification API.
 */
public record TextClassificationRequest(String projectId, String spaceId, DataReference documentReference, Parameters parameters,
    Map<String, Object> custom) {}
