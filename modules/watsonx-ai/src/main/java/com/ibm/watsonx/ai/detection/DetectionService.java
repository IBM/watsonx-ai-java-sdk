/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

/**
 * Service class to interact with IBM watsonx.ai Text Detection APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DetectionService detectionService = DetectionService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
 *     .projectId("my-project-id")
 *     .build();
 *
 * DetectionResponse<DetectionTextResponse> response = detectionService.detect(
 *     DetectionTextRequest.builder()
 *         .input("...")
 *         .detectors(Pii.create(), Hap.builder().threshold(0.3).build())
 *         .build()
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public final class DetectionService extends ProjectService {

    private final DetectionRestClient client;

    private DetectionService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
        client = DetectionRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.getAuthenticationProvider())
            .build();
    }

    public DetectionResponse<DetectionTextResponse> detect(DetectionTextRequest request) {
        var projectSpace = resolveProjectSpace(request);
        var transactionId = request.getTransactionId();
        var textDetectionRequest =
            new TextDetectionContentDetectors(request.getInput(), request.getDetectors(), projectSpace.projectId(), projectSpace.spaceId());
        return client.detect(transactionId, textDetectionRequest);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DetectionService detectionService = DetectionService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
     *     .projectId("my-project-id")
     *     .build();
     *
     * DetectionResponse<DetectionTextResponse> response = detectionService.detect(
     *     DetectionTextRequest.builder()
     *         .input("...")
     *         .detectors(Pii.create(), Hap.builder().threshold(0.3).build())
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
    public final static class Builder extends ProjectService.Builder<Builder> {

        private Builder() {}

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
