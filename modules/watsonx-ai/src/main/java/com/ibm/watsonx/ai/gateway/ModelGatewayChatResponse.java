/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import com.ibm.watsonx.ai.chat.TextChatResponse;

/**
 * Response returned by the model gateway chat endpoint.
 */
public class ModelGatewayChatResponse extends TextChatResponse {

    private final String serviceTier;
    private final String systemFingerprint;
    private final Boolean cached;

    protected ModelGatewayChatResponse(Builder<?> builder) {
        super(builder);
        serviceTier = builder.serviceTier;
        systemFingerprint = builder.systemFingerprint;
        cached = builder.cached;
    }

    /**
     * Returns the service tier used to process the request.
     *
     * @return the service tier
     */
    public String serviceTier() {
        return serviceTier;
    }

    /**
     * Returns the backend system fingerprint, useful for detecting backend changes that may affect determinism.
     *
     * @return the system fingerprint
     */
    public String systemFingerprint() {
        return systemFingerprint;
    }

    /**
     * Returns whether the response was served from cache.
     *
     * @return {@code true} if the response was cached
     */
    public Boolean cached() {
        return cached;
    }

    /**
     * Creates a builder initialized with the current state of this {@code ModelGatewayChatResponse}.
     *
     * @return a new {@link Builder} instance
     */
    @Override
    public Builder<?> toBuilder() {
        return new Builder<>()
            .id(this.id())
            .object(this.object())
            .model(this.model())
            .choices(this.choices())
            .created(this.created())
            .usage(this.usage())
            .extractionTags(this.extractionTags())
            .modelId(this.modelId())
            .modelVersion(this.modelVersion())
            .createdAt(this.createdAt())
            .moderations(this.moderations())
            .detections(this.detections())
            .serviceTier(this.serviceTier)
            .systemFingerprint(this.systemFingerprint)
            .cached(this.cached);
    }

    /**
     * Returns a new {@link Builder} instance for {@link ModelGatewayChatResponse}.
     *
     * @return a new {@link Builder}
     */
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Builder for constructing {@link ModelGatewayChatResponse} instances.
     *
     * @param <B> the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder<B>> extends TextChatResponse.Builder<B> {

        /** Creates a new {@code Builder}. */
        public Builder() {}

        private String serviceTier;
        private String systemFingerprint;
        private Boolean cached;

        /**
         * Sets the service tier.
         *
         * @param serviceTier the service tier string
         */
        public B serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return (B) this;
        }

        /**
         * Sets the system fingerprint.
         *
         * @param systemFingerprint the backend fingerprint string
         */
        public B systemFingerprint(String systemFingerprint) {
            this.systemFingerprint = systemFingerprint;
            return (B) this;
        }

        /**
         * Sets whether the response was served from cache.
         *
         * @param cached the cached flag
         */
        public B cached(Boolean cached) {
            this.cached = cached;
            return (B) this;
        }

        /**
         * Builds a {@link ModelGatewayChatResponse} instance.
         *
         * @return a new {@link ModelGatewayChatResponse}
         */
        @Override
        public ModelGatewayChatResponse build() {
            return new ModelGatewayChatResponse(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((serviceTier == null) ? 0 : serviceTier.hashCode());
        result = prime * result + ((systemFingerprint == null) ? 0 : systemFingerprint.hashCode());
        result = prime * result + ((cached == null) ? 0 : cached.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        ModelGatewayChatResponse other = (ModelGatewayChatResponse) obj;
        if (serviceTier == null) {
            if (other.serviceTier != null)
                return false;
        } else if (!serviceTier.equals(other.serviceTier))
            return false;
        if (systemFingerprint == null) {
            if (other.systemFingerprint != null)
                return false;
        } else if (!systemFingerprint.equals(other.systemFingerprint))
            return false;
        if (cached == null) {
            if (other.cached != null)
                return false;
        } else if (!cached.equals(other.cached))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ModelGatewayChatResponse [id=" + id() + ", object=" + object() + ", model=" + model() + ", modelId=" + modelId()
            + ", modelVersion=" + modelVersion() + ", createdAt=" + createdAt() + ", choices=" + choices() + ", created=" + created()
            + ", usage=" + usage() + ", extractionTags=" + extractionTags() + ", moderations=" + moderations() + ", detections=" + detections()
            + ", serviceTier=" + serviceTier + ", systemFingerprint=" + systemFingerprint + ", cached=" + cached + "]";
    }
}
