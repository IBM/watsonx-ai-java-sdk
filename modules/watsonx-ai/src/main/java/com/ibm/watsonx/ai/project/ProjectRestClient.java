/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx Platform Projects API.
 */
public abstract class ProjectRestClient extends WatsonxRestClient {

    protected ProjectRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Finds the project with the given ID.
     *
     * @param projectId the project UUID
     * @return an {@link Optional} containing the {@link Project} if found, or {@link Optional#empty()} if not found
     */
    public abstract Optional<Project> findProject(String projectId);

    /**
     * Creates a new {@link Builder} using the first available {@link ProjectRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ProjectRestClient.Builder builder() {
        return ServiceLoader.load(ProjectRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ProjectRestClient} instances.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ProjectRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     */
    public interface ProjectRestClientBuilderFactory extends Supplier<ProjectRestClient.Builder> {}
}
