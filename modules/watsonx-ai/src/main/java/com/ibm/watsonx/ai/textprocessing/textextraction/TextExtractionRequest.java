/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.DataReference;

/**
 * Represents a request for the Text Extraction API.
 *
 * @param projectId the project identifier
 * @param spaceId the space identifier
 * @param documentReference reference to the input document in COS
 * @param resultsReference reference to where results should be stored in COS
 * @param parameters extraction parameters
 * @param custom user-defined custom properties
 */
public record TextExtractionRequest(String projectId, String spaceId, DataReference documentReference, DataReference resultsReference,
    Parameters parameters, Map<String, Object> custom) {}
