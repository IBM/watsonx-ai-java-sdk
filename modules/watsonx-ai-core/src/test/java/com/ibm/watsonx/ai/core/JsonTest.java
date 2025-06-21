/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class JsonTest {

  record Person(String name, String lastname) {
  };

  @Test
  void to_json_test() {
    var json = Json.toJson(new Person("Alan", "Wake"));
    assertEquals("{\"name\":\"Alan\",\"lastname\":\"Wake\"}", json);
  }

  @Test
  void from_json_test() {
    var person = Json.fromJson("{\"name\":\"Alan\",\"lastname\":\"Wake\"}", Person.class);
    assertEquals(new Person("Alan", "Wake"), person);
  }
}
