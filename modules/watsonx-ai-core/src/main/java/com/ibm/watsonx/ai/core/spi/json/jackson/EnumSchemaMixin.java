/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json.jackson;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class EnumSchemaMixin {
  @JsonProperty("enum")
  abstract List<String> enumValues();
}
