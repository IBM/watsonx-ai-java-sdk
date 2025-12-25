/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ChatMessageTest {

    @Test
    void should_return_text_when_calling_toString_on_text_content() {
        assertEquals("Hello", UserMessage.text("Hello").content().get(0).toString());
    }
}
