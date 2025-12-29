/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import org.mockito.ArgumentCaptor;

public class Utils {

    public static final String OK_RESPONSE = """
        {
            "access_token": "my_super_token",
            "refresh_token": "not_supported",
            "ims_user_id": 13176852,
            "token_type": "Bearer",
            "expires_in": 3600,
            "expiration": %d,
            "scope": "ibm openid"
        }""";

    public static final String WRONG_RESPONSE = """
        {
            "errorCode": "BXNIM0415E",
            "errorMessage": "Provided API key could not be found.",
            "context": {
                "requestId": "bTlidjk-91c2c5d491364775817d4e78ad5d8d00",
                "requestType": "incoming.Identity_Token",
                "userAgent": "PostmanRuntime/7.44.0",
                "url": "https://iam.cloud.ibm.com",
                "instanceId": "iamid-10-5-4792-d160320-6b494b795d-m9bv9",
                "threadId": "bf061",
                "host": "iamid-10-5-4792-d160320-6b494b795d-m9bv9",
                "startTime": "30.05.2025 15:46:38:151 UTC",
                "endTime": "30.05.2025 15:46:38:236 UTC",
                "elapsedTime": "85",
                "locale": "en_US",
                "clusterName": "iam-id-prod-eu-de-fra05"
            }
        }""";

    public static String bodyPublisherToString(ArgumentCaptor<HttpRequest> request) {
        return bodyPublisherToString(request.getValue());
    }

    public static String bodyPublisherToString(HttpRequest request) {
        HttpRequest.BodyPublisher bodyPublisher = request.bodyPublisher().orElseThrow();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bodyPublisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                baos.write(item.array(), item.position(), item.remaining());
            }

            @Override
            public void onError(Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            @Override
            public void onComplete() {}
        });

        return baos.toString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static HttpResponse<String> okResponse() {
        HttpResponse<String> response = mock(HttpResponse.class);
        long expiration = Instant.now().plusSeconds(3600).getEpochSecond();
        when(response.body()).thenReturn(OK_RESPONSE.formatted(expiration));
        when(response.statusCode()).thenReturn(200);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static HttpResponse<String> expiredResponse() {
        HttpResponse<String> response = mock(HttpResponse.class);
        long expiration = Instant.now().minusSeconds(3600).getEpochSecond();
        when(response.body()).thenReturn(OK_RESPONSE.formatted(expiration));
        when(response.statusCode()).thenReturn(200);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static HttpResponse<String> koResponse() {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(WRONG_RESPONSE);
        when(response.headers())
            .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));
        when(response.statusCode()).thenReturn(400);
        return response;
    }

    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        NoSuchFieldException lastException = null;

        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                lastException = e;
                clazz = clazz.getSuperclass();
            }
        }
        throw lastException;
    }
}
