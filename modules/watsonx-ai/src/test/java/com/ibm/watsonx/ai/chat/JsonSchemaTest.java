/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.model.schema.Format;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;


public class JsonSchemaTest {

    @Test
    void should_create_object_schema_with_string_and_integer_constraints() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "minLength": 12,
                  "maxLength": 68
                },
                "age": {
                  "type": "integer",
                  "minimum": 18,
                  "maximum": 60
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string()
                .minLength(12)
                .maxLength(68))
            .property("age", JsonSchema.integer()
                .minimum(18)
                .maximum(60))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_nested_object_schema_with_required_properties() {

        final String EXPECTED = """
              {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "object",
                      "properties": {
                        "firstName": {
                          "type": "string",
                          "pattern": "^[a-zA-Z]+$"
                        },
                        "lastName": {
                          "type": "string"
                        },
                        "middleName": {
                          "type": "string"
                        }
                      },
                      "required": [ "firstName", "lastName" ]
                    },
                    "age": {
                      "type": "integer",
                      "exclusiveMinimum": 18,
                      "exclusiveMaximum": 60
                    }
                  }
            }""";

        var schema = JsonSchema.object()
            .property("name",
                JsonSchema.object()
                    .property("firstName", JsonSchema.string().pattern("^[a-zA-Z]+$"))
                    .property("lastName", JsonSchema.string())
                    .property("middleName", JsonSchema.string())
                    .required(List.of("firstName", "lastName"))
            )
            .property("age", JsonSchema.integer()
                .exclusiveMinimum(18)
                .exclusiveMaximum(60))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_number_property_and_constraints() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "performanceRating": {
                  "type": "number",
                  "minimum": 18,
                  "maximum": 60
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("performanceRating", JsonSchema.number()
                .minimum(18)
                .maximum(60))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_enum_properties() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "hobbies": {
                  "enum": [
                    "reading",
                    "writing",
                    "painting"
                  ]
                },
                "department": {
                  "enum": [
                    "engineering",
                    "marketing",
                    "sales",
                    "HR",
                    "finance"
                  ]
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("hobbies", JsonSchema.enumeration("reading", "writing", "painting"))
            .property("department", JsonSchema.enumeration("engineering", "marketing", "sales", "HR", "finance"))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_enum_including_null() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "performanceRating": {
                  "enum": [
                    1,
                    2,
                    3,
                    4,
                    5,
                    null
                  ]
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("performanceRating", JsonSchema.enumeration(1, 2, 3, 4, 5, null))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_array_property_and_required_fields() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "hobbies": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              },
              "required": [
                "name",
                "age",
                "hobbies"
              ]
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("hobbies", JsonSchema.array().items(JsonSchema.string()))
            .required("name", "age", "hobbies")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_array_of_objects_and_size_constraints() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "skills": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "level": {
                        "type": "string"
                      }
                    }
                  },
                  "minItems": 1,
                  "maxItems": 2
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("skills", JsonSchema.array()
                .minItems(1)
                .maxItems(2)
                .items(
                    JsonSchema.object()
                        .property("name", JsonSchema.string())
                        .property("level", JsonSchema.string())
                ))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_nullable_array_property() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "skills": {
                  "type": ["array", "null"],
                  "items": {
                    "type": "string"
                  }
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("skills", JsonSchema.array().items(JsonSchema.string()).nullable())
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_throw_exception_when_array_schema_has_no_items_defined() {
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("skills", JsonSchema.array())
            .build());
    }

    @Test
    void should_create_object_schema_with_boolean_property() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "hasAgreedToTerms": {
                  "type": "boolean"
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("age", JsonSchema.integer())
            .property("hasAgreedToTerms", JsonSchema.bool())
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_nullable_and_described_properties() {

        final String EXPECTED = """
            {
                "type": "object",
                "properties": {
                  "name": {
                    "type": ["string", "null"],
                    "description": "description"
                  },
                  "age": {
                    "type": ["integer", "null"],
                    "description": "description"
                  },
                  "score": {
                     "type": ["number", "null"],
                    "description": "description"
                  },
                  "hasAgreedToTerms": {
                    "type": ["boolean", "null"],
                    "description": "description"
                  }
                }
              }
            """;

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string().nullable().description("description"))
            .property("age", JsonSchema.integer().nullable().description("description"))
            .property("score", JsonSchema.number().nullable().description("description"))
            .property("hasAgreedToTerms", JsonSchema.bool().nullable().description("description"))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_described_properties_and_number_constraints() {

        final String EXPECTED = """
            {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "description"
                  },
                  "age": {
                    "type": "integer",
                    "description": "description"
                  },
                  "score": {
                     "type": "number",
                    "description": "description",
                    "exclusiveMinimum": 0,
                    "exclusiveMaximum": 10
                  },
                  "hasAgreedToTerms": {
                     "type": "boolean",
                    "description": "description"
                  }
                }
              }
            """;

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string().description("description"))
            .property("age", JsonSchema.integer().description("description"))
            .property("score", JsonSchema.number()
                .description("description")
                .exclusiveMinimum(0)
                .exclusiveMaximum(10))
            .property("hasAgreedToTerms", JsonSchema.bool().description("description"))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_all_nullable_property_types() {

        final String EXPECTED = """
            {
                "type": "object",
                "properties": {
                  "name": {
                    "type": ["string", "null"]
                  },
                  "age": {
                    "type": ["integer", "null"]
                  },
                  "score": {
                    "type": ["number", "null"]
                  },
                  "hasAgreedToTerms": {
                    "type": ["boolean", "null"]
                  },
                  "skills": {
                    "type": ["array", "null"],
                    "items": {
                        "type": "string"
                    }
                  },
                  "object": {
                    "type": ["object", "null"]
                  }
                }
            }
            """;

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string().nullable())
            .property("age", JsonSchema.integer().nullable())
            .property("score", JsonSchema.number().nullable())
            .property("hasAgreedToTerms", JsonSchema.bool().nullable())
            .property("skills", JsonSchema.array().nullable().items(JsonSchema.string()))
            .property("object", JsonSchema.object().nullable())
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_use_the_correct_format() {

        final String EXPECTED = """
            {
                "type": "object",
                "properties": {
                  "date": {
                    "type": "string",
                    "format": "date"
                  },
                  "date-time": {
                    "type": "string",
                    "format": "date-time"
                  },
                  "time": {
                    "type": "string",
                    "format": "time"
                  },
                  "duration": {
                    "type": "string",
                    "format": "duration"
                  },
                  "email": {
                    "type": "string",
                    "format": "email"
                  },
                  "hostname": {
                    "type": "string",
                    "format": "hostname"
                  },
                  "ipv4": {
                    "type": "string",
                    "format": "ipv4"
                  },
                  "ipv6": {
                    "type": "string",
                    "format": "ipv6"
                  },
                  "uuid": {
                    "type": "string",
                    "format": "uuid"
                  }
                }
            }
            """;

        var schema = JsonSchema.object()
            .property("date", JsonSchema.string().format(Format.DATE))
            .property("date-time", JsonSchema.string().format(Format.DATE_TIME))
            .property("time", JsonSchema.string().format(Format.TIME))
            .property("duration", JsonSchema.string().format(Format.DURATION))
            .property("email", JsonSchema.string().format(Format.EMAIL))
            .property("hostname", JsonSchema.string().format(Format.HOSTNAME))
            .property("ipv4", JsonSchema.string().format(Format.IPV4))
            .property("ipv6", JsonSchema.string().format(Format.IPV6))
            .property("uuid", JsonSchema.string().format(Format.UUID))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }
}
