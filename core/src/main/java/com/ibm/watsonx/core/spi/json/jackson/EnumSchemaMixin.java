package com.ibm.watsonx.core.spi.json.jackson;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class EnumSchemaMixin {
    @JsonProperty("enum")
    abstract List<String> enumValues();
}
