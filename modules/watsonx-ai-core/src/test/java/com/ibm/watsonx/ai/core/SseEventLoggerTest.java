/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.OFF;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
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

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, throwingLogger, Level.INFO);

        CountDownLatch warned = new CountDownLatch(1);
        var log4jLogger = (Logger) LogManager.getLogger(SseEventLogger.class);
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

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, Level.INFO);

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

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, customLogger, Level.INFO);
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

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, slowFirst, Level.INFO);

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

        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, concurrencyTrackingLogger, Level.INFO);

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

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, Level.INFO);

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

        SseEventLogger logger = new SseEventLogger(downstream, 200, null, blockingLogger, Level.INFO);

        logger.onNext("data: hello");
        logger.onNext("");
        assertTrue(loggerEntered.await(2, TimeUnit.SECONDS), "custom logger was never invoked");

        logger.onError(new RuntimeException("boom"));
        assertFalse(downstreamErrored.await(300, TimeUnit.MILLISECONDS), "onError propagated before the pending log finished");

        loggerBlock.countDown();
        assertTrue(downstreamErrored.await(2, TimeUnit.SECONDS), "onError was never propagated once the pending log finished");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_sanitize_complete_events_without_changing_downstream_data(boolean custom) throws InterruptedException {
        List<String> lines = List.of(
            ": metadata-secret",
            "event: metadata-secret",
            "id: metadata-secret",
            "retry: metadata-secret",
            "unknown: metadata-secret",
            "data:{\"events\":[",
            "data: {\"apiKey\":\"json-secret\",\"password\":\"escaped\\\"secret\"},",
            "data: {\"credentials\":{\"custom\":\"opaque-secret\"}}]}",
            "",
            "data:{\"password\":\"malformed-secret\"",
            "",
            "data: [DONE]",
            "");
        HttpHeaders headers = HttpHeaders.of(Map.of(
            "sEt-CoOkIe", List.of("session=header-secret"),
            "Content-Type", List.of("text/event-stream")), (k, v) -> true);
        List<String> received = new ArrayList<>();
        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {}

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        };

        List<HttpResponseLog> entries = new CopyOnWriteArrayList<>();
        CountDownLatch logged = new CountDownLatch(3);
        HttpResponseLogger responseLogger = custom ? entry -> {
            entries.add(entry);
            logged.countDown();
        } : null;
        List<String> logs = new CopyOnWriteArrayList<>();
        var appender = capturingAppender(logs);
        var log4jLogger = (Logger) LogManager.getLogger(SseEventLogger.class);
        log4jLogger.addAppender(appender);
        try {
            SseEventLogger logger = new SseEventLogger(downstream, 200, headers, responseLogger, Level.INFO);
            lines.forEach(logger::onNext);
            if (custom)
                assertTrue(logged.await(2, TimeUnit.SECONDS), "not every custom log was delivered");

            String diagnostic = custom ? entries.stream().map(SseEventLoggerTest::describe).reduce("", String::concat) : String.join("\n", logs);
            for (String secret : List.of("metadata-secret", "json-secret", "escaped", "opaque-secret", "malformed-secret", "header-secret"))
                assertFalse(diagnostic.contains(secret), () -> "Secret found in diagnostic event: " + diagnostic);
            assertTrue(diagnostic.contains("***"));
            assertFalse(diagnostic.contains("SSE metadata omitted"));
            assertTrue(diagnostic.contains("data: [DONE]"));
            assertEquals(lines, received);
            assertEquals("session=header-secret", headers.firstValue("Set-Cookie").orElseThrow());
        } finally {
            log4jLogger.removeAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_preserve_empty_and_multiline_data_fields(boolean custom) throws InterruptedException {
        List<String> lines = List.of(
            "event: omitted",
            "datax: omitted",
            "Data: omitted",
            " ",
            "data",
            "",
            "data:",
            "\t",
            "data: ",
            "",
            "data:  leading",
            "data:\tkept",
            "data",
            "data: tail",
            "",
            "id: omitted\ndata: first\n\nevent: omitted\ndata: second\ndata:\n",
            "\r\n");
        List<String> received = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);
        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {}

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {
                completed.countDown();
            }
        };
        List<String> bodies = new CopyOnWriteArrayList<>();
        HttpResponseLogger responseLogger = custom ? entry -> bodies.add(entry.body()) : null;
        List<String> logs = new CopyOnWriteArrayList<>();
        var appender = capturingAppender(logs);
        var log4jLogger = (Logger) LogManager.getLogger(SseEventLogger.class);
        log4jLogger.addAppender(appender);
        try {
            SseEventLogger logger = new SseEventLogger(downstream, 200, null, responseLogger, Level.INFO);
            lines.forEach(logger::onNext);
            logger.onComplete();
            assertTrue(completed.await(2, TimeUnit.SECONDS), "the downstream completion was not delivered");
            String marker = "\n- body: ";
            List<String> actual = custom ? bodies : logs.stream().map(log -> log.substring(log.indexOf(marker) + marker.length())).toList();
            assertEquals(List.of("data: ", "data: ", "data: ", "data:  leading\n\tkept\n\ntail", "data: first\nsecond\n"), actual);
            assertEquals(lines, received);
            for (int index = 0; index < lines.size(); index++)
                assertSame(lines.get(index), received.get(index));
        } finally {
            log4jLogger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    void should_evaluate_the_log_level_when_the_event_ends() throws InterruptedException {
        List<String> bodies = new CopyOnWriteArrayList<>();
        CountDownLatch logged = new CountDownLatch(2);
        HttpResponseLogger responseLogger = entry -> {
            bodies.add(entry.body());
            logged.countDown();
        };
        var log4jLogger = (Logger) LogManager.getLogger(SseEventLogger.class);
        var previousLevel = log4jLogger.getLevel();
        SseEventLogger logger = new SseEventLogger(noopSubscriber(), 200, null, responseLogger, Level.INFO);
        try {
            log4jLogger.setLevel(OFF);
            logger.onNext("data: first");
            log4jLogger.setLevel(INFO);
            logger.onNext("");
            logger.onNext("data: discarded");
            log4jLogger.setLevel(OFF);
            logger.onNext("");
            log4jLogger.setLevel(INFO);
            logger.onNext("data: second");
            logger.onNext("");
            assertTrue(logged.await(2, TimeUnit.SECONDS), "not every enabled event was logged");
            assertEquals(List.of("data: first", "data: second"), bodies);
        } finally {
            log4jLogger.setLevel(previousLevel);
        }
    }

    @Test
    void should_sanitize_stream_errors_and_logger_failures_without_changing_the_downstream_error() throws InterruptedException {
        RuntimeException original = new RuntimeException("apiKey=stream-secret", new RuntimeException("cause-secret"));
        original.addSuppressed(new RuntimeException("suppressed-secret"));
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        CountDownLatch errored = new CountDownLatch(1);
        Flow.Subscriber<String> downstream = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {}

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable throwable) {
                receivedError.set(throwable);
                errored.countDown();
            }

            @Override
            public void onComplete() {}
        };
        HttpResponseLogger failingLogger = entry -> {
            throw new RuntimeException("password=callback-secret", new RuntimeException("callback-cause-secret"));
        };

        List<String> logs = new CopyOnWriteArrayList<>();
        var appender = capturingAppender(logs);
        var log4jLogger = (Logger) LogManager.getLogger(SseEventLogger.class);
        log4jLogger.addAppender(appender);
        try {
            SseEventLogger logger = new SseEventLogger(downstream, 200, null, failingLogger, Level.INFO);
            logger.onNext("data: hello");
            logger.onNext("");
            logger.onError(original);
            assertTrue(errored.await(2, TimeUnit.SECONDS), "the downstream error was not delivered");
            assertSame(original, receivedError.get());
            String diagnostic = String.join("\n", logs);
            for (String secret : List.of("stream-secret", "cause-secret", "suppressed-secret", "callback-secret", "callback-cause-secret"))
                assertFalse(diagnostic.contains(secret), () -> "Secret found in diagnostic stack trace: " + diagnostic);
            assertTrue(diagnostic.contains("SSE stream failed"));
            assertTrue(diagnostic.contains("Failed to log response"));
            assertTrue(diagnostic.contains("SseEventLoggerTest.java:"));
        } finally {
            log4jLogger.removeAppender(appender);
            appender.stop();
        }
    }

    private static AbstractAppender capturingAppender(List<String> logs) {
        var appender = new AbstractAppender("sse-sanitization-capture", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                logs.add(event.getMessage().getFormattedMessage() + stackTrace(event.getThrown()));
            }
        };
        appender.start();
        return appender;
    }

    private static String describe(HttpResponseLog entry) {
        return "requestId=" + entry.requestId() + ", status=" + entry.statusCode() + ", headers=" + entry.headers() + ", uri=" + entry.uri()
            + ", body=" + entry.body() + ", error=" + stackTrace(entry.error());
    }

    private static String stackTrace(Throwable error) {
        if (error == null)
            return "";
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
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
