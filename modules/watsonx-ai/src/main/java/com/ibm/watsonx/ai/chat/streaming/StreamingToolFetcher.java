/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.UUID;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;

/**
 * The {@code StreamingToolFetcher} class is responsible for fetching a list of tools from a streaming api.
 */
public final class StreamingToolFetcher {

    private volatile String completionId;
    private volatile int index;
    private StringBuffer arguments;
    private volatile String id, name;

    public StreamingToolFetcher(String completionId, int index) {
        this.completionId = completionId;
        this.index = index;
        arguments = new StringBuffer();
    }

    public void setId(String id) {
        if (nonNull(id) && !id.isBlank())
            this.id = id;
    }

    public void setName(String name) {
        if (nonNull(name) && !name.isBlank())
            this.name = name;
    }

    public void appendArguments(String arguments) {
        if (nonNull(arguments) && !arguments.isEmpty())
            this.arguments.append(arguments);
    }

    public void setArguments(String arguments) {
        this.arguments = new StringBuffer(arguments);
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

    public CompletedToolCall build() {
        // Watsonx doesn't return "id" if the option tool-choice is set to REQUIRED.
        if (isNull(id))
            id = UUID.randomUUID().toString();

        return new CompletedToolCall(completionId, ToolCall.of(index, id, name, arguments.toString().isBlank() ? "{}" : arguments.toString()));
    }
}
