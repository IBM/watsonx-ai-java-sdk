package com.ibm.watsonx.core.spi.json.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.ibm.watsonx.core.chat.JsonSchema.EnumSchema;
import com.ibm.watsonx.core.spi.json.JsonAdapter;

/**
 * Default SPI implementation of {@link JsonAdapter} using Jackson.
 */
public class JacksonAdapter implements JsonAdapter {

    private final ObjectMapper objectMapper;

    public JacksonAdapter() {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.addMixIn(EnumSchema.class, EnumSchemaMixin.class);
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String prettyPrint(Object obj) {
        try {
            if (obj instanceof String str) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree((str)));
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
