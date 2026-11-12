/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.compatibility;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.jackson2.provider.JacksonProvider;

public class JsonProviderProbe {

    private static boolean explicitSerializationUsed;
    private static boolean explicitDeserializationUsed;

    public static void main(String[] args) throws Exception {
        switch(args[0]) {
            case "blind", "null-tccl" -> firstCallOnWorker(args[0]);
            case "missing", "multiple" -> initializationFailure(args[0]);
            case "explicit" -> {
                roundTrip();
                check(explicitSerializationUsed, "Serialization did not use the explicit provider");
                check(explicitDeserializationUsed, "Deserialization did not use the explicit provider");
            }
            case "normal" -> roundTrip();
            default -> throw new IllegalArgumentException("Unknown probe: " + args[0]);
        }
    }

    private static void firstCallOnWorker(String mode) throws InterruptedException {
        var failure = new AtomicReference<Throwable>();
        var worker = new Thread(() -> {
            var thread = Thread.currentThread();
            var original = thread.getContextClassLoader();
            try (var empty = new URLClassLoader(new URL[0], null)) {
                thread.setContextClassLoader(mode.equals("null-tccl") ? null : empty);
                roundTrip();
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                thread.setContextClassLoader(original);
            }
        });
        worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
        worker.start();
        worker.join();
        if (failure.get() != null)
            throw new AssertionError("The first JSON operation failed with " + mode, failure.get());
        roundTrip();
    }

    private static void initializationFailure(String mode) {
        var first = failure(() -> Json.toJson(Map.of("probe", "ok")));
        check(first instanceof ExceptionInInitializerError, "Expected ExceptionInInitializerError, found: " + first);
        var cause = first.getCause();
        check(cause instanceof IllegalStateException, "Expected IllegalStateException as the cause, found: " + cause);
        var message = cause.getMessage();
        check(message != null, "Missing provider diagnostic");
        if (mode.equals("multiple")) {
            check(message.contains("Multiple JsonProvider implementations found"), message);
            check(message.contains("com.ibm.watsonx.ai.jackson2.provider.JacksonProvider"), message);
            check(message.contains("com.ibm.watsonx.ai.jackson3.provider.JacksonProvider"), message);
            check(!message.contains("@"), "Provider diagnostic contains identity hashes: " + message);
        } else {
            check(message.contains("No JsonProvider found"), message);
            check(message.contains("watsonx-ai-jackson2") && message.contains("watsonx-ai-jackson3"), message);
            check(message.contains("META-INF/services/" + JsonProvider.class.getName()), message);
        }
        var second = failure(() -> Json.toJson(Map.of("probe", "ok")));
        check(second instanceof NoClassDefFoundError, "Expected NoClassDefFoundError on the second call, found: " + second);
    }

    private static Throwable failure(Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            return error;
        }
        throw new AssertionError("Expected the JSON operation to fail");
    }

    private static void roundTrip() {
        var original = AssistantMessage.tools(List.of(ToolCall.of("1", "getWeather", "{\"city\":\"Rome\"}")));
        var result = Json.fromJson(Json.toJson(original), AssistantMessage.class);
        check(original.equals(result), "AssistantMessage round trip differs: " + result);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    public static class ExplicitJsonProvider implements JsonProvider {

        private final JsonProvider delegate = new JacksonProvider();

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            explicitDeserializationUsed = true;
            return delegate.fromJson(json, type);
        }

        @Override
        public <T> T fromJson(String json, TypeToken<T> type) {
            explicitDeserializationUsed = true;
            return delegate.fromJson(json, type);
        }

        @Override
        public String toJson(Object value) {
            explicitSerializationUsed = true;
            return delegate.toJson(value);
        }

        @Override
        public String prettyPrint(Object value) {
            return delegate.prettyPrint(value);
        }

        @Override
        public boolean isValidObject(String value) {
            return delegate.isValidObject(value);
        }
    }
}
