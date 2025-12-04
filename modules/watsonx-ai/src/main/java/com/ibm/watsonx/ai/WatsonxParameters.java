/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

/**
 * Abstract base class that defines common configuration parameters for watsonx.ai.
 */
public abstract class WatsonxParameters {
    protected final String projectId;
    protected final String spaceId;
    protected final String transactionId;

    protected WatsonxParameters(Builder<?> builder) {
        projectId = builder.projectId;
        spaceId = builder.spaceId;
        transactionId = builder.transactionId;
    }

    /**
     * Returns the project id.
     *
     * @return project id value
     */
    public String projectId() {
        return projectId;
    }

    /**
     * Returns the space id.
     *
     * @return space id value
     */
    public String spaceId() {
        return spaceId;
    }

    /**
     * Returns the transaction id.
     *
     * @return transaction id value
     */
    public String transactionId() {
        return transactionId;
    }

    /**
     * Abstract builder class for constructing {@link WatsonxParameters} instances.
     *
     * @param <T> the type of the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        protected String projectId;
        protected String spaceId;
        protected String transactionId;

        /**
         * Sets the project id.
         *
         * @param projectId Project id value
         */
        public T projectId(String projectId) {
            this.projectId = projectId;
            return (T) this;
        }

        /**
         * Sets the space id.
         *
         * @param spaceId Space id value
         */
        public T spaceId(String spaceId) {
            this.spaceId = spaceId;
            return (T) this;
        }

        /**
         * Sets the transaction id for request tracking.
         *
         * @param transactionId the transaction id.
         */
        public T transactionId(String transactionId) {
            this.transactionId = transactionId;
            return (T) this;
        }
    }

    /**
     * Abstract subclass of {@link WatsonxParameters} that introduces a required {@code modelId}.
     */
    public static abstract class WatsonxModelParameters extends WatsonxParameters {
        protected final String modelId;

        protected WatsonxModelParameters(Builder<?> builder) {
            super(builder);
            modelId = builder.modelId;
        }

        /**
         * Returns the model id.
         *
         * @return the model id value
         */
        public String modelId() {
            return modelId;
        }

        /**
         * Abstract builder class for constructing {@link WatsonxModelParameters} instances.
         *
         * @param <T> the concrete builder subclass
         */
        @SuppressWarnings("unchecked")
        public static abstract class Builder<T extends Builder<T>> extends WatsonxParameters.Builder<T> {
            protected String modelId;

            /**
             * Sets the model id.
             *
             * @param modelId the model identifier to use
             */
            public T modelId(String modelId) {
                this.modelId = modelId;
                return (T) this;
            }
        }
    }
}
