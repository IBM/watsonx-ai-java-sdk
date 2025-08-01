/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import java.util.List;
import java.util.Map;

public record DeploymentResource(ResourceMeta metadata, DeploymentEntity entity) {

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

    public record ResourceCommitInfo(
        String committedAt,
        String commitMessage) {}

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

    public record OnlineDeployment(Map<String, Object> parameters) {}

    public record SimpleRel(String id) {}

    public record HardwareSpec(
        String id,
        String rev,
        String name,
        Integer numNodes) {}

    public record HardwareRequest(
        String size,
        Number numNodes) {}

    public record ModelRel(
        String id,
        String rev,
        String resourceKey) {}

    public record DeploymentStatus(
        String state,
        Message message,
        ApiErrorResponse failure,
        List<Inference> inference) {}

    public record Message(
        String level,
        String text) {}

    public record ApiErrorResponse(
        String trace,
        List<ApiError> errors) {}

    public record ApiError(
        String code,
        String message,
        String moreInfo,
        ApiErrorTarget target) {}

    public record ApiErrorTarget(
        String type,
        String name) {}

    public record Inference(
        String url,
        Boolean sse,
        Boolean usesServingName) {}
}
