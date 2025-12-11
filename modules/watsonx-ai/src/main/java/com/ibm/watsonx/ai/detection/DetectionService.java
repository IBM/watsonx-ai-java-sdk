/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
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
 *     .projectId("my-project-id")
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
public class DetectionService extends ProjectService {
    private final DetectionRestClient client;

    private DetectionService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = DetectionRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .build();
    }

    public DetectionResponse<DetectionTextResponse> detect(DetectionTextRequest request) {
        var projectSpace = resolveProjectSpace(request);
        var transactionId = request.transactionId();
        var textDetectionRequest =
            new TextDetectionContentDetectors(request.input(), request.detectors(), projectSpace.projectId(), projectSpace.spaceId());
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
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("my-project-id")
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
