/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import org.junit.jupiter.api.Test;

public class SseEventLoggerTest {

    @Test
    void should_forward_all_lifecycle_events_to_downstream_subscriber() {
        List<String> received = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        List<String> completed = new ArrayList<>();

        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {
                errors.add(t);
            }

            @Override
            public void onComplete() {
                completed.add("done");
            }
        };

        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (k, v) -> true);
        SseEventLogger logger = new SseEventLogger(downstream, 200, headers);

        Subscription mockSub = mock(Subscription.class);
        logger.onSubscribe(mockSub);

        logger.onNext("data: hello");
        logger.onNext("");
        logger.onNext("data: world");
        logger.onError(new RuntimeException("test-error"));
        logger.onComplete();

        assertEquals(List.of("data: hello", "", "data: world"), received);
        assertEquals(1, errors.size());
        assertEquals("test-error", errors.get(0).getMessage());
        assertEquals(1, completed.size());
    }

    @Test
    void should_accept_null_headers() {
        Flow.Subscriber<String> downstream = noopSubscriber();
        assertDoesNotThrow(() -> new SseEventLogger(downstream, 200, null));
    }

    @Test
    void should_not_log_when_blank_line_arrives_with_empty_buffer() {
        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null);
        assertDoesNotThrow(() -> logger.onNext(""));
    }

    private static Flow.Subscriber<String> noopSubscriber() {
        return new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        };
    }
}
