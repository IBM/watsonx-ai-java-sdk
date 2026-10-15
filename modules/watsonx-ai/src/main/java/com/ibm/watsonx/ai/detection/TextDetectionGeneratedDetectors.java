/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.Map;

/**
 * Represents a request for generated text detection.
 */
public final class TextDetectionGeneratedDetectors extends BaseDetectionRequest {
    private final String prompt;
    private final String generatedText;

    public TextDetectionGeneratedDetectors(String prompt, String generatedText, Map<String, Map<String, Object>> detectors, String projectId,
        String spaceId) {
        super(detectors, projectId, spaceId);
        this.prompt = prompt;
        this.generatedText = generatedText;
    }

    /**
     * Returns the text prompt.
     *
     * @return the prompt
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Returns the generated text.
     *
     * @return the generated text
     */
    public String generatedText() {
        return generatedText;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((prompt == null) ? 0 : prompt.hashCode());
        result = prime * result + ((generatedText == null) ? 0 : generatedText.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        TextDetectionGeneratedDetectors other = (TextDetectionGeneratedDetectors) obj;
        if (prompt == null) {
            if (other.prompt != null)
                return false;
        } else if (!prompt.equals(other.prompt))
            return false;
        if (generatedText == null) {
            if (other.generatedText != null)
                return false;
        } else if (!generatedText.equals(other.generatedText))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextDetectionGeneratedDetectors [detectors=" + detectors() + ", projectId=" + projectId() + ", spaceId=" + spaceId() + ", prompt="
            + prompt + ", generatedText=" + generatedText + "]";
    }
}
