/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.WatsonxService.ScopedService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service class to interact with IBM watsonx.ai Text Detection APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DetectionService detectionService = DetectionService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("project-id")
 *     .build();
 *
 * DetectionResponse<DetectionTextResponse> response = detectionService.detect(
 *     DetectionTextRequest.builder()
 *         .input("...")
 *         .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3).build())
 *         .build()
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class DetectionService extends ScopedService {
    private final DetectionRestClient client;
    private final String serviceInstanceId;

    private DetectionService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        serviceInstanceId = builder.serviceInstanceId;
        client = DetectionRestClient.builder()
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
     * Performs content detection on the provided text using the specified detectors.
     *
     * @param request the detection request containing input text and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public DetectionResponse<DetectionTextResponse> detect(DetectionTextRequest request) {
        requireNonNull(request, "request cannot be null");
        var transactionId = request.transactionId();
        var serviceInstanceId = resolveServiceInstanceId(request.serviceInstanceId());
        String projectId = null;
        String spaceId = null;
        if (isNull(serviceInstanceId)) {
            var projectSpace = resolveProjectSpace(request);
            projectId = projectSpace.projectId();
            spaceId = projectSpace.spaceId();
        }
        var textDetectionRequest = new TextDetectionContentDetectors(request.input(), request.detectors(), projectId, spaceId);
        return client.detect(transactionId, serviceInstanceId, textDetectionRequest);
    }

    /**
     * Performs context-based detection on the provided input text against context documents.
     * <p>
     * Supports context relevance and faithfulness (groundedness) detection.
     *
     * @param request the detection request containing input text, context documents, and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public DetectionResponse<DetectionContextResponse> detectContext(DetectionContextRequest request) {
        requireNonNull(request, "request cannot be null");
        var transactionId = request.transactionId();
        var serviceInstanceId = resolveServiceInstanceId(request.serviceInstanceId());
        String projectId = null;
        String spaceId = null;
        if (isNull(serviceInstanceId)) {
            var projectSpace = resolveProjectSpace(request);
            projectId = projectSpace.projectId();
            spaceId = projectSpace.spaceId();
        }
        var contextDetectionRequest =
            new TextDetectionContextDetectors(request.input(), request.context(), request.detectors(), projectId, spaceId);
        return client.detectContext(transactionId, serviceInstanceId, contextDetectionRequest);
    }

    /**
     * Performs detection on a prompt and its generated text.
     * <p>
     * Supports answer relevance detection.
     *
     * @param request the detection request containing a prompt, generated text, and detector configurations
     * @return a {@link DetectionResponse} containing the detection results
     */
    public DetectionResponse<DetectionGeneratedResponse> detectGenerated(DetectionGeneratedRequest request) {
        requireNonNull(request, "request cannot be null");
        var transactionId = request.transactionId();
        var serviceInstanceId = resolveServiceInstanceId(request.serviceInstanceId());
        String projectId = null;
        String spaceId = null;
        if (isNull(serviceInstanceId)) {
            var projectSpace = resolveProjectSpace(request);
            projectId = projectSpace.projectId();
            spaceId = projectSpace.spaceId();
        }
        var generatedDetectionRequest =
            new TextDetectionGeneratedDetectors(request.prompt(), request.generatedText(), request.detectors(), projectId, spaceId);
        return client.detectGenerated(transactionId, serviceInstanceId, generatedDetectionRequest);
    }

    /**
     * Resolves the {@code serviceInstanceId} from the request, falling back to the default configured on this service.
     *
     * @param requestServiceInstanceId the per-request override, or {@code null} if not set
     * @return the resolved service instance id, or {@code null} if neither the request nor the service defines one
     */
    private String resolveServiceInstanceId(String requestServiceInstanceId) {
        return nonNull(requestServiceInstanceId) ? requestServiceInstanceId : serviceInstanceId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DetectionService detectionService = DetectionService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("project-id")
     *     .build();
     *
     * DetectionResponse<DetectionTextResponse> response = detectionService.detect(
     *     DetectionTextRequest.builder()
     *         .input("...")
     *         .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3).build())
     *         .build()
     * );
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DetectionService} instances with configurable parameters.
     */
    public final static class Builder extends ScopedService.Builder<Builder> {
        private String serviceInstanceId;

        private Builder() {}

        /**
         * Sets the watsonx.governance service instance id.
         *
         * @param serviceInstanceId the default service instance id
         */
        public Builder governanceInstanceId(String serviceInstanceId) {
            this.serviceInstanceId = serviceInstanceId;
            return this;
        }

        /**
         * Builds a {@link DetectionService} instance using the configured parameters.
         *
         * @return a new instance of {@link DetectionService}
         */
        public DetectionService build() {
            return new DetectionService(this);
        }
    }
}
