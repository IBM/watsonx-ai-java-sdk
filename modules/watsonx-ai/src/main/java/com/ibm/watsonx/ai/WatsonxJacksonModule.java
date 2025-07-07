/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ibm.watsonx.ai.chat.model.JsonSchema.EnumSchema;

/**
 * Custom Jackson module used to register mix-in annotations for serializing and deserializing specific components.
 */
public class WatsonxJacksonModule extends SimpleModule {

  public WatsonxJacksonModule() {
    super("watsonx-ai-jackson-module");
    setMixInAnnotation(EnumSchema.class, EnumSchemaMixin.class);
  }

  /**
   * Mix-in interface to support enum deserialization where the enum values should be listed under the JSON property "enum".
   */
  public static abstract class EnumSchemaMixin {
    @JsonProperty("enum")
    abstract List<String> enumValues();
  }
}