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

    public WatsonxParameters(Builder<?> builder) {
        projectId = builder.projectId;
        spaceId = builder.spaceId;
    }

    /**
     * Returns the project id.
     *
     * @return project id value
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Returns the space id.
     *
     * @return space id value
     */
    public String getSpaceId() {
        return spaceId;
    }

    /**
     * Abstract builder class for constructing {@link WatsonxParameters} instances.
     *
     * @param <T> the type of the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String projectId;
        private String spaceId;

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
    }

    /**
     * Abstract subclass of {@link WatsonxParameters} that introduces a required {@code modelId}.
     */
    public static abstract class WatsonxModelParameters extends WatsonxParameters {
        protected final String modelId;

        public WatsonxModelParameters(Builder<?> builder) {
            super(builder);
            modelId = builder.modelId;
        }

        /**
         * Returns the model id.
         *
         * @return the model id value
         */
        public String getModelId() {
            return modelId;
        }

        /**
         * Abstract builder class for constructing {@link WatsonxModelParameters} instances.
         *
         * @param <T> the concrete builder subclass
         */
        @SuppressWarnings("unchecked")
        public static abstract class Builder<T extends Builder<T>> extends WatsonxParameters.Builder<T> {
            private String modelId;

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
