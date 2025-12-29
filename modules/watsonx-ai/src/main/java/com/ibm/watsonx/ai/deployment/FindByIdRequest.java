/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to retrieve deployment details.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FindByIdRequest.builder()
 *     .deploymentId("...")
 *     .spaceId("...")
 *     .build();
 * }</pre>
 *
 */
public final class FindByIdRequest extends WatsonxParameters {
    private final String deploymentId;

    private FindByIdRequest(Builder builder) {
        super(builder);
        deploymentId = builder.deploymentId;
    }

    /**
     * Returns the deployment identifier.
     *
     * @return the deployment id
     */
    public String deploymentId() {
        return deploymentId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FindByIdRequest.builder()
     *     .deploymentId("...")
     *     .spaceId("...")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FindByIdRequest} instances.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String deploymentId;

        private Builder() {}

        /**
         * Sets the deployment identifier.
         *
         * @param deploymentId the deployment ID
         */
        public Builder deploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
            return this;
        }

        /**
         * Builds a {@link FindByIdRequest} instance.
         *
         * @return a new instance of {@link FindByIdRequest}
         */
        public FindByIdRequest build() {
            return new FindByIdRequest(this);
        }
    }
}
