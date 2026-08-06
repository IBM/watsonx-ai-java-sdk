/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.exception.AuthenticationTokenExpiredException;
import com.ibm.watsonx.ai.core.exception.AuthorizationRejectedException;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.InvalidRequestEntityException;
import com.ibm.watsonx.ai.core.exception.JsonTypeErrorException;
import com.ibm.watsonx.ai.core.exception.JsonValidationErrorException;
import com.ibm.watsonx.ai.core.exception.ModelNoSupportForFunctionException;
import com.ibm.watsonx.ai.core.exception.ModelNotSupportedException;
import com.ibm.watsonx.ai.core.exception.TokenQuotaReachedException;
import com.ibm.watsonx.ai.core.exception.UserAuthorizationFailedException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;

public class WatsonxExceptionTest {

    @Test
    void should_construct_with_status_code_only() {
        WatsonxException ex = new WatsonxException(500);
        assertEquals(500, ex.statusCode());
        assertTrue(ex.details().isEmpty());
    }

    @Test
    void should_construct_all_typed_exceptions_with_message_and_details() {
        WatsonxError error = new WatsonxError(400, "trace", List.of());

        assertInstanceOf(AuthenticationTokenExpiredException.class,
            new AuthenticationTokenExpiredException("msg", 401, error));
        assertInstanceOf(AuthorizationRejectedException.class,
            new AuthorizationRejectedException("msg", 403, error));
        assertInstanceOf(InvalidInputArgumentException.class,
            new InvalidInputArgumentException("msg", 400, error));
        assertInstanceOf(InvalidRequestEntityException.class,
            new InvalidRequestEntityException("msg", 400, error));
        assertInstanceOf(JsonTypeErrorException.class,
            new JsonTypeErrorException("msg", 400, error));
        assertInstanceOf(JsonValidationErrorException.class,
            new JsonValidationErrorException("msg", 400, error));
        assertInstanceOf(ModelNoSupportForFunctionException.class,
            new ModelNoSupportForFunctionException("msg", 400, error));
        assertInstanceOf(ModelNotSupportedException.class,
            new ModelNotSupportedException("msg", 400, error));
        assertInstanceOf(TokenQuotaReachedException.class,
            new TokenQuotaReachedException("msg", 429, error));
        assertInstanceOf(UserAuthorizationFailedException.class,
            new UserAuthorizationFailedException("msg", 401, error));
    }
}
