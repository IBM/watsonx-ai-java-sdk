package com.ibm.watsonx.runtime;

import com.ibm.watsonx.runtime.chat.model.ChatParameters;
import com.ibm.watsonx.runtime.embedding.EmbeddingParameters;

/**
 * This is an abstract class that contains the shared parameters for various Builder configurations.
 * <p>
 * It serves as a base class for different types of parameters used in the {@code watsonx.ai} platform.
 *
 * @see ChatParameters
 * @see EmbeddingParameters
 */
public abstract class WatsonxParameters {
    protected final String projectId;
    protected final String spaceId;
    protected final String modelId;

    public WatsonxParameters(Builder<?> builder) {
        projectId = builder.projectId;
        spaceId = builder.spaceId;
        modelId = builder.modelId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getModelId() {
        return modelId;
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String projectId;
        private String spaceId;
        private String modelId;

        /**
         * Sets the default project id to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * 
         * @param spaceId Project id value
         */
        public T projectId(String projectId) {
            this.projectId = projectId;
            return (T) this;
        }

        /**
         * Sets the default space id to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * 
         * @param spaceId Space id value
         */
        public T spaceId(String spaceId) {
            this.spaceId = spaceId;
            return (T) this;
        }

        /**
         * Sets the default model to be used for chat completions.
         * <p>
         * If you want to override this value, use the {@link ChatParameters}.
         * <p>
         * For a full list of available model ids, see the
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx">link</a>.
         *
         * @param modelId the model identifier to use
         * @return the builder instance
         */
        public T modelId(String modelId) {
            this.modelId = modelId;
            return (T) this;
        }
    }
}
