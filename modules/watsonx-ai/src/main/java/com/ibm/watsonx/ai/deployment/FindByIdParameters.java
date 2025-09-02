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
 * FindByIdParameters.builder()
 *     .spaceId("...")
 *     .build();
 * }</pre>
 *
 */
public class FindByIdParameters extends WatsonxParameters {
    private final String deployment;

    public FindByIdParameters(Builder builder) {
        super(builder);
        deployment = builder.deployment;
    }

    public String getDeployment() {
        return deployment;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FindByIdParameters.builder()
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
     * Builder class for constructing {@link FindByIdParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private String deployment;

        private Builder() {}

        public Builder deployment(String deployment) {
            this.deployment = deployment;
            return this;
        }

        public FindByIdParameters build() {
            return new FindByIdParameters(this);
        }
    }
}
