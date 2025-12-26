/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ImageContent;
import com.ibm.watsonx.ai.chat.model.TextContent;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ChatMessageTest {

    @Test
    void should_return_text_when_calling_toString_on_text_content() {
        assertEquals("Hello", UserMessage.text("Hello").content().get(0).toString());
    }

    @Test
    void should_return_text_when_user_message_contains_only_a_text_content() {
        var userMessage = UserMessage.text("Hello");
        assertEquals("Hello", userMessage.text());
    }

    @Test
    void should_throw_exception_when_text_method_called_on_invalid_user_message() {
        var userMessage = new UserMessage(null, List.of(), null);
        assertThrows(IllegalStateException.class, () -> userMessage.text());

        var multipleContent = UserMessage.of(TextContent.of("Hello"), ImageContent.of("jpg", "base64"));
        assertThrows(IllegalStateException.class, () -> multipleContent.text());

        var noTextContent = UserMessage.of(ImageContent.of("jpg", "base64"));
        assertThrows(IllegalStateException.class, () -> noTextContent.text());
    }
}
