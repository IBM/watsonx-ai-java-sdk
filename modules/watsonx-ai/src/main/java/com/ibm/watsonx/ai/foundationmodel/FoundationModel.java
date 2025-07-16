/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import java.util.List;
import java.util.Map;

public record FoundationModel(
    String modelId,
    String label,
    String provider,
    String source,
    List<Function> functions,
    String shortDescription,
    String longDescription,
    String termsUrl,
    String inputTier,
    String outputTier,
    String numberParams,
    Integer minShotSize,
    List<String> taskIds,
    List<Task> tasks,
    ModelLimits modelLimits,
    Map<String, Limit> limits,
    List<Lifecycle> lifecycle,
    TrainingParameters trainingParameters,
    List<Version> versions,
    List<String> supportedLanguages,
    LoRaFineTuningParameters loraFineTuningParameters,
    String dataType,
    String architectureType,
    CuratedModelInfo curatedModelInfo,
    List<DeploymentParameter> deploymentParameters) {

    public Integer maxSequenceLength() {
        return modelLimits.maxSequenceLength();
    }

    public Integer maxOutputTokens() {
        return modelLimits.maxOutputTokens();
    }

    public record Function(String id) {}
    public record Ratings(Integer quality) {}
    public record DefaultValue(String defaultValue) {}
    public record TargetModules(List<String> defaultValue) {}
    public record GradientCheckpointing(Boolean defaultValue) {}
    public record NumGpus(Integer defaultValue) {}

    public record Task(
        String id,
        Ratings ratings,
        List<String> tags,
        TrainingParameters trainingParameters) {}

    public record ModelLimits(
        Integer maxSequenceLength,
        Integer maxOutputTokens,
        Integer trainingDataMaxRecords,
        Integer embeddingDimension) {}

    public record Limit(
        String callTime,
        Integer maxOutputTokens) {}

    public record Lifecycle(
        String id,
        String startDate,
        List<String> alternativeModelIds) {}

    public record TrainingParameters(
        InitMethod initMethod,
        DefaultValue initText,
        NumVirtualTokens numVirtualTokens,
        IntRange numEpochs,
        DefaultValue verbalizer,
        IntRange batchSize,
        IntRange maxInputTokens,
        IntRange maxOutputTokens,
        DefaultValue torchDtype,
        IntRange accumulateSteps,
        DoubleRange learningRate) {}

    public record InitMethod(
        List<String> supported,
        String defaultValue) {}

    public record NumVirtualTokens(
        List<Integer> supported,
        Integer defaultValue) {}

    public record IntRange(
        Integer defaultValue,
        Integer min,
        Integer max) {}

    public record DoubleRange(
        Double defaultValue,
        Double min,
        Double max) {}

    public record Version(
        String version,
        String availableDate) {}

    public record LoRaFineTuningParameters(
        IntRange numEpochs,
        DefaultValue verbalizer,
        IntRange batchSize,
        IntRange accumulateSteps,
        DoubleRange learningRate,
        IntRange maxSeqLength,
        DefaultValue tokenizer,
        DefaultValue responseTemplate,
        NumGpus numGpus,
        PeftParameters peftParameters,
        GradientCheckpointing gradientCheckpointing) {}

    public record PeftParameters(
        Type type,
        Rank rank,
        TargetModules targetModules,
        IntRange loraAlpha,
        DoubleRange loraDropout) {}

    public record Type(
        List<String> supported,
        String defaultValue) {}

    public record Rank(
        List<Integer> supported,
        Integer defaultValue) {}

    public record CuratedModelInfo(
        String baseModelId,
        String hardwareSpec,
        String loraHardwareSpec,
        Integer configuredMaxSequenceLength,
        Integer configuredMaxOutputTokens,
        String category,
        Integer maxGpuLoras,
        Integer maxCpuLoras,
        Integer maxLoraRank) {}

    public record DeploymentParameter(
        String name,
        String displayName,
        Object defaultValue,
        String type) {}
}