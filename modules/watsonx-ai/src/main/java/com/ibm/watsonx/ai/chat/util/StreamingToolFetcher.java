/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.UUID;
import com.ibm.watsonx.ai.chat.model.ToolCall;

/**
 * The {@code StreamingToolFetcher} class is responsible for fetching a list of tools from a streaming api.
 */
public final class StreamingToolFetcher {

    public record PartialToolCall(int index, String id, String name, String arguments) {}

    private int index;
    private StringBuilder arguments;
    private String id, name;

    public StreamingToolFetcher(int index) {
        this.index = index;
        arguments = new StringBuilder();
    }

    public void setId(String id) {
        if (nonNull(id))
            this.id = id;
    }

    public void setName(String name) {
        if (nonNull(name) && !name.isBlank())
            this.name = name;
    }

    public void appendArguments(String arguments) {
        if (nonNull(arguments))
            this.arguments.append(arguments);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public ToolCall build() {
        // Watsonx doesn't return "id" if the option tool-choice is set to REQUIRED.
        if (isNull(id)) {
            this.id = UUID.randomUUID().toString();
        }
        return ToolCall.of(index, id, name, arguments.toString().isBlank() ? "{}" : arguments.toString());
    }
}
