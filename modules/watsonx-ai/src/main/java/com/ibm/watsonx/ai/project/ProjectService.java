/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import static java.util.Objects.requireNonNull;
import java.util.Optional;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for retrieving project metadata via the Projects API.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ProjectService service = ProjectService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .build();
 *
 * Project project = service.findProject("...").orElseThrow();
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ProjectService extends WatsonxService {

    private final ProjectRestClient client;

    // Required by CDI for proxy / bean instantiation
    protected ProjectService() {
        super();
        client = null;
    }

    private ProjectService(Builder builder) {
        super(builder);
        client = ProjectRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .requestLogger(requestLogger, requestLogLevel)
            .responseLogger(responseLogger, responseLogLevel)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Finds the project with the given ID.
     *
     * @param projectId The project UUID.
     * @return An {@link Optional} containing the {@link Project} if found, or {@link Optional#empty()} if not found.
     */
    public Optional<Project> findProject(String projectId) {
        requireNonNull(projectId, "projectId must be provided");
        return client.findProject(projectId);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return A new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ProjectService} instances.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {

        private Builder() {}

        @Override
        public Builder baseUrl(CloudRegion url) {
            return super.baseUrl(url.wxEndpoint().replace("/wx", ""));
        }

        /**
         * Builds a {@link ProjectService} instance using the configured parameters.
         *
         * @return A new instance of {@link ProjectService}.
         */
        public ProjectService build() {
            return new ProjectService(this);
        }
    }
}
