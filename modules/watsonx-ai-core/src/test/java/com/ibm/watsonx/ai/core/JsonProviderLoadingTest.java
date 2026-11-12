/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;

@DisabledInNativeImage
public class JsonProviderLoadingTest {

    public static class SecondStubJsonProvider extends StubJsonProvider {}

    private static ClassLoader seesStub() {
        return JsonProviderLoadingTest.class.getClassLoader();
    }

    private static ClassLoader seesNothing() {
        return ClassLoader.getPlatformClassLoader();
    }

    private static ClassLoader failsWhenUsed() {
        return new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) {
                throw new AssertionError("The fallback class loader must not be used");
            }
        };
    }

    private static ClassLoader servicesFrom(String resource) {
        var parent = seesStub();
        return new ClassLoader(parent) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return name.equals("META-INF/services/" + JsonProvider.class.getName())
                    ? parent.getResources(resource)
                    : super.getResources(name);
            }
        };
    }

    @Test
    void load_provider_should_use_the_fallback_when_the_primary_finds_nothing() {
        assertInstanceOf(StubJsonProvider.class, Json.loadProvider(seesNothing(), seesStub()));
    }

    @Test
    void load_provider_should_not_use_the_fallback_when_the_primary_finds_a_provider() {
        assertInstanceOf(StubJsonProvider.class, Json.loadProvider(seesStub(), failsWhenUsed()));
    }

    @Test
    void load_provider_should_name_the_bindings_when_no_class_loader_finds_a_provider() {
        var ex = assertThrows(IllegalStateException.class, () -> Json.loadProvider(seesNothing(), seesNothing()));
        assertTrue(ex.getMessage().contains("watsonx-ai-jackson2"));
        assertTrue(ex.getMessage().contains("watsonx-ai-jackson3"));
        assertTrue(ex.getMessage().contains("META-INF/services/com.ibm.watsonx.ai.core.spi.json.JsonProvider"));
    }

    @Test
    void resolve_provider_should_list_the_class_names_when_multiple_providers() {
        var ex = assertThrows(IllegalStateException.class,
            () -> Json.resolveProvider(List.of(new StubJsonProvider(), new JsonTest.StubJsonProvider(false))));
        assertTrue(ex.getMessage().contains(StubJsonProvider.class.getName()));
        assertTrue(ex.getMessage().contains(JsonTest.StubJsonProvider.class.getName()));
        assertFalse(ex.getMessage().contains("@"));
    }

    @Test
    void load_provider_should_not_use_the_fallback_when_the_primary_finds_multiple_providers() {
        var ex = assertThrows(IllegalStateException.class,
            () -> Json.loadProvider(servicesFrom("json-providers/two-providers.txt"), failsWhenUsed()));
        assertTrue(ex.getMessage().contains("Multiple JsonProvider implementations found"));
    }

    @Test
    void load_provider_should_not_use_the_fallback_when_the_primary_has_a_broken_provider_descriptor() {
        var ex = assertThrows(ServiceConfigurationError.class,
            () -> Json.loadProvider(servicesFrom("json-providers/missing-provider.txt"), failsWhenUsed()));
        assertTrue(ex.getMessage().contains("MissingJsonProvider"));
    }
}
