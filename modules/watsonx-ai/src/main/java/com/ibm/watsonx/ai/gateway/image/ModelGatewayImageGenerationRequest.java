/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import static java.util.Objects.requireNonNullElse;

/**
 * Payload request for the Model Gateway image generation endpoint.
 *
 * @param model the model identifier
 * @param prompt the text description of the desired image
 * @param background the background transparency of the generated images, or {@code null} if not set
 * @param moderation the content moderation level applied to the generated images, or {@code null} if not set
 * @param n the number of images to generate, or {@code null} if not set
 * @param outputCompression the compression level applied to the generated images, or {@code null} if not set
 * @param outputFormat the file format of the generated images, or {@code null} if not set
 * @param partialImages the number of partial images streamed before the final result, or {@code null} if not set
 * @param quality the quality of the generated images, or {@code null} if not set
 * @param responseFormat the format in which the generated images are returned, or {@code null} if not set
 * @param size the dimensions of the generated images, or {@code null} if not set
 * @param style the visual style of the generated images, or {@code null} if not set
 * @param user a unique identifier representing the end-user, or {@code null} if not set
 */
public record ModelGatewayImageGenerationRequest(
    String model,
    String prompt,
    String background,
    String moderation,
    Integer n,
    Integer outputCompression,
    String outputFormat,
    Integer partialImages,
    String quality,
    String responseFormat,
    String size,
    String style,
    String user) {

    /**
     * Creates the payload for the given model by flattening a request and its parameters.
     *
     * @param model the model identifier
     * @param request the {@link ModelGatewayImageRequest} containing the prompt and optional parameters
     * @return a new {@link ModelGatewayImageGenerationRequest}
     */
    static ModelGatewayImageGenerationRequest of(String model, ModelGatewayImageRequest request) {

        var parameters = requireNonNullElse(request.parameters(), ModelGatewayImageParameters.builder().build());

        return new ModelGatewayImageGenerationRequest(
            model,
            request.prompt(),
            parameters.background(),
            parameters.moderation(),
            parameters.n(),
            parameters.outputCompression(),
            parameters.outputFormat(),
            parameters.partialImages(),
            parameters.quality(),
            parameters.responseFormat(),
            parameters.size(),
            parameters.style(),
            parameters.user());
    }
}
