/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.List;
import java.util.Map;

/**
 * Represents a request for context-based text detection.
 */
public final class TextDetectionContextDetectors extends BaseDetectionRequest {
    private static final String CONTEXT_TYPE_DOCS = "docs";

    private final String input;
    private final String contextType;
    private final List<String> context;

    public TextDetectionContextDetectors(String input, List<String> context, Map<String, Map<String, Object>> detectors, String projectId,
        String spaceId) {
        super(detectors, projectId, spaceId);
        this.input = input;
        this.contextType = CONTEXT_TYPE_DOCS;
        this.context = context == null ? null : List.copyOf(context);
    }

    /**
     * Returns the input text to analyze.
     *
     * @return the input text
     */
    public String input() {
        return input;
    }

    /**
     * Returns the context type.
     *
     * @return the context type
     */
    public String contextType() {
        return contextType;
    }

    /**
     * Returns the context documents.
     *
     * @return the list of context document URLs
     */
    public List<String> context() {
        return context;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + ((contextType == null) ? 0 : contextType.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        TextDetectionContextDetectors other = (TextDetectionContextDetectors) obj;
        if (input == null) {
            if (other.input != null)
                return false;
        } else if (!input.equals(other.input))
            return false;
        if (contextType == null) {
            if (other.contextType != null)
                return false;
        } else if (!contextType.equals(other.contextType))
            return false;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextDetectionContextDetectors [detectors=" + detectors() + ", projectId=" + projectId() + ", spaceId=" + spaceId() + ", input="
            + input + ", contextType=" + contextType + ", context=" + context + "]";
    }
}
