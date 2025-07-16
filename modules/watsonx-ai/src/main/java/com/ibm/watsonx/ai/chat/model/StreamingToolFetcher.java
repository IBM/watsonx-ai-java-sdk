/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.UUID;

/**
 * The {@code StreamingToolFetcher} class is responsible for fetching a list of tools from a streaming api.
 */
public class StreamingToolFetcher {

    private int index;
    private StringBuilder arguments;
    private String id, type, name;

    public StreamingToolFetcher(int index) {
        this.index = index;
        arguments = new StringBuilder();
    }

    public void setId(String id) {
        if (nonNull(id))
            this.id = id;
    }

    public void setType(String type) {
        if (nonNull(type))
            this.type = type;
    }

    public void setName(String name) {
        if (nonNull(name) && !name.isBlank())
            this.name = name;
    }

    public void appendArguments(String arguments) {
        if (nonNull(arguments))
            this.arguments.append(arguments);
    }

    public ToolCall build() {
        // Watsonx doesn't return "id" if the option tool-choice is set to REQUIRED.
        if (isNull(id)) {
            this.id = UUID.randomUUID().toString();
        }
        return new ToolCall(index, id, type, FunctionCall.of(name, arguments.toString()));
    }
}
