/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.core.chat.JsonSchema;
import com.ibm.watsonx.core.chat.JsonSchema.ArraySchema;
import com.ibm.watsonx.core.chat.JsonSchema.BooleanSchema;
import com.ibm.watsonx.core.chat.JsonSchema.EnumSchema;
import com.ibm.watsonx.core.chat.JsonSchema.IntegerSchema;
import com.ibm.watsonx.core.chat.JsonSchema.NumberSchema;
import com.ibm.watsonx.core.chat.JsonSchema.StringSchema;

public class JsonSchemaTest {

  @Test
  void test_object_schema_1() {

    final String EXPECTED = """
      {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "age": {
            "type": "integer"
          }
        }
      }""";

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_object_schema_2() {

    final String EXPECTED = """
        {
            "type": "object",
            "properties": {
              "name": {
                "type": "object",
                "properties": {
                  "firstName": {
                    "type": "string"
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
                "type": "integer"
              }
            }
      }""";

    var schema = JsonSchema.builder()
      .addProperty("name",
        JsonSchema.builder()
          .addProperty("firstName", StringSchema.of())
          .addProperty("lastName", StringSchema.of())
          .addProperty("middleName", StringSchema.of())
          .required(List.of("firstName", "lastName"))
          .build()
      )
      .addProperty("age", IntegerSchema.of())
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addObjectProperty("name", JsonSchema.builder()
        .addStringProperty("firstName")
        .addStringProperty("lastName")
        .addStringProperty("middleName")
        .required("firstName", "lastName")
      )
      .addIntegerProperty("age")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_object_schema_3() {

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
            "type": "number"
          }
        }
      }""";

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("performanceRating", NumberSchema.of())
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addNumberProperty("performanceRating")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_enum_schema_1() {

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

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("hobbies", EnumSchema.of("reading", "writing", "painting"))
      .addProperty("department", EnumSchema.of("engineering", "marketing", "sales", "HR", "finance"))
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addEnumProperty("hobbies", "reading", "writing", "painting")
      .addEnumProperty("department", "engineering", "marketing", "sales", "HR", "finance")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_enum_schema_2() {

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

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("performanceRating", EnumSchema.of(1, 2, 3, 4, 5, null))
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addEnumProperty("performanceRating", 1, 2, 3, 4, 5, null)
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_array_schema() {

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

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("hobbies", ArraySchema.of(StringSchema.of()))
      .required(List.of("name", "age", "hobbies"))
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addArrayProperty("hobbies", StringSchema.of())
      .required("name", "age", "hobbies")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_array_object_schema() {

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
            }
          }
        }
      }""";

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("skills", ArraySchema.of(
        JsonSchema.builder()
          .addProperty("name", StringSchema.of())
          .addProperty("level", StringSchema.of())
          .build()
      ))
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addArrayProperty("skills",
        JsonSchema.builder()
          .addStringProperty("name")
          .addStringProperty("level")
      ).build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_boolean_schema() {

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

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.of())
      .addProperty("age", IntegerSchema.of())
      .addProperty("hasAgreedToTerms", BooleanSchema.of())
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addStringProperty("name")
      .addIntegerProperty("age")
      .addBooleanProperty("hasAgreedToTerms")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_nullable_description_schema() {

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

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.ofNullable("description"))
      .addProperty("age", IntegerSchema.ofNullable("description"))
      .addProperty("score", NumberSchema.ofNullable("description"))
      .addProperty("hasAgreedToTerms", BooleanSchema.ofNullable("description"))
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addNullableStringProperty("name", "description")
      .addNullableIntegerProperty("age", "description")
      .addNullableNumberProperty("score", "description")
      .addNullableBooleanProperty("hasAgreedToTerms", "description")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }

  @Test
  void test_nullable_schema() {

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
            }
          }
        }
      """;

    var schema = JsonSchema.builder()
      .addProperty("name", StringSchema.ofNullable())
      .addProperty("age", IntegerSchema.ofNullable())
      .addProperty("score", NumberSchema.ofNullable())
      .addProperty("hasAgreedToTerms", BooleanSchema.ofNullable())
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);

    schema = JsonSchema.builder()
      .addNullableStringProperty("name")
      .addNullableIntegerProperty("age")
      .addNullableNumberProperty("score")
      .addNullableBooleanProperty("hasAgreedToTerms")
      .build();

    JSONAssert.assertEquals(EXPECTED, Json.toJson(schema), true);
  }
}
