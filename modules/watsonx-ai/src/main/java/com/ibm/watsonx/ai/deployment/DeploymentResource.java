/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import java.util.List;
import java.util.Map;

/**
 * Represents a deployment resource containing metadata and entity information.
 *
 * @param metadata the resource metadata
 * @param entity the deployment entity details
 */
public record DeploymentResource(ResourceMeta metadata, DeploymentEntity entity) {

    /**
     * Metadata information for a deployment resource.
     *
     * @param id the unique identifier
     * @param createdAt the creation timestamp
     * @param rev the revision identifier
     * @param owner the owner identifier
     * @param modifiedAt the last modification timestamp
     * @param parentId the parent resource identifier
     * @param name the resource name
     * @param description the resource description
     * @param tags the list of tags
     * @param commitInfo the commit information
     * @param spaceId the space identifier
     * @param projectId the project identifier
     */
    public record ResourceMeta(
        String id,
        String createdAt,
        String rev,
        String owner,
        String modifiedAt,
        String parentId,
        String name,
        String description,
        List<String> tags,
        ResourceCommitInfo commitInfo,
        String spaceId,
        String projectId) {}

    /**
     * Commit information for a resource.
     *
     * @param committedAt the commit timestamp
     * @param commitMessage the commit message
     */
    public record ResourceCommitInfo(
        String committedAt,
        String commitMessage) {}

    /**
     * Entity details for a deployment.
     *
     * @param online the online deployment configuration
     * @param custom custom properties
     * @param promptTemplate the prompt template reference
     * @param hardwareSpec the hardware specification
     * @param hardwareRequest the hardware request
     * @param asset the model asset reference
     * @param baseModelId the base model identifier
     * @param deployedAssetType the type of deployed asset
     * @param verbalizer the verbalizer configuration
     * @param status the deployment status
     * @param tooling tooling configuration
     */
    public record DeploymentEntity(
        OnlineDeployment online,
        Map<String, Object> custom,
        SimpleRel promptTemplate,
        HardwareSpec hardwareSpec,
        HardwareRequest hardwareRequest,
        ModelRel asset,
        String baseModelId,
        String deployedAssetType,
        String verbalizer,
        DeploymentStatus status,
        Map<String, String> tooling) {}

    /**
     * Online deployment configuration.
     *
     * @param parameters deployment parameters
     */
    public record OnlineDeployment(Map<String, Object> parameters) {}

    /**
     * Simple resource reference.
     *
     * @param id the resource identifier
     */
    public record SimpleRel(String id) {}

    /**
     * Hardware specification for a deployment.
     *
     * @param id the hardware spec identifier
     * @param rev the revision
     * @param name the hardware spec name
     * @param numNodes the number of nodes
     */
    public record HardwareSpec(
        String id,
        String rev,
        String name,
        Integer numNodes) {}

    /**
     * Hardware request configuration.
     *
     * @param size the hardware size
     * @param numNodes the number of nodes
     */
    public record HardwareRequest(
        String size,
        Number numNodes) {}

    /**
     * Model asset reference.
     *
     * @param id the model identifier
     * @param rev the revision
     * @param resourceKey the resource key
     */
    public record ModelRel(
        String id,
        String rev,
        String resourceKey) {}

    /**
     * Deployment status information.
     *
     * @param state the deployment state
     * @param message the status message
     * @param failure the error response if failed
     * @param inference the inference endpoints
     */
    public record DeploymentStatus(
        String state,
        Message message,
        ApiErrorResponse failure,
        List<Inference> inference) {}

    /**
     * Status message.
     *
     * @param level the message level
     * @param text the message text
     */
    public record Message(
        String level,
        String text) {}

    /**
     * API error response.
     *
     * @param trace the error trace
     * @param errors the list of errors
     */
    public record ApiErrorResponse(
        String trace,
        List<ApiError> errors) {}

    /**
     * API error details.
     *
     * @param code the error code
     * @param message the error message
     * @param moreInfo additional information URL
     * @param target the error target
     */
    public record ApiError(
        String code,
        String message,
        String moreInfo,
        ApiErrorTarget target) {}

    /**
     * Error target information.
     *
     * @param type the target type
     * @param name the target name
     */
    public record ApiErrorTarget(
        String type,
        String name) {}

    /**
     * Inference endpoint information.
     *
     * @param url the inference URL
     * @param sse whether SSE is supported
     * @param usesServingName whether serving name is used
     */
    public record Inference(
        String url,
        Boolean sse,
        Boolean usesServingName) {}
}
