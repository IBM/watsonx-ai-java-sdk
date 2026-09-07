/*
 * Copyright 2025 IBM Corporation
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
    void should_create_object_schema_with_contains_array_property() {

        final String EXPECTED = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "skills": {
                    "type": "array",
                    "contains": {
                        "const": "JavaScript"
                    }
                }
              }
            }""";

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .property("skills", JsonSchema.array().contains(JsonSchema.constant("JavaScript")))
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
                  },
                  "array": {
                    "type": ["array", "null"],
                    "description": "description",
                    "items": {
                        "type": "string"
                    }
                  }
                }
              }
            """;

        var schema = JsonSchema.object()
            .property("name", JsonSchema.string("description").nullable())
            .property("age", JsonSchema.integer("description").nullable())
            .property("score", JsonSchema.number("description").nullable())
            .property("hasAgreedToTerms", JsonSchema.bool("description").nullable())
            .property("array", JsonSchema.array("description").nullable().items(JsonSchema.string()))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
    }

    @Test
    void should_create_object_schema_with_described_properties() {

        final String EXPECTED = """
            {
                "type": "object",
                "description": "description",
                "minProperties": 2,
                "maxProperties": 10,
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
                  },
                  "constant": {
                    "const": "Hello"
                  }
                }
              }
            """;

        var schema = JsonSchema.object("description")
            .minProperties(2)
            .maxProperties(10)
            .property("name", JsonSchema.string().description("description"))
            .property("age", JsonSchema.integer().description("description"))
            .property("score", JsonSchema.number()
                .description("description")
                .exclusiveMinimum(0)
                .exclusiveMaximum(10))
            .property("hasAgreedToTerms", JsonSchema.bool().description("description"))
            .property("constant", JsonSchema.constant("Hello"))
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

    @Test
    void should_add_additional_properties_to_object_schema() {

        JSONAssert.assertEquals("""
            {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string"
                    }
                },
                "additionalProperties": {
                    "type": "string"
                }
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("name", JsonSchema.string())
                    .additionalProperties(JsonSchema.string())
                    .build()
            ), true);

        JSONAssert.assertEquals("""
            {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string"
                    }
                },
                "additionalProperties": false
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("name", JsonSchema.string())
                    .additionalProperties(false)
                    .build()
            ), true);
    }

    @Test
    void should_add_multiple_of_property() {

        JSONAssert.assertEquals("""
            {
                "type": "object",
                "properties": {
                    "age": {
                        "type": "integer",
                        "multipleOf": 5
                    },
                    "height": {
                        "type": "number",
                        "multipleOf": 0.1
                    }
                }
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("age", JsonSchema.integer().multipleOf(5))
                    .property("height", JsonSchema.number().multipleOf(0.1))
                    .build()
            ), true);
    }

    @Test
    void should_use_pattern_properties() {

        JSONAssert.assertEquals("""
            {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "string"
                    }
                },
                "patternProperties": {
                    "^DEPT-[0-9]{3}$": {
                        "type": "string",
                        "pattern": "^[A-Z]+$"
                    }
                }
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("name", JsonSchema.string())
                    .patternProperty("^DEPT-[0-9]{3}$", JsonSchema.string().pattern("^[A-Z]+$"))
                    .build()
            ), true);
    }

    @Test
    void should_support_oneOf_on_object_with_alternative_required_fields() {

        JSONAssert.assertEquals("""
            {
              "type": "object",
              "properties": {
                "age": { "type": number },
                "dateOfBirth": { "type": "string", "format": "date" }
              },
              "oneOf": [
                { "required": ["age"] },
                { "required": ["dateOfBirth"] }
              ]
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("age", JsonSchema.number())
                    .property("dateOfBirth", JsonSchema.string().format(Format.DATE))
                    .oneOf(JsonSchema.required("age"), JsonSchema.required("dateOfBirth"))
                    .build()
            ), true);
    }

    @Test
    void should_support_oneOf_on_property_with_different_constraints() {
        JSONAssert.assertEquals("""
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer",
                  "oneOf": [
                    {
                      "minimum": 18,
                      "maximum": 60
                    },
                    {
                      "minimum": 65
                    }
                  ]
                }
              }
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("name", JsonSchema.string())
                    .property("age", JsonSchema.integer().oneOf(
                        JsonSchema.integer().minimum(18).maximum(60),
                        JsonSchema.integer().minimum(65)))
                    .build()
            ), true);
    }

    @Test
    void should_support_anyOf_on_object_with_alternative_required_fields() {

        JSONAssert.assertEquals("""
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                },
                "employeeType": {
                  "enum": [
                    "full-time",
                    "part-time"
                  ]
                }
              },
              "anyOf": [
                {
                  "required": [
                    "salary"
                  ]
                },
                {
                  "required": [
                    "hourlyRate"
                  ]
                }
              ]
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("name", JsonSchema.string())
                    .property("age", JsonSchema.integer())
                    .property("employeeType", JsonSchema.enumeration("full-time", "part-time"))
                    .anyOf(JsonSchema.required("salary"), JsonSchema.required("hourlyRate"))
                    .build()
            ), true);
    }

    @Test
    void should_remove_nullable_and_description_from_oneOf_alternatives() {

        JSONAssert.assertEquals("""
            {
              "type": "object",
              "properties": {
                "test": {
                  "type": "string",
                  "oneOf": [{ "maxLength": 1}]
                }
              }
            }""",
            Json.toJson(
                JsonSchema.object()
                    .property("test", JsonSchema.string().oneOf(JsonSchema.string("test").nullable().maxLength(1)))
                    .build()
            ), true);
    }

    @Test
    void should_throw_exception_when_using_unsupported_methods_on_constant_schema() {
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.constant("test").description("test"));
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.constant("test").nullable());
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.constant("test").oneOf(JsonSchema.constant("test")));
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.constant("test").oneOf(List.of(JsonSchema.constant("test"))));
    }

    @Test
    void should_throw_exception_when_using_unsupported_methods_on_required_schema() {
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.required("test").description("test"));
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.required("test").nullable());
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.required("test").oneOf(JsonSchema.required("test")));
        assertThrows(UnsupportedOperationException.class, () -> JsonSchema.required("test").oneOf(List.of(JsonSchema.required("test"))));
    }

    @Test
    void should_support_oneOf_on_array_with_different_item_types() {
        System.out.println(Json.toJson(
            JsonSchema.array().items(JsonSchema.string())
                .oneOf(
                    JsonSchema.array().items(JsonSchema.string()).minItems(1),
                    JsonSchema.array().items(JsonSchema.number()).minItems(1)
                )
                .build()
        ));
        JSONAssert.assertEquals("""
            {
              "type": "array",
              "oneOf": [
                {
                  "items": {
                    "type": "string"
                  },
                  "minItems": 1
                },
                {
                  "items": {
                    "type": "number"
                  },
                  "minItems": 1
                }
              ]
            }""",
            Json.toJson(
                JsonSchema.array()
                    .oneOf(
                        JsonSchema.array().items(JsonSchema.string()).minItems(1),
                        JsonSchema.array().items(JsonSchema.number()).minItems(1)
                    )
                    .build()
            ), true);
    }
}
