/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of the retrieve deployment details.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
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

        public Builder deploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
            return this;
        }

        public FindByIdRequest build() {
            return new FindByIdRequest(this);
        }
    }
}
