/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;

/**
 * Empirically checks whether a provider finishes one tool's arguments before emitting the next tool index. Successful runs describe only the observed
 * streams, not a protocol guarantee. Raw SSE events include metadata-only tool deltas and are saved in a unique batch directory under
 * {@code target/tool-call-streaming-order} for inspection.
 * <p>
 * These checks run with the chat and Model Gateway streaming integration tests. A failure can disprove the ordering assumption or expose a provider
 * error without implying an SDK regression.
 * <p>
 * Configure comma-separated model lists with {@code WATSONX_TOOL_ORDER_CHAT_MODELS} and {@code WATSONX_TOOL_ORDER_GATEWAY_MODELS}. Gateway models
 * default to the existing Claude, Gemma and OpenAI integration-test variables. {@code WATSONX_TOOL_ORDER_REPETITIONS} defaults to three. Repetitions
 * alternate between three calls to one function and calls to three different functions, both with required tool choice. Each request includes a fresh
 * nonce in the prompt and requested argument IDs to avoid reusing cached generations. Gemma receives an 8192-token budget. Other models receive 2048
 * tokens. Requests time out after 180 seconds. Every run must exercise multiple tools with the requested scenario and nonce. Arguments can arrive in
 * one or more fragments. Reappearing tool indices, invalid arguments at an index transition and interrupted responses fail the check. No tool is
 * executed.
 */
final class ToolCallStreamingOrderSupportTest {

    private ToolCallStreamingOrderSupportTest() {}

    private static final Duration TIMEOUT = Duration.ofSeconds(180);
    private static final Path ARTIFACT_DIRECTORY = Path.of("target", "tool-call-streaming-order",
        Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID());
    private static final List<String> CHAT_MODELS = List.of(
        "ibm/granite-4-h-small",
        "mistralai/mistral-small-3-1-24b-instruct-2503");
    private static final ChatHandler HANDLER = new ChatHandler() {
        @Override
        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {}

        @Override
        public boolean failOnFirstError() {
            return true;
        }
    };

    static Stream<DynamicTest> chatCases() {
        return cases("chat", models("WATSONX_TOOL_ORDER_CHAT_MODELS", CHAT_MODELS), testCase -> {
            var recorder = new RawEvents();
            var service = ChatService.builder()
                .baseUrl(System.getenv("WATSONX_URL"))
                .apiKey(System.getenv("WATSONX_API_KEY"))
                .projectId(System.getenv("WATSONX_PROJECT_ID"))
                .modelId(testCase.model())
                .timeout(TIMEOUT)
                .logRequests(false)
                .logResponses(recorder)
                .build();
            var request = ChatRequest.builder()
                .messages(UserMessage.text(prompt(testCase)))
                .tools(tools(testCase.distinctFunctions()))
                .parameters(ChatParameters.builder()
                    .toolChoiceOption(testCase.toolChoice())
                    .maxCompletionTokens(testCase.maxCompletionTokens())
                    .build())
                .build();
            observe(testCase, recorder, service.chatStreaming(request, HANDLER));
        });
    }

    static Stream<DynamicTest> gatewayCases() {
        var defaults = Stream.of(
            "WATSONX_GATEWAY_CHAT_MODEL_CLAUDE",
            "WATSONX_GATEWAY_CHAT_MODEL_GEMMA",
            "WATSONX_GATEWAY_CHAT_MODEL_OPENAI")
            .map(System::getenv)
            .filter(value -> value != null && !value.isBlank())
            .toList();
        var models = models("WATSONX_TOOL_ORDER_GATEWAY_MODELS", defaults);
        assertFalse(models.isEmpty(), "Set WATSONX_TOOL_ORDER_GATEWAY_MODELS or the existing gateway model variables");
        return cases("gateway", models, testCase -> {
            var recorder = new RawEvents();
            var service = ModelGatewayChatService.builder()
                .baseUrl(System.getenv("WATSONX_URL"))
                .apiKey(System.getenv("WATSONX_API_KEY"))
                .modelId(testCase.model())
                .timeout(TIMEOUT)
                .logRequests(false)
                .logResponses(recorder)
                .build();
            var request = ModelGatewayChatRequest.builder()
                .messages(UserMessage.text(prompt(testCase)))
                .tools(tools(testCase.distinctFunctions()))
                .parameters(ModelGatewayChatParameters.builder()
                    .toolChoiceOption(testCase.toolChoice())
                    .parallelToolCalls(true)
                    .maxCompletionTokens(testCase.maxCompletionTokens())
                    .build())
                .build();
            observe(testCase, recorder, service.chatStreaming(request, HANDLER));
        });
    }

    private static Stream<DynamicTest> cases(String endpoint, List<String> models, Experiment experiment) {
        var configured = System.getenv("WATSONX_TOOL_ORDER_REPETITIONS");
        int repetitions = configured == null || configured.isBlank() ? 3 : Integer.parseInt(configured.trim());
        assertTrue(repetitions > 0, "WATSONX_TOOL_ORDER_REPETITIONS must be positive");
        return models.stream().flatMap(model -> IntStream.rangeClosed(1, repetitions).mapToObj(repetition -> {
            var testCase = new TestCase(endpoint, model, repetition, repetition % 2 == 0, UUID.randomUUID().toString());
            return DynamicTest.dynamicTest(testCase.toString(), () -> experiment.run(testCase));
        }));
    }

    private static List<String> models(String variable, List<String> defaults) {
        var configured = System.getenv(variable);
        if (configured == null || configured.isBlank())
            return defaults.stream().distinct().toList();
        var models = Arrays.stream(configured.split(",")).map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
        assertFalse(models.isEmpty(), variable + " must contain at least one model");
        return models;
    }

    private static List<Tool> tools(boolean distinctFunctions) {
        var names = distinctFunctions ? List.of("draft_rome_plan", "draft_oslo_plan", "draft_lisbon_plan") : List.of("draft_city_plan");
        return names.stream().map(name -> Tool.of(name, "Draft a synthetic travel plan. This function has no external effects.",
            JsonSchema.object()
                .property("city", JsonSchema.string())
                .property("request_id", JsonSchema.string())
                .property("notes", JsonSchema.string().description("A travel plan of about 25 words"))
                .required("city", "request_id", "notes")))
            .toList();
    }

    private static String prompt(TestCase testCase) {
        var functions = testCase.distinctFunctions()
            ? "Call draft_rome_plan for Rome, draft_oslo_plan for Oslo, and draft_lisbon_plan for Lisbon."
            : "Call draft_city_plan three times, once for Rome, once for Oslo, and once for Lisbon.";
        return """
            Produce exactly THREE independent function calls in this single assistant response. Do not wait for results between calls.
            %s
            This request's unique experiment nonce is %s. Preserve it exactly in every request_id argument.
            For each call set city to the specified city and request_id to the city's lowercase name followed by a hyphen and the nonce.
            Set notes to an original English travel plan of about 25 words describing food, museums and walks.
            Each call must contain its own complete notes string.
            These are synthetic drafts. All information needed is already available. Do not ask follow-up questions or return prose.
            """.formatted(functions, testCase.nonce());
    }

    private static void observe(TestCase testCase, RawEvents recorder, CompletableFuture<ChatResponse> future) throws Exception {
        List<String> events;
        Path artifact;
        try {
            future.get(TIMEOUT.toSeconds() + 10, TimeUnit.SECONDS);
        } finally {
            if (!future.isDone())
                future.cancel(true);
            // SseEventLogger waits for its ordered logging tasks before delivering stream completion.
            events = recorder.snapshot();
            artifact = ARTIFACT_DIRECTORY.resolve(testCase.fileName());
            Files.createDirectories(artifact.getParent());
            Files.writeString(artifact, String.join("\n\n", events) + "\n\n", UTF_8);
            System.out.printf("Tool order %s: toolChoice=%s, maxCompletionTokens=%d, raw SSE events=%d, artifact=%s%n",
                testCase, testCase.toolChoice(), testCase.maxCompletionTokens(), events.size(), artifact.toAbsolutePath());
        }
        assertFalse(events.isEmpty(), testCase + ": no raw SSE captured. Enable INFO logging for SseEventLogger");
        var choices = new LinkedHashMap<Integer, ChoiceObservation>();
        for (var event : events) {
            var data = event.lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).replaceFirst("^ ", ""))
                .reduce((first, next) -> first + "\n" + next)
                .orElse("");
            if (data.isBlank() || "[DONE]".equals(data))
                continue;
            var chunk = Json.fromJson(data, PartialChatResponse.class);
            if (chunk.choices() == null)
                continue;
            for (var choice : chunk.choices()) {
                assertNotNull(choice.index(), testCase + ": raw choice has no index");
                var observed = choices.computeIfAbsent(choice.index(), ignored -> new ChoiceObservation());
                if (choice.delta() != null && choice.delta().toolCalls() != null)
                    choice.delta().toolCalls().forEach(observed::accept);
                if (choice.finishReason() != null && !choice.finishReason().isBlank())
                    observed.finishReason = choice.finishReason();
            }
        }
        choices.forEach((index, observed) -> System.out.printf("Tool order %s choice=%d: %s%n", testCase, index, observed.summary()));
        var context = testCase + ", artifact=" + artifact.toAbsolutePath();
        for (var entry : choices.entrySet()) {
            assertTrue(entry.getValue().violations.isEmpty(), context + ", choice=" + entry.getKey() + ": " + entry.getValue().violations);
        }
        for (var entry : choices.entrySet()) {
            var finishReason = entry.getValue().finishReason;
            assertTrue(finishReason == null || Set.of("tool_calls", "stop").contains(finishReason),
                context + ": choice " + entry.getKey() + " finished with " + finishReason);
        }
        assertTrue(choices.values().stream().anyMatch(choice -> choice.tools.size() >= 2),
            context + ": need at least two tool calls in the same choice");
        for (var entry : choices.entrySet()) {
            var choice = entry.getValue();
            if (choice.tools.isEmpty())
                continue;
            choice.tools.forEach((index, tool) -> {
                if (!Json.isValidObjectOrEncodedObject(tool.arguments.toString()))
                    choice.violations.add("tool " + index + " has incomplete arguments at stream end");
                if (tool.name == null)
                    choice.violations.add("tool " + index + " has no function name at stream end");
            });
            assertTrue(choice.violations.isEmpty(), context + ", choice=" + entry.getKey() + ": " + choice.violations);
        }
        assertTrue(choices.values().stream().filter(choice -> choice.tools.size() >= 2)
            .anyMatch(choice -> choice.coversScenario(testCase.distinctFunctions())),
            context + ": need at least two requested functions, or two different requested cities for draft_city_plan");
        assertTrue(choices.values().stream().flatMap(choice -> choice.tools.values().stream())
            .allMatch(tool -> tool.city() != null
                && (tool.city().toLowerCase(Locale.ROOT) + "-" + testCase.nonce()).equals(tool.argument("request_id"))),
            context + ": tool arguments did not preserve this request's unique nonce");
    }

    @FunctionalInterface
    private interface Experiment {
        void run(TestCase testCase) throws Exception;
    }

    private record TestCase(String endpoint, String model, int repetition, boolean distinctFunctions, String nonce) {
        ToolChoiceOption toolChoice() {
            return ToolChoiceOption.REQUIRED;
        }

        int maxCompletionTokens() {
            return model.toLowerCase(Locale.ROOT).contains("gemma") ? 8192 : 2048;
        }

        String fileName() {
            return endpoint + "-" + model.replaceAll("[^a-zA-Z0-9._-]", "_") + "-run-" + repetition + ".sse";
        }
    }

    private static final class RawEvents implements HttpResponseLogger {
        private final List<String> events = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void log(HttpResponseLog entry) {
            if (entry.body() != null)
                events.add(entry.body());
        }

        List<String> snapshot() {
            synchronized (events) {
                return List.copyOf(events);
            }
        }
    }

    private static final class ChoiceObservation {
        private final Map<Integer, ToolObservation> tools = new LinkedHashMap<>();
        private final List<Integer> rawIndexes = new ArrayList<>();
        private final List<Integer> transitions = new ArrayList<>();
        private final List<String> violations = new ArrayList<>();
        private final Set<Integer> abandoned = new HashSet<>();
        private Integer current;
        private String finishReason;

        void accept(ToolCall delta) {
            var index = delta.index();
            assertNotNull(index, "Raw tool delta has no index");
            rawIndexes.add(index);
            if (!index.equals(current)) {
                transitions.add(index);
                if (abandoned.contains(index))
                    violations.add("abandoned index " + index + " appeared again at raw delta " + rawIndexes.size());
                if (current != null) {
                    var previous = tools.get(current);
                    if (!Json.isValidObjectOrEncodedObject(previous.arguments.toString()))
                        violations.add("tool " + current + " arguments were incomplete when index " + index + " first appeared");
                    if (previous.name == null)
                        violations.add("tool " + current + " had no function name when index " + index + " first appeared");
                    previous.idPresentAtSwitch = previous.id != null;
                    abandoned.add(current);
                }
                current = index;
            }
            var tool = tools.computeIfAbsent(index, ignored -> new ToolObservation());
            if (delta.id() != null && !delta.id().isBlank())
                tool.id = delta.id();
            if (delta.function() != null) {
                if (delta.function().name() != null && !delta.function().name().isBlank())
                    tool.name = delta.function().name();
                if (delta.function().arguments() != null && !delta.function().arguments().isEmpty()) {
                    tool.arguments.append(delta.function().arguments());
                    tool.argumentFragments++;
                }
            }
        }

        boolean coversScenario(boolean distinctFunctions) {
            if (distinctFunctions) {
                var expected = Set.of("draft_rome_plan", "draft_oslo_plan", "draft_lisbon_plan");
                return tools.values().stream().allMatch(tool -> tool.name != null && expected.contains(tool.name))
                    && tools.values().stream().map(tool -> tool.name).distinct().count() >= 2;
            }
            var expected = Set.of("rome", "oslo", "lisbon");
            var cities = tools.values().stream().map(ToolObservation::city).filter(city -> city != null)
                .map(city -> city.trim().toLowerCase(Locale.ROOT)).filter(expected::contains).distinct().count();
            return tools.values().stream().allMatch(tool -> "draft_city_plan".equals(tool.name)) && cities >= 2;
        }

        String summary() {
            var fragments = new LinkedHashMap<Integer, Integer>();
            var metadata = new LinkedHashMap<Integer, String>();
            tools.forEach((index, tool) -> fragments.put(index, tool.argumentFragments));
            tools.forEach((index, tool) -> metadata.put(index, "name=" + tool.name + ", city=" + tool.city()
                + ", idPresent=" + (tool.id != null) + ", idPresentAtSwitch=" + tool.idPresentAtSwitch));
            return "index transitions=" + transitions + ", raw tool deltas=" + rawIndexes.size() + ", argument fragments=" + fragments
                + ", metadata=" + metadata + ", finish reason=" + finishReason + ", violations=" + violations;
        }
    }

    private static final class ToolObservation {
        private final StringBuilder arguments = new StringBuilder();
        private int argumentFragments;
        private String name;
        private String id;
        private Boolean idPresentAtSwitch;

        String city() {
            return argument("city");
        }

        String argument(String name) {
            var json = arguments.toString();
            if (!Json.isValidObjectOrEncodedObject(json))
                return null;
            var object = Json.fromJson(Json.normalizeObject(json), Object.class);
            return object instanceof Map<?, ?> map && map.get(name) instanceof String value ? value : null;
        }
    }
}
