/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static java.util.Optional.ofNullable;
import java.util.List;
import java.util.Map;

/**
 * Represents a foundation model with its metadata and capabilities.
 *
 * @param modelId the unique model identifier
 * @param label the display label
 * @param provider the model provider
 * @param source the model source
 * @param functions the list of supported functions
 * @param shortDescription brief description
 * @param longDescription detailed description
 * @param termsUrl URL to terms and conditions
 * @param inputTier the input pricing tier
 * @param outputTier the output pricing tier
 * @param numberParams the number of parameters
 * @param minShotSize minimum shot size for few-shot learning
 * @param taskIds the list of supported task identifiers
 * @param tasks the list of supported tasks
 * @param modelLimits the model limits
 * @param limits the limits map
 * @param lifecycle the lifecycle information
 * @param trainingParameters the training parameters
 * @param versions the list of model versions
 * @param supportedLanguages the list of supported languages
 * @param loraFineTuningParameters the LoRA fine-tuning parameters
 * @param dataType the data type
 * @param architectureType the architecture type
 * @param curatedModelInfo curated model information
 * @param deploymentParameters the deployment parameters
 */
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

    /**
     * Returns the maximum sequence length supported by the model.
     *
     * @return the maximum sequence length, or {@code null} if not available
     */
    public Integer maxSequenceLength() {
        return ofNullable(modelLimits).map(m -> m.maxSequenceLength()).orElse(null);
    }

    /**
     * Returns the maximum number of output tokens supported by the model.
     *
     * @return the maximum output tokens, or {@code null} if not available
     */
    public Integer maxOutputTokens() {
        return ofNullable(modelLimits).map(m -> m.maxOutputTokens()).orElse(null);
    }

    /**
     * Represents a function supported by the model.
     *
     * @param id the function identifier
     */
    public record Function(String id) {}

    /**
     * Represents quality ratings.
     *
     * @param quality the quality rating
     */
    public record Ratings(Integer quality) {}

    /**
     * Represents a default value.
     *
     * @param defaultValue the default value
     */
    public record DefaultValue(String defaultValue) {}

    /**
     * Represents target modules configuration.
     *
     * @param defaultValue the list of default target modules
     */
    public record TargetModules(List<String> defaultValue) {}

    /**
     * Represents gradient checkpointing configuration.
     *
     * @param defaultValue whether gradient checkpointing is enabled by default
     */
    public record GradientCheckpointing(Boolean defaultValue) {}

    /**
     * Represents GPU configuration.
     *
     * @param defaultValue the default number of GPUs
     */
    public record NumGpus(Integer defaultValue) {}

    /**
     * Represents a task supported by the model.
     *
     * @param id the task identifier
     * @param ratings the task ratings
     * @param tags the task tags
     * @param trainingParameters the training parameters for this task
     */
    public record Task(
        String id,
        Ratings ratings,
        List<String> tags,
        TrainingParameters trainingParameters) {}

    /**
     * Represents model limits and constraints.
     *
     * @param maxSequenceLength the maximum sequence length
     * @param maxOutputTokens the maximum output tokens
     * @param trainingDataMaxRecords the maximum training data records
     * @param embeddingDimension the embedding dimension
     */
    public record ModelLimits(
        Integer maxSequenceLength,
        Integer maxOutputTokens,
        Integer trainingDataMaxRecords,
        Integer embeddingDimension) {}

    /**
     * Represents usage limits.
     *
     * @param callTime the call time limit
     * @param maxOutputTokens the maximum output tokens
     */
    public record Limit(
        String callTime,
        Integer maxOutputTokens) {}

    /**
     * Represents model lifecycle information.
     *
     * @param id the lifecycle identifier
     * @param startDate the start date
     * @param alternativeModelIds the list of alternative model identifiers
     */
    public record Lifecycle(
        String id,
        String startDate,
        List<String> alternativeModelIds) {}

    /**
     * Represents training parameters for the model.
     *
     * @param initMethod the initialization method
     * @param initText the initialization text
     * @param numVirtualTokens the number of virtual tokens
     * @param numEpochs the number of epochs range
     * @param verbalizer the verbalizer configuration
     * @param batchSize the batch size range
     * @param maxInputTokens the maximum input tokens range
     * @param maxOutputTokens the maximum output tokens range
     * @param torchDtype the torch data type
     * @param accumulateSteps the accumulate steps range
     * @param learningRate the learning rate range
     */
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

    /**
     * Represents initialization method configuration.
     *
     * @param supported the list of supported methods
     * @param defaultValue the default method
     */
    public record InitMethod(
        List<String> supported,
        String defaultValue) {}

    /**
     * Represents virtual tokens configuration.
     *
     * @param supported the list of supported values
     * @param defaultValue the default value
     */
    public record NumVirtualTokens(
        List<Integer> supported,
        Integer defaultValue) {}

    /**
     * Represents an integer range with default, min, and max values.
     *
     * @param defaultValue the default value
     * @param min the minimum value
     * @param max the maximum value
     */
    public record IntRange(
        Integer defaultValue,
        Integer min,
        Integer max) {}

    /**
     * Represents a double range with default, min, and max values.
     *
     * @param defaultValue the default value
     * @param min the minimum value
     * @param max the maximum value
     */
    public record DoubleRange(
        Double defaultValue,
        Double min,
        Double max) {}

    /**
     * Represents a model version.
     *
     * @param version the version identifier
     * @param availableDate the availability date
     */
    public record Version(
        String version,
        String availableDate) {}

    /**
     * Represents LoRA fine-tuning parameters.
     *
     * @param numEpochs the number of epochs range
     * @param verbalizer the verbalizer configuration
     * @param batchSize the batch size range
     * @param accumulateSteps the accumulate steps range
     * @param learningRate the learning rate range
     * @param maxSeqLength the maximum sequence length range
     * @param tokenizer the tokenizer configuration
     * @param responseTemplate the response template configuration
     * @param numGpus the number of GPUs configuration
     * @param peftParameters the PEFT parameters
     * @param gradientCheckpointing the gradient checkpointing configuration
     */
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

    /**
     * Represents PEFT (Parameter-Efficient Fine-Tuning) parameters.
     *
     * @param type the PEFT type configuration
     * @param rank the rank configuration
     * @param targetModules the target modules configuration
     * @param loraAlpha the LoRA alpha range
     * @param loraDropout the LoRA dropout range
     */
    public record PeftParameters(
        Type type,
        Rank rank,
        TargetModules targetModules,
        IntRange loraAlpha,
        DoubleRange loraDropout) {}

    /**
     * Represents type configuration.
     *
     * @param supported the list of supported types
     * @param defaultValue the default type
     */
    public record Type(
        List<String> supported,
        String defaultValue) {}

    /**
     * Represents rank configuration.
     *
     * @param supported the list of supported ranks
     * @param defaultValue the default rank
     */
    public record Rank(
        List<Integer> supported,
        Integer defaultValue) {}

    /**
     * Represents curated model information.
     *
     * @param baseModelId the base model identifier
     * @param hardwareSpec the hardware specification
     * @param loraHardwareSpec the LoRA hardware specification
     * @param configuredMaxSequenceLength the configured maximum sequence length
     * @param configuredMaxOutputTokens the configured maximum output tokens
     * @param category the model category
     * @param maxGpuLoras the maximum GPU LoRAs
     * @param maxCpuLoras the maximum CPU LoRAs
     * @param maxLoraRank the maximum LoRA rank
     */
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

    /**
     * Represents a deployment parameter.
     *
     * @param name the parameter name
     * @param displayName the display name
     * @param defaultValue the default value
     * @param type the parameter type
     */
    public record DeploymentParameter(
        String name,
        String displayName,
        Object defaultValue,
        String type) {}
}