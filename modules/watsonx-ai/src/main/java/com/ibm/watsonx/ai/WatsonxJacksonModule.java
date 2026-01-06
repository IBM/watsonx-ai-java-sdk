/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.ChatUsage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.ArraySchema;
import com.ibm.watsonx.ai.chat.model.schema.ConstantSchema;
import com.ibm.watsonx.ai.chat.model.schema.EnumSchema;
import com.ibm.watsonx.ai.chat.model.schema.IntegerSchema;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.chat.model.schema.NumberSchema;
import com.ibm.watsonx.ai.chat.model.schema.ObjectSchema;
import com.ibm.watsonx.ai.chat.model.schema.RequiredSchema;
import com.ibm.watsonx.ai.chat.model.schema.StringSchema;
import com.ibm.watsonx.ai.detection.BaseDetectionRequest;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.TextDetectionContentDetectors;
import com.ibm.watsonx.ai.detection.detector.GraniteGuardian;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.textgeneration.Moderation;
import com.ibm.watsonx.ai.textgeneration.Moderation.InputRanges;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters.LengthPenalty;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters.ReturnOptions;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.KvpPage;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;

/**
 * Custom Jackson module used to register mix-in annotations for serializing and deserializing specific components.
 */
public class WatsonxJacksonModule extends SimpleModule {

    /**
     * Constructs a new WatsonxJacksonModule and registers all mix-in annotations.
     */
    public WatsonxJacksonModule() {
        super("watsonx-ai-jackson-module");

        // --- Chat Mixin --- //
        setMixInAnnotation(ChatResponse.class, ChatResponseMixin.class);
        setMixInAnnotation(ChatResponse.Builder.class, ChatResponseBuilderMixin.class);
        setMixInAnnotation(AssistantMessage.class, AssistantMessageMixIn.class);
        setMixInAnnotation(TextChatRequest.class, TextChatRequestMixin.class);
        setMixInAnnotation(TextChatRequest.Builder.class, TextChatRequestBuilderMixin.class);
        setMixInAnnotation(ToolArguments.class, ToolArgumentsMixin.class);

        // -- Text Generation Mixin --- //
        setMixInAnnotation(TextGenerationParameters.class, TextGenerationParametersMixin.class);
        setMixInAnnotation(TextGenerationParameters.Builder.class, TextGenerationParametersBuilderMixin.class);
        setMixInAnnotation(Moderation.class, ModerationMixin.class);
        setMixInAnnotation(Moderation.Builder.class, ModerationBuilderMixin.class);

        // --- Schema Mixin --- //
        setMixInAnnotation(ArraySchema.class, ArraySchemaMixin.class);
        setMixInAnnotation(ConstantSchema.class, ConstantSchemaMixin.class);
        setMixInAnnotation(RequiredSchema.class, RequiredSchemaMixin.class);
        setMixInAnnotation(EnumSchema.class, EnumSchemaMixin.class);
        setMixInAnnotation(IntegerSchema.class, IntegerSchemaMixin.class);
        setMixInAnnotation(JsonSchema.class, JsonSchemaMixin.class);
        setMixInAnnotation(ObjectSchema.class, ObjectSchemaMixin.class);
        setMixInAnnotation(NumberSchema.class, NumberSchemaMixin.class);
        setMixInAnnotation(StringSchema.class, StringSchemaMixin.class);

        // --- Detection Mixin --- //
        setMixInAnnotation(DetectionTextResponse.class, DetectionTextResponseMixin.class);
        setMixInAnnotation(DetectionTextRequest.class, DetectionTextRequestMixin.class);
        setMixInAnnotation(DetectionTextRequest.Builder.class, DetectionTextRequestBuilderMixin.class);
        setMixInAnnotation(TextDetectionContentDetectors.class, TextDetectionContentDetectorsMixin.class);
        setMixInAnnotation(TextDetectionContentDetectors.class, TextDetectionContentDetectorsMixin.class);
        setMixInAnnotation(BaseDetectionRequest.class, BaseDetectionRequestMixin.class);

        // --- Foudation Mixin --- //
        setMixInAnnotation(FoundationModel.DefaultValue.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.NumGpus.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.InitMethod.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.Type.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.NumVirtualTokens.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.IntRange.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.Rank.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.DoubleRange.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.GradientCheckpointing.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.TargetModules.class, DefaultValueMixin.class);
        setMixInAnnotation(FoundationModel.DeploymentParameter.class, DefaultValueMixin.class);

        // --- Text Extraction / Classification Mixin --- //
        setMixInAnnotation(Schema.class, SchemaMixin.class);
        setMixInAnnotation(Schema.Builder.class, SchemaBuilderMixin.class);
        setMixInAnnotation(KvpFields.class, KvpFieldsMixin.class);
        setMixInAnnotation(KvpFields.Builder.class, KvpFieldsBuilderMixin.class);

        // --- Time Series Mixin --- //
        setMixInAnnotation(InputSchema.class, InputSchemaMixin.class);
        setMixInAnnotation(InputSchema.Builder.class, InputSchemaBuilderMixin.class);
        setMixInAnnotation(TimeSeriesParameters.class, TimeSeriesParametersMixin.class);
        setMixInAnnotation(TimeSeriesParameters.Builder.class, TimeSeriesParametersBuilderMixin.class);
    }

    @JsonDeserialize(builder = Moderation.Builder.class)
    public abstract static class ModerationMixin {

        @JsonProperty("hap")
        abstract Hap hap();

        @JsonProperty("pii")
        abstract Pii pii();

        @JsonProperty("granite_guardian")
        abstract GraniteGuardian graniteGuardian();

        @JsonProperty("input_ranges")
        abstract List<InputRanges> inputRanges();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class ModerationBuilderMixin {}

    @JsonDeserialize(builder = TextGenerationParameters.Builder.class)
    public abstract static class TextGenerationParametersMixin {

        @JsonProperty("decoding_method")
        abstract String decodingMethod();

        @JsonProperty("length_penalty")
        abstract LengthPenalty lengthPenalty();

        @JsonProperty("max_new_tokens")
        abstract Integer maxNewTokens();

        @JsonProperty("min_new_tokens")
        abstract Integer minNewTokens();

        @JsonProperty("random_seed")
        abstract Integer randomSeed();

        @JsonProperty("stop_sequences")
        abstract List<String> stopSequences();

        @JsonProperty("temperature")
        abstract Double temperature();

        @JsonProperty("time_limit")
        abstract Long timeLimit();

        @JsonProperty("top_k")
        abstract Integer topK();

        @JsonProperty("top_p")
        abstract Double topP();

        @JsonProperty("repetition_penalty")
        abstract Double repetitionPenalty();

        @JsonProperty("truncate_input_tokens")
        abstract Integer truncateInputTokens();

        @JsonProperty("return_options")
        abstract ReturnOptions returnOptions();

        @JsonProperty("include_stop_sequence")
        abstract Boolean includeStopSequence();

        @JsonProperty("prompt_variables")
        abstract Map<String, String> promptVariables();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class TextGenerationParametersBuilderMixin {}

    @JsonDeserialize(builder = TimeSeriesParameters.Builder.class)
    public abstract static class TimeSeriesParametersMixin {

        @JsonProperty("model_id")
        abstract String modelId();

        @JsonProperty("prediction_length")
        abstract Integer predictionLength();

        @JsonProperty("future_data")
        abstract ForecastData futureData();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class TimeSeriesParametersBuilderMixin {}

    @JsonDeserialize(builder = InputSchema.Builder.class)
    public abstract static class InputSchemaMixin {

        @JsonProperty("timestamp_column")
        abstract String timestampColumn();

        @JsonProperty("id_columns")
        abstract List<String> idColumns();

        @JsonProperty("freq")
        abstract String freq();

        @JsonProperty("target_columns")
        abstract List<String> targetColumns();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class InputSchemaBuilderMixin {}

    public abstract static class BaseDetectionRequestMixin {

        @JsonProperty("detectors")
        abstract Map<String, Map<String, Object>> detectors();

        @JsonProperty("project_id")
        abstract String projectId();

        @JsonProperty("space_id")
        abstract String spaceId();
    }

    public abstract static class TextDetectionContentDetectorsMixin {

        @JsonProperty("input")
        abstract String input();
    }

    public abstract static class DetectionTextResponseMixin {
        @JsonCreator
        public DetectionTextResponseMixin(
            @JsonProperty("text") String text,
            @JsonProperty("detection_type") String detectionType,
            @JsonProperty("detection") String detection,
            @JsonProperty("score") double score,
            @JsonProperty("start") int start,
            @JsonProperty("end") int end) {}
    }

    @JsonDeserialize(builder = DetectionTextRequest.Builder.class)
    public abstract static class DetectionTextRequestMixin {

        @JsonProperty("input")
        abstract String input();

        @JsonProperty("detectors")
        abstract Map<String, Map<String, Object>> detectors();

        @JsonProperty("project_id")
        abstract String projectId();

        @JsonProperty("space_id")
        abstract String spaceId();

        @JsonProperty("transaction_id")
        abstract String transactionId();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class DetectionTextRequestBuilderMixin {}

    @JsonDeserialize(builder = ChatResponse.Builder.class)
    public abstract static class ChatResponseMixin {

        @JsonProperty("id")
        abstract String id();

        @JsonProperty("object")
        abstract String object();

        @JsonProperty("model_id")
        abstract String modelId();

        @JsonProperty("model")
        abstract String model();

        @JsonProperty("choices")
        abstract List<ResultChoice> choices();

        @JsonProperty("created")
        abstract Long created();

        @JsonProperty("model_version")
        abstract String modelVersion();

        @JsonProperty("created_at")
        abstract String createdAt();

        @JsonProperty("usage")
        abstract ChatUsage usage();

        @JsonProperty("extraction_tags")
        abstract ExtractionTags extractionTags();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class ChatResponseBuilderMixin {}

    @JsonDeserialize(builder = TextChatRequest.Builder.class)
    public abstract static class TextChatRequestMixin {

        @JsonProperty("model_id")
        abstract String modelId();

        @JsonProperty("space_id")
        abstract String spaceId();

        @JsonProperty("project_id")
        abstract String projectId();

        @JsonProperty("messages")
        abstract List<ChatMessage> messages();

        @JsonProperty("tools")
        abstract List<Tool> tools();

        @JsonProperty("tool_choice_option")
        abstract String toolChoiceOption();

        @JsonProperty("tool_choice")
        abstract Map<String, Object> toolChoice();

        @JsonProperty("frequency_penalty")
        abstract Double frequencyPenalty();

        @JsonProperty("logit_bias")
        abstract Map<String, Integer> logitBias();

        @JsonProperty("logprobs")
        abstract Boolean logprobs();

        @JsonProperty("top_logprobs")
        abstract Integer topLogprobs();

        @JsonProperty("max_completion_tokens")
        abstract Integer maxCompletionTokens();

        @JsonProperty("n")
        abstract Integer n();

        @JsonProperty("presence_penalty")
        abstract Double presencePenalty();

        @JsonProperty("seed")
        abstract Integer seed();

        @JsonProperty("stop")
        abstract List<String> stop();

        @JsonProperty("temperature")
        abstract Double temperature();

        @JsonProperty("top_p")
        abstract Double topP();

        @JsonProperty("time_limit")
        abstract Long timeLimit();

        @JsonProperty("response_format")
        abstract Map<String, Object> responseFormat();

        @JsonProperty("chat_template_kwargs")
        abstract Map<String, Object> chatTemplateKwargs();

        @JsonProperty("include_reasoning")
        abstract Boolean includeReasoning();

        @JsonProperty("reasoning_effort")
        abstract String reasoningEffort();

        @JsonProperty("guided_choice")
        abstract Set<String> guidedChoice();

        @JsonProperty("guided_regex")
        abstract String guidedRegex();

        @JsonProperty("guided_grammar")
        abstract String guidedGrammar();

        @JsonProperty("repetition_penalty")
        abstract Double repetitionPenalty();

        @JsonProperty("length_penalty")
        abstract Double lengthPenalty();

        @JsonProperty("context")
        abstract String context();

        @JsonProperty("json_schema")
        abstract JsonSchemaObject jsonSchema();

        @JsonProperty("crypto")
        abstract Map<String, Object> crypto();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class TextChatRequestBuilderMixin {}

    public abstract static class ToolArgumentsMixin {

        @JsonAnyGetter
        abstract Map<String, Object> getRaw();
    }

    public abstract class ArraySchemaMixin {

        @JsonProperty("items")
        abstract JsonSchema items();

        @JsonProperty("contains")
        abstract JsonSchema contains();

        @JsonProperty("minItems")
        abstract Integer minItems();

        @JsonProperty("maxItems")
        abstract Integer maxItems();

    }

    public abstract static class ConstantSchemaMixin {
        @JsonProperty("const")
        abstract String constant();
    }

    public abstract static class RequiredSchemaMixin {
        @JsonProperty("required")
        abstract String required();
    }

    public abstract static class EnumSchemaMixin {
        @JsonProperty("enum")
        abstract List<String> enumValues();
    }

    public abstract static class IntegerSchemaMixin {

        @JsonProperty("minimum")
        abstract Integer minimum();

        @JsonProperty("maximum")
        abstract Integer maximum();

        @JsonProperty("exclusiveMinimum")
        abstract Integer exclusiveMinimum();

        @JsonProperty("exclusiveMaximum")
        abstract Integer exclusiveMaximum();

        @JsonProperty("multipleOf")
        abstract Integer multipleOf();
    }

    public abstract static class JsonSchemaMixin {

        @JsonProperty("description")
        abstract String description();

        @JsonProperty("type")
        abstract Object type();

        @JsonProperty("oneOf")
        abstract List<String> oneOf();

    }

    public abstract class ObjectSchemaMixin {

        @JsonProperty("properties")
        abstract Map<String, JsonSchema> properties();

        @JsonProperty("required")
        abstract List<String> required();

        @JsonProperty("anyOf")
        abstract List<String> anyOf();

        @JsonProperty("minProperties")
        abstract Integer minProperties();

        @JsonProperty("maxProperties")
        abstract Integer maxProperties();

        @JsonProperty("patternProperties")
        abstract Map<String, JsonSchema> patternProperties();

        @JsonProperty("additionalProperties")
        abstract Object additionalProperties();
    }

    public abstract static class NumberSchemaMixin {

        @JsonProperty("minimum")
        abstract Integer minimum();

        @JsonProperty("maximum")
        abstract Integer maximum();

        @JsonProperty("exclusiveMinimum")
        abstract Integer exclusiveMinimum();

        @JsonProperty("exclusiveMaximum")
        abstract Integer exclusiveMaximum();

        @JsonProperty("multipleOf")
        abstract Double multipleOf();
    }

    public abstract static class StringSchemaMixin {

        @JsonProperty("pattern")
        abstract String getPattern();

        @JsonProperty("maxLength")
        abstract Integer getMaxLength();

        @JsonProperty("minLength")
        abstract Integer getMinLength();

        @JsonProperty("format")
        abstract String getFormat();
    }

    public abstract static class DefaultValueMixin {
        @JsonProperty("default")
        abstract Object defaultValue();
    }

    public abstract static class AssistantMessageMixIn {
        @JsonIgnore
        abstract String thinking();
    }

    @JsonDeserialize(builder = Schema.Builder.class)
    public abstract class SchemaMixin {

        @JsonProperty("document_type")
        abstract String documentType();

        @JsonProperty("document_description")
        abstract String documentDescription();

        @JsonProperty("fields")
        abstract Map<String, KvpField> fields();

        @JsonProperty("pages")
        abstract KvpPage pages();

        @JsonProperty("additional_prompt_instructions")
        abstract String additionalPromptInstructions();

    }

    @JsonPOJOBuilder(withPrefix = "")
    public abstract class SchemaBuilderMixin {}

    @JsonDeserialize(builder = KvpFields.Builder.class)
    public abstract class KvpFieldsMixin {}

    @JsonPOJOBuilder(withPrefix = "")
    public abstract class KvpFieldsBuilderMixin {
        @JsonAnySetter
        public abstract KvpFields.Builder add(String key, KvpFields.KvpField value);
    }
}