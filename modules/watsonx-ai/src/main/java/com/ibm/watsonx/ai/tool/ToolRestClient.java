/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.tool.ToolService.Resources;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Utility Agent Tools APIs.
 */
public abstract class ToolRestClient extends WatsonxRestClient {

    protected ToolRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Retrieves the complete list of supported utility tools.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @return a {@link Resources} object containing the list of {@link UtilityTool} instances representing all available tools.
     */
    public abstract Resources getAll(String transactionId);

    /**
     * Retrieves metadata and configuration details for a specific tool by name.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param name The unique name of the tool to retrieve.
     * @return a {@link UtilityTool} instance containing the tool's metadata and configuration.
     */
    public abstract UtilityTool getByName(String transactionId, String name);

    /**
     * Executes the specified utility tool with the provided input and configuration.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param request A {@link ToolRequest} request.
     * @return The output string produced by the tool.
     */
    public abstract String run(String transactionId, ToolRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link ToolRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ToolRestClient.Builder builder() {
        return ServiceLoader.load(ToolRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ToolRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ToolRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks (e.g., Quarkus, Spring) to provide their own client implementations.
     */
    public interface ToolRestClientBuilderFactory extends Supplier<ToolRestClient.Builder> {}
}
