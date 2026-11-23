/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.function.Function;
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
    void should_keep_legacy_constructors_compatible_with_init_cause() {
        var cause = new IllegalStateException("read failure");
        for (var exception : List.of(new WatsonxException(500), new WatsonxException("response", 500, null),
            new AuthenticationTokenExpiredException("response", 401, null),
            new AuthenticationTokenExpiredException(new WatsonxException(401)))) {
            assertSame(exception, exception.initCause(cause));
            assertSame(cause, exception.getCause());
        }
    }

    @Test
    void should_retain_error_body_and_parse_failure() {
        var cause = new IllegalArgumentException("invalid error payload");
        var exception = new WatsonxException("{\"error\":", 503, null, cause);

        assertEquals("{\"error\":", exception.getMessage());
        assertEquals(503, exception.statusCode());
        assertTrue(exception.details().isEmpty());
        assertSame(cause, exception.getCause());
    }

    @Test
    void should_preserve_response_and_diagnostic_failures_when_copying_to_specific_exceptions() {
        var details = new WatsonxError(400, "request-trace", List.of());
        var cause = new IllegalArgumentException("invalid error field");
        var original = new WatsonxException("original response", 400, details, cause);
        var suppressed = new IllegalStateException("cleanup failure");
        original.addSuppressed(suppressed);
        List<Function<WatsonxException, WatsonxException>> constructors = List.of(AuthenticationTokenExpiredException::new,
            AuthorizationRejectedException::new, InvalidInputArgumentException::new, InvalidRequestEntityException::new,
            JsonTypeErrorException::new, JsonValidationErrorException::new, ModelNoSupportForFunctionException::new,
            ModelNotSupportedException::new, TokenQuotaReachedException::new, UserAuthorizationFailedException::new);

        for (var constructor : constructors) {
            var copy = constructor.apply(original);
            assertEquals(original.getMessage(), copy.getMessage());
            assertEquals(original.statusCode(), copy.statusCode());
            assertSame(details, copy.details().orElseThrow());
            assertSame(cause, copy.getCause());
            assertArrayEquals(new Throwable[] { suppressed }, copy.getSuppressed());
        }
    }

    @Test
    void should_compare_error_codes_when_code_or_expected_code_is_missing() {
        var missingCode = new WatsonxError.Error(null, "forbidden", null);
        var accessDenied = new WatsonxError.Error("AccessDenied", "forbidden", null);

        assertFalse(missingCode.is(WatsonxError.Code.COS_ACCESS_DENIED));
        assertFalse(missingCode.is(null));
        assertFalse(accessDenied.is(null));
        assertTrue(accessDenied.is(WatsonxError.Code.COS_ACCESS_DENIED));
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
