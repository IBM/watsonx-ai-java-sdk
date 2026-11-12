/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static com.ibm.watsonx.ai.compatibility.TestArtifacts.jar;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;

@DisabledInNativeImage
class JsonProviderCompatibilityTest {

    private static Path probeClasses;
    private static Path core;
    private static Path models;
    private static Path binding2;
    private static Path binding3;
    private static List<Path> jackson2;
    private static List<Path> jackson3;

    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void locateArtifacts() throws Exception {
        probeClasses = TestArtifacts.location(JsonProviderProbe.class);
        core = jar("com.ibm.watsonx.ai.core.Json");
        models = jar("com.ibm.watsonx.ai.chat.model.AssistantMessage");
        binding2 = jar("com.ibm.watsonx.ai.jackson2.provider.JacksonProvider");
        binding3 = jar("com.ibm.watsonx.ai.jackson3.provider.JacksonProvider");
        var annotations = jar("com.fasterxml.jackson.annotation.JsonProperty");
        jackson2 = List.of(jar("com.fasterxml.jackson.databind.JsonNode"), jar("com.fasterxml.jackson.core.JsonFactory"), annotations);
        jackson3 = List.of(jar("tools.jackson.databind.JsonNode"), jar("tools.jackson.core.json.JsonFactory"), annotations);
    }

    @Test
    void should_initialize_with_invisible_tccl_and_jackson2() throws Exception {
        runOnClasspath("blind", List.of(binding2), jackson2);
    }

    @Test
    void should_initialize_with_invisible_tccl_and_jackson3() throws Exception {
        runOnClasspath("blind", List.of(binding3), jackson3);
    }

    @Test
    void should_initialize_with_null_tccl_and_jackson2() throws Exception {
        runOnClasspath("null-tccl", List.of(binding2), jackson2);
    }

    @Test
    void should_initialize_with_null_tccl_and_jackson3() throws Exception {
        runOnClasspath("null-tccl", List.of(binding3), jackson3);
    }

    @Test
    void should_report_missing_provider() throws Exception {
        runOnClasspath("missing", List.of(), List.of());
    }

    @Test
    void should_report_ambiguity_with_jackson2_first() throws Exception {
        runOnClasspath("multiple", List.of(binding2, binding3), bothJacksonVersions());
    }

    @Test
    void should_report_ambiguity_with_jackson3_first() throws Exception {
        runOnClasspath("multiple", List.of(binding3, binding2), bothJacksonVersions());
    }

    @Test
    void should_prefer_explicit_provider_with_both_bindings() throws Exception {
        var explicit = temporaryDirectory.resolve("explicit");
        var services = Files.createDirectories(explicit.resolve("META-INF/services"));
        Files.writeString(services.resolve(JsonProvider.class.getName()), JsonProviderProbe.ExplicitJsonProvider.class.getName() + "\n");
        runOnClasspath("explicit", List.of(explicit, binding2, binding3), bothJacksonVersions());
    }

    @Test
    void should_round_trip_with_jackson2_on_module_path() throws Exception {
        runOnModulePath("normal", List.of(binding2), jackson2);
    }

    @Test
    void should_round_trip_with_jackson3_on_module_path() throws Exception {
        runOnModulePath("normal", List.of(binding3), jackson3);
    }

    @Test
    void should_report_ambiguity_with_both_bindings_on_module_path() throws Exception {
        runOnModulePath("multiple", List.of(binding2, binding3), bothJacksonVersions());
    }

    private List<Path> bothJacksonVersions() {
        var dependencies = new ArrayList<>(jackson2);
        dependencies.addAll(jackson3);
        return dependencies.stream().distinct().toList();
    }

    private void runOnClasspath(String mode, List<Path> bindings, List<Path> dependencies) throws Exception {
        var classpath = new ArrayList<>(List.of(probeClasses, core, models));
        classpath.addAll(bindings);
        classpath.addAll(dependencies);
        runProbe(mode, classpath, List.of());
    }

    private void runOnModulePath(String mode, List<Path> bindings, List<Path> dependencies) throws Exception {
        var sdk = new ArrayList<>(List.of(core, models));
        sdk.addAll(bindings);
        var modulepath = new ArrayList<Path>();
        for (var artifact : sdk) {
            // Stable names also support automatic modules for the 999-SNAPSHOT development version.
            var name = artifact.getParent().getParent().getFileName() + ".jar";
            modulepath.add(Files.copy(artifact, temporaryDirectory.resolve(name)));
        }
        modulepath.addAll(dependencies);
        runProbe(mode, List.of(probeClasses), modulepath);
    }

    private void runProbe(String mode, List<Path> classpath, List<Path> modulepath) throws Exception {
        var executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
            "-cp", joinPaths(classpath)));
        if (!modulepath.isEmpty()) {
            command.addAll(List.of("--module-path", joinPaths(modulepath), "--add-modules", "ALL-MODULE-PATH"));
        }
        command.addAll(List.of(JsonProviderProbe.class.getName(), mode));
        var output = temporaryDirectory.resolve("probe-" + mode + ".log");
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            if (!process.waitFor(45, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                fail("JSON probe timed out: " + command + "\nLog: " + output + "\n" + Files.readString(output));
            }
            assertEquals(0, process.exitValue(), "JSON probe failed: " + command + "\nLog: " + output + "\n" + Files.readString(output));
        } finally {
            if (process.isAlive())
                process.destroyForcibly();
        }
    }

    private static String joinPaths(List<Path> paths) {
        return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

}
