/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents a rerank request.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = RerankParameters.builder()
 *     .topN(3)
 *     .returnDocuments(true)
 *     .build();
 *
 * RerankRequest request = RerankRequest.builder()
 *     .query("What is watsonx.ai?")
 *     .inputs(List.of("Document 1", "Document 2", "Document 3"))
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 */
public final class RerankRequest {
    private final String query;
    private final List<String> inputs;
    private final RerankParameters parameters;

    private RerankRequest(Builder builder) {
        query = builder.query;
        inputs = isNull(builder.inputs) ? null : List.copyOf(builder.inputs);
        parameters = builder.parameters;
    }

    /**
     * Returns the query text used for reranking.
     *
     * @return the query text, or {@code null} if not set
     */
    public String query() {
        return query;
    }

    /**
     * Returns the input texts to be reranked.
     *
     * @return the list of input texts, or {@code null} if not set
     */
    public List<String> inputs() {
        return inputs;
    }

    /**
     * Returns the rerank parameters.
     *
     * @return the rerank parameters, or {@code null} if not set
     */
    public RerankParameters parameters() {
        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = RerankParameters.builder()
     *     .topN(3)
     *     .returnDocuments(true)
     *     .build();
     *
     * RerankRequest request = RerankRequest.builder()
     *     .query("What is watsonx.ai?")
     *     .inputs(List.of("Document 1", "Document 2", "Document 3"))
     *     .parameters(parameters)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link RerankRequest} instances.
     */
    public final static class Builder {
        private String query;
        private List<String> inputs;
        private RerankParameters parameters;

        private Builder() {}

        /**
         * Sets the query text used for reranking.
         *
         * @param query the query text
         */
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        /**
         * Sets the input texts for the request, replacing any existing inputs.
         * <p>
         * This method completely overwrites the current list of inputs with the provided values.
         *
         * @param inputs the list of input texts to rerank
         */
        public Builder inputs(List<String> inputs) {
            this.inputs = inputs;
            return this;
        }

        /**
         * Sets the parameters controlling the rerank model behavior.
         *
         * @param parameters an {@link RerankParameters} instance
         */
        public Builder parameters(RerankParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link RerankRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link RerankRequest}
         */
        public RerankRequest build() {
            return new RerankRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
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
        RerankRequest other = (RerankRequest) obj;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        if (inputs == null) {
            if (other.inputs != null)
                return false;
        } else if (!inputs.equals(other.inputs))
            return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RerankRequest [query=" + query + ", inputs=" + inputs + ", parameters=" + parameters + "]";
    }
}
