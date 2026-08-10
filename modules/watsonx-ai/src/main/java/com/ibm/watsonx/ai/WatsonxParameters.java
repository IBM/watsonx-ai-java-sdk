/*
 * Copyright 2025 IBM Corporation
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            WatsonxModelParameters other = (WatsonxModelParameters) obj;
            if (modelId == null) {
                if (other.modelId != null)
                    return false;
            } else if (!modelId.equals(other.modelId))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "WatsonxModelParameters [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", modelId="
                + modelId + "]";
        }
    }

    /**
     * Abstract subclass of {@link WatsonxCryptoParameters} that introduces the {@code crypto} field.
     */
    public static abstract class WatsonxCryptoParameters extends WatsonxModelParameters {
        protected final String crypto;

        protected WatsonxCryptoParameters(Builder<?> builder) {
            super(builder);
            crypto = builder.crypto;
        }

        /**
         * Returns the crypto key reference for encrypting inference requests.
         *
         * @return the crypto key reference identifier
         */
        public String crypto() {
            return crypto;
        }

        /**
         * Abstract builder class for constructing {@link WatsonxCryptoParameters} instances.
         *
         * @param <T> the concrete builder subclass
         */
        @SuppressWarnings("unchecked")
        public static abstract class Builder<T extends Builder<T>> extends WatsonxModelParameters.Builder<T> {
            protected String crypto;

            /**
             * Sets the crypto key reference for encrypting inference requests.
             * <p>
             * The key reference should be an identifier from a keys management service.
             *
             * @param crypto the key reference identifier
             * @see <a href=
             *      "https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-generation.html?context=wx&audience=wdp#inf-encrypt">Encrypting
             *      inference requests</a>
             */
            public T crypto(String crypto) {
                this.crypto = crypto;
                return (T) this;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((crypto == null) ? 0 : crypto.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            WatsonxCryptoParameters other = (WatsonxCryptoParameters) obj;
            if (crypto == null) {
                if (other.crypto != null)
                    return false;
            } else if (!crypto.equals(other.crypto))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "WatsonxCryptoParameters [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + ", modelId="
                + modelId + ", crypto=" + crypto + "]";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((spaceId == null) ? 0 : spaceId.hashCode());
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
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
        WatsonxParameters other = (WatsonxParameters) obj;
        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId))
            return false;
        if (spaceId == null) {
            if (other.spaceId != null)
                return false;
        } else if (!spaceId.equals(other.spaceId))
            return false;
        if (transactionId == null) {
            if (other.transactionId != null)
                return false;
        } else if (!transactionId.equals(other.transactionId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "WatsonxParameters [projectId=" + projectId + ", spaceId=" + spaceId + ", transactionId=" + transactionId + "]";
    }
}
