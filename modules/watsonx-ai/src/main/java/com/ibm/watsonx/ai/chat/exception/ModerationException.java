/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;

/**
 * Exception thrown when the moderation system blocked the chat response entirely, leaving no choices in the output.
 */
public final class ModerationException extends RuntimeException {

    private final Map<String, List<ModerationResult>> moderations;

    /**
     * Constructs a new {@code ModerationException} with the given moderation results.
     *
     * @param moderations a map from detector name (e.g. {@code "pii"}, {@code "hap"}) to its list of {@link ModerationResult} entries describing each
     *            flagged span
     */
    public ModerationException(Map<String, List<ModerationResult>> moderations) {
        super("The chat response was blocked by the moderation system (policies triggered: " + String.join(", ", moderations.keySet()) + ")");
        this.moderations = Collections.unmodifiableMap(moderations);
    }

    /**
     * Returns the moderation results that caused this exception, keyed by detector name.
     *
     * @return an unmodifiable map from detector name to its list of {@link ModerationResult} entries, never {@code null}
     */
    public Map<String, List<ModerationResult>> moderations() {
        return moderations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moderations == null) ? 0 : moderations.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ModerationException other = (ModerationException) obj;
        if (moderations == null) {
            if (other.moderations != null)
                return false;
        } else if (!moderations.equals(other.moderations))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ModerationException [moderations=" + moderations + "]";
    }
}
