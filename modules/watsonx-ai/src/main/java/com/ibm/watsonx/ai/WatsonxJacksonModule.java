/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.schema.EnumSchema;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Custom Jackson module used to register mix-in annotations for serializing and deserializing specific components.
 */
public class WatsonxJacksonModule extends SimpleModule {

    public WatsonxJacksonModule() {
        super("watsonx-ai-jackson-module");
        setMixInAnnotation(JsonSchema.class, JsonSchemaMixin.class);
        setMixInAnnotation(EnumSchema.class, EnumSchemaMixin.class);
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
        setMixInAnnotation(AssistantMessage.class, AssistantMessageMixIn.class);
        setMixInAnnotation(Schema.class, SchemaMixin.class);
        setMixInAnnotation(Schema.Builder.class, SchemaBuilderMixin.class);
        setMixInAnnotation(KvpFields.class, KvpFieldsMixin.class);
        setMixInAnnotation(KvpFields.Builder.class, KvpFieldsBuilderMixin.class);
    }

    /**
     * Mix-in interface for JsonSchema class.
     */
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public abstract static class JsonSchemaMixin {
        @JsonProperty("const")
        abstract String getConstant();
    }

    /**
     * Mix-in interface to support enum deserialization where the enum values should be listed under the JSON property "enum".
     */
    public abstract static class EnumSchemaMixin {
        @JsonProperty("enum")
        abstract List<String> getEnumValues();
    }

    /**
     * Mix-in abstract class for supporting default value serialization/deserialization. This is used for various parameter types that expose a
     * "default" field in JSON.
     */
    public abstract static class DefaultValueMixin {
        @JsonProperty("default")
        abstract Object defaultValue();
    }

    /**
     * Mix-in abstract class for supporting serialization/deserialization of {@link AssistantMessage} without exposing the internal {@code thinking}
     * field.
     */
    public abstract static class AssistantMessageMixIn {
        @JsonIgnore
        abstract String thinking();
    }

    /**
     * Mix-in class that provides custom deserialization support for the {@link Schema} model using its builder pattern.
     */
    @JsonDeserialize(builder = Schema.Builder.class)
    public abstract class SchemaMixin {}

    /**
     * Mix-in class that provides custom deserialization support for the {@link Schema.Builder} model using its builder pattern.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public abstract class SchemaBuilderMixin {}

    /**
     * Mix-in class that provides custom deserialization support for the {@link KvpFields} model using its builder pattern.
     */
    @JsonDeserialize(builder = KvpFields.Builder.class)
    public abstract class KvpFieldsMixin {}

    /**
     * Mix-in class that provides custom deserialization support for the {@link KvpFields.Builder} model using its builder pattern.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public abstract class KvpFieldsBuilderMixin {
        @JsonAnySetter
        public abstract KvpFields.Builder add(String key, KvpFields.KvpField value);
    }
}