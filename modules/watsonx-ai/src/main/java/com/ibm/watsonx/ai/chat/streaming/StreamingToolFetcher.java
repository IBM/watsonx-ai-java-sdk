/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;

/**
 * Accumulates tool call data from streaming API chunks.
 * <p>
 * This class is responsible for incrementally building a complete {@link ToolCall} from partial data received during streaming.
 */
public final class StreamingToolFetcher {

    private volatile String completionId;
    private volatile int choiceIndex;
    private volatile int toolIndex;
    private StringBuffer arguments;
    private volatile String id, name;
    private final AtomicBoolean argumentsEmitted = new AtomicBoolean();

    public StreamingToolFetcher(String completionId, int choiceIndex, int toolIndex) {
        this.completionId = completionId;
        this.choiceIndex = choiceIndex;
        this.toolIndex = toolIndex;
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

    public int getToolIndex() {
        return toolIndex;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getChoiceIndex() {
        return choiceIndex;
    }

    /**
     * Marks that an arguments fragment has been emitted for this tool call.
     *
     * @return {@code true} the first time it is called, {@code false} afterwards
     */
    public boolean markArgumentsEmitted() {
        return argumentsEmitted.compareAndSet(false, true);
    }

    public CompletedToolCall build() {
        // Watsonx doesn't return "id" if the option tool-choice is set to REQUIRED.
        if (isNull(id))
            id = UUID.randomUUID().toString();

        return new CompletedToolCall(completionId, choiceIndex,
            ToolCall.of(toolIndex, id, name, arguments.toString().isBlank() ? "{}" : arguments.toString()));
    }
}
