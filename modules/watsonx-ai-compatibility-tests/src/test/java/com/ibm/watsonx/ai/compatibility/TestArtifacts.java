/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.compatibility;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestArtifacts {

    private TestArtifacts() {}

    static Path location(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    static Path jar(String className) throws Exception {
        var type = Class.forName(className, false, TestArtifacts.class.getClassLoader());
        var path = location(type);
        assertTrue(Files.isRegularFile(path) && Files.isReadable(path),
            () -> "Expected a packaged JAR for " + className + ": " + path + ". Run ./mvnw verify from the repository root.");
        return path;
    }
}
