/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.core.Json;

@DisabledInNativeImage
class NativeMetadataTest {

    @Test
    void should_reference_existing_classes_in_jackson2_native_configuration() throws Exception {
        verifyMetadata("jackson2");
    }

    @Test
    void should_reference_existing_classes_in_jackson3_native_configuration() throws Exception {
        verifyMetadata("jackson3");
    }

    private static void verifyMetadata(String binding) throws Exception {
        var provider = Class.forName("com.ibm.watsonx.ai." + binding + ".provider.JacksonProvider", false,
            NativeMetadataTest.class.getClassLoader());
        var bindingJar = TestArtifacts.location(provider);
        var jars = List.of(TestArtifacts.location(Json.class), TestArtifacts.location(AssistantMessage.class), bindingJar);
        for (var jar : jars)
            assertTrue(Files.isRegularFile(jar) && Files.isReadable(jar),
                () -> "Expected a readable SDK jar at " + jar + ". Run ./mvnw verify from the repository root.");

        var config = "META-INF/native-image/com.ibm.watsonx/watsonx-ai-" + binding + "/";
        Set<String> references = new TreeSet<>();
        try (var archive = new ZipFile(bindingJar.toFile())) {
            var metadata = readResource(archive, config + "reachability-metadata.json");
            var matcher = Pattern.compile("\"(com\\.ibm\\.watsonx\\.[^\"]+)\"").matcher(metadata);
            while (matcher.find())
                references.add(matcher.group(1));
            assertFalse(references.isEmpty(), "No SDK class references in native metadata for " + binding);

            var initialization = archive.getEntry(config + "native-image.properties");
            assertNotNull(initialization, "Missing native-image.properties in " + bindingJar);
            var properties = new Properties();
            try (var input = archive.getInputStream(initialization)) {
                properties.load(input);
            }
            matcher = Pattern.compile("--initialize-at-build-time=([^\\s]+)").matcher(properties.getProperty("Args", ""));
            assertTrue(matcher.find(), "Missing build-time initialization in " + bindingJar);
            do {
                for (var name : matcher.group(1).split(",")) {
                    if (name.startsWith("com.ibm.watsonx."))
                        references.add(name);
                }
            } while (matcher.find());
        }

        Set<String> classes = new TreeSet<>();
        for (var jar : jars) {
            try (var archive = new ZipFile(jar.toFile())) {
                archive.stream().filter(entry -> entry.getName().endsWith(".class"))
                    .forEach(entry -> classes.add(entry.getName().replace('/', '.').replaceFirst("\\.class$", "")));
            }
        }

        Set<String> missing = new TreeSet<>();
        for (var reference : references) {
            var component = reference;
            while (component.endsWith("[]"))
                component = component.substring(0, component.length() - 2);
            if (!classes.contains(component))
                missing.add(component);
        }
        assertTrue(missing.isEmpty(), () -> "Native configuration for " + binding + " references missing classes: " + missing);
    }

    private static String readResource(ZipFile archive, String name) throws IOException {
        var entry = archive.getEntry(name);
        assertNotNull(entry, "Missing " + name + " in " + archive.getName());
        try (var input = archive.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
