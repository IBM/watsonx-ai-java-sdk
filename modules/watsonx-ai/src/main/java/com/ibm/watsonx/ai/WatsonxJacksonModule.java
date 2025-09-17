/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.JsonSchema.EnumSchema;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;

/**
 * Custom Jackson module used to register mix-in annotations for serializing and deserializing specific components.
 */
public class WatsonxJacksonModule extends SimpleModule {

    public WatsonxJacksonModule() {
        super("watsonx-ai-jackson-module");
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
    }

    /**
     * Mix-in interface to support enum deserialization where the enum values should be listed under the JSON property "enum".
     */
    public static abstract class EnumSchemaMixin {
        @JsonProperty("enum")
        abstract List<String> enumValues();
    }

    /**
     * Mix-in abstract class for supporting default value serialization/deserialization. This is used for various parameter types that expose a
     * "default" field in JSON.
     */
    public abstract class DefaultValueMixin {
        @JsonProperty("default")
        abstract Object defaultValue();
    }

    /**
     * Mix-in abstract class for supporting serialization/deserialization of {@link AssistantMessage} without exposing the internal {@code thinking}
     * field.
     */
    public abstract class AssistantMessageMixIn {
        @JsonIgnore
        abstract String thinking();
    }
}