/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.List;

/**
 * Represents the response returned by the Text Detection API.
 *
 * @param <T> the type of detection results contained in the response
 * @param detections the list of detection results
 */
public record DetectionResponse<T extends BaseDetectionResponse>(List<T> detections) {}
