/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;

@DisabledInNativeImage
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

    @Test
    void should_not_propagate_exception_thrown_by_custom_response_logger() throws InterruptedException {
        List<String> received = new ArrayList<>();
        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        };

        HttpResponseLogger throwingLogger = entry -> {
            throw new RuntimeException("boom");
        };

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, throwingLogger, org.slf4j.event.Level.INFO);

        CountDownLatch warned = new CountDownLatch(1);
        var log4jLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(SseEventLogger.class);
        var appender = new AbstractAppender("test-capture", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                if ("Failed to log response".equals(event.getMessage().getFormattedMessage()))
                    warned.countDown();
            }
        };
        appender.start();
        log4jLogger.addAppender(appender);
        try {
            assertDoesNotThrow(() -> {
                logger.onNext("data: hello");
                logger.onNext("");
            });
            assertEquals(List.of("data: hello", ""), received);
            assertTrue(warned.await(2, TimeUnit.SECONDS), "expected a warning to be logged for the failing custom logger");
        } finally {
            log4jLogger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    void should_forward_event_to_downstream_before_custom_logger_completes() throws InterruptedException {
        List<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch loggerEntered = new CountDownLatch(1);
        CountDownLatch loggerBlock = new CountDownLatch(1);

        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        };

        HttpResponseLogger blockingLogger = entry -> {
            loggerEntered.countDown();
            awaitQuietly(loggerBlock);
        };

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, org.slf4j.event.Level.INFO);

        logger.onNext("data: hello");
        logger.onNext("");

        assertTrue(loggerEntered.await(2, TimeUnit.SECONDS), "custom logger was never invoked");
        assertEquals(List.of("data: hello", ""), received);

        loggerBlock.countDown();
    }

    @Test
    void should_run_custom_logger_on_callback_executor_not_io_executor() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        List<String> threadName = new CopyOnWriteArrayList<>();

        HttpResponseLogger customLogger = entry -> {
            threadName.add(Thread.currentThread().getName());
            done.countDown();
        };

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, customLogger, org.slf4j.event.Level.INFO);
        logger.onNext("data: hello");
        logger.onNext("");

        assertTrue(done.await(2, TimeUnit.SECONDS), "custom logger was never invoked");
        assertFalse(threadName.get(0).startsWith("http-io-"), () -> "expected the callback executor, but was: " + threadName.get(0));
    }

    @Test
    void should_deliver_custom_logger_calls_in_emission_order_even_when_the_first_is_slow() throws InterruptedException {
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(3);

        HttpResponseLogger slowFirst = entry -> {
            if (order.isEmpty())
                sleep(200);
            order.add(entry.body());
            done.countDown();
        };

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, slowFirst, org.slf4j.event.Level.INFO);

        logger.onNext("data: one");
        logger.onNext("");
        logger.onNext("data: two");
        logger.onNext("");
        logger.onNext("data: three");
        logger.onNext("");

        assertTrue(done.await(2, TimeUnit.SECONDS), "not every custom logger call completed");
        assertEquals(List.of("data: one", "data: two", "data: three"), order);
    }

    @Test
    void should_never_run_two_custom_logger_calls_at_the_same_time() throws InterruptedException {
        AtomicInteger running = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(3);

        HttpResponseLogger concurrencyTrackingLogger = entry -> {
            peak.accumulateAndGet(running.incrementAndGet(), Math::max);
            sleep(50);
            running.decrementAndGet();
            done.countDown();
        };

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, concurrencyTrackingLogger, org.slf4j.event.Level.INFO);

        logger.onNext("data: one");
        logger.onNext("");
        logger.onNext("data: two");
        logger.onNext("");
        logger.onNext("data: three");
        logger.onNext("");

        assertTrue(done.await(2, TimeUnit.SECONDS), "not every custom logger call completed");
        assertEquals(1, peak.get(), "two custom logger calls overlapped");
    }

    @Test
    void should_defer_on_complete_until_the_pending_log_finishes() throws InterruptedException {
        CountDownLatch loggerEntered = new CountDownLatch(1);
        CountDownLatch loggerBlock = new CountDownLatch(1);
        CountDownLatch downstreamCompleted = new CountDownLatch(1);

        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {
                downstreamCompleted.countDown();
            }
        };

        HttpResponseLogger blockingLogger = entry -> {
            loggerEntered.countDown();
            awaitQuietly(loggerBlock);
        };

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, org.slf4j.event.Level.INFO);

        logger.onNext("data: hello");
        logger.onNext("");
        assertTrue(loggerEntered.await(2, TimeUnit.SECONDS), "custom logger was never invoked");

        logger.onComplete();
        assertFalse(downstreamCompleted.await(300, TimeUnit.MILLISECONDS), "onComplete propagated before the pending log finished");

        loggerBlock.countDown();
        assertTrue(downstreamCompleted.await(2, TimeUnit.SECONDS), "onComplete was never propagated once the pending log finished");
    }

    @Test
    void should_defer_on_error_until_the_pending_log_finishes() throws InterruptedException {
        CountDownLatch loggerEntered = new CountDownLatch(1);
        CountDownLatch loggerBlock = new CountDownLatch(1);
        CountDownLatch downstreamErrored = new CountDownLatch(1);

        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable t) {
                downstreamErrored.countDown();
            }

            @Override
            public void onComplete() {}
        };

        HttpResponseLogger blockingLogger = entry -> {
            loggerEntered.countDown();
            awaitQuietly(loggerBlock);
        };

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, org.slf4j.event.Level.INFO);

        logger.onNext("data: hello");
        logger.onNext("");
        assertTrue(loggerEntered.await(2, TimeUnit.SECONDS), "custom logger was never invoked");

        logger.onError(new RuntimeException("boom"));
        assertFalse(downstreamErrored.await(300, TimeUnit.MILLISECONDS), "onError propagated before the pending log finished");

        loggerBlock.countDown();
        assertTrue(downstreamErrored.await(2, TimeUnit.SECONDS), "onError was never propagated once the pending log finished");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
