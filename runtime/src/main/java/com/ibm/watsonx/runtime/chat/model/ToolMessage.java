package com.ibm.watsonx.runtime.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a message from a tool in response to a tool call initiated by the assistant.
 * <p>
 * This message is used to return the result of a previously issued {@link ToolCall} back to the assistant.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * ToolMessage.of("The result is 4", "3425fcab-9740-40b8-99b5-3d8884156221");
 * }</pre>
 *
 * @param role the role of the message's author, always {@code tool}
 * @param content the content returned by the tool
 * @param toolCallId the identifier of the tool call to which this message is responding
 */
public final record ToolMessage(String role, String content, String toolCallId) implements ChatMessage {

    public static final String ROLE = "tool";

    public ToolMessage {
        role = ROLE;
        requireNonNull(content);
        requireNonNull(toolCallId);
    }

    /**
     * Creates a new {@link ToolMessage}.
     *
     * @param content the content of the tool message
     * @param toolCallId the ID of the tool call to which this message responds
     * @return a new {@link ToolMessage}
     */
    public static ToolMessage of(String content, String toolCallId) {
        return new ToolMessage(ROLE, content, toolCallId);
    }
}