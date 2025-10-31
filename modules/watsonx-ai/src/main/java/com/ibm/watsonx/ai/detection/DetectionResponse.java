/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.List;

/**
 * Represents the response returned by the Text Detection API.
 */
public record DetectionResponse<T extends BaseDetectionResponse>(List<T> detections) {}
