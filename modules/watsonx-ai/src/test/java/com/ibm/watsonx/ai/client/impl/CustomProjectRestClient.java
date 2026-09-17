/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.Optional;
import com.ibm.watsonx.ai.project.Project;
import com.ibm.watsonx.ai.project.ProjectRestClient;
import com.ibm.watsonx.ai.project.ProjectRestClient.ProjectRestClientBuilderFactory;

public class CustomProjectRestClient extends ProjectRestClient {

    CustomProjectRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public Optional<Project> findProject(String projectId) {
        throw new UnsupportedOperationException("Unimplemented method 'findProject'");
    }

    public static final class CustomProjectRestClientBuilderFactory
        implements ProjectRestClientBuilderFactory {
        @Override
        public ProjectRestClient.Builder get() {
            return new CustomProjectRestClient.Builder();
        }
    }

    static final class Builder extends ProjectRestClient.Builder {
        @Override
        public ProjectRestClient build() {
            return new CustomProjectRestClient(this);
        }
    }
}
