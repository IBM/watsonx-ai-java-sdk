/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError.Error;;

public class WatsonxErrorTest {

  @Test
  void test_watsonx_error_1() {

    var value = fromJson(
      """
        {
            "errors": [
                {
                    "code": "authentication_token_not_valid",
                    "message": "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                }
            ],
            "trace": "23e11747002c4d2919987401b745f6a7",
            "status_code": 401
        }""",
      WatsonxError.class);

    var expected = new WatsonxError(
      401,
      "23e11747002c4d2919987401b745f6a7",
      List.of(
        new Error(
          "authentication_token_not_valid",
          "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
          "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
        )
      )
    );

    assertEquals(expected, value);
  }

  @Test
  void test_watsonx_error_2() {

    var value = fromJson("""
      {
          "errors": [
              {
                  "code": "authorization_rejected",
                  "message": "error"
              },
              {
                  "code": "json_type_error",
                  "message": "error"
              },
              {
                  "code": "model_not_supported",
                  "message": "error"
              },
              {
                  "code": "model_no_support_for_function",
                  "message": "error"
              },
              {
                  "code": "user_authorization_failed",
                  "message": "error"
              },
              {
                  "code": "invalid_request_entity",
                  "message": "error"
              },
              {
                  "code": "invalid_input_argument",
                  "message": "error"
              },
              {
                  "code": "token_quota_reached",
                  "message": "error"
              },
              {
                  "code": "authentication_token_expired",
                  "message": "error"
              },
              {
                  "code": "text_extraction_event_does_not_exist",
                  "message": "error"
              }
          ],
          "trace": "23e11747002c4d2919987401b745f6a7",
          "status_code": 401
      }""", WatsonxError.class);

    var expected = new WatsonxError(
      401,
      "23e11747002c4d2919987401b745f6a7",
      List.of(
        new Error(
          "authorization_rejected",
          "error",
          null
        ),
        new Error(
          "json_type_error",
          "error",
          null
        ),
        new Error(
          "model_not_supported",
          "error",
          null
        ),
        new Error(
          "model_no_support_for_function",
          "error",
          null
        ),
        new Error(
          "user_authorization_failed",
          "error",
          null
        ),
        new Error(
          "invalid_request_entity",
          "error",
          null
        ),
        new Error(
          "invalid_input_argument",
          "error",
          null
        ),
        new Error(
          "token_quota_reached",
          "error",
          null
        ),
        new Error(
          "authentication_token_expired",
          "error",
          null
        ),
        new Error(
          "text_extraction_event_does_not_exist",
          "error",
          null
        )
      )
    );

    assertEquals(expected, value);
  }
}
