/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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

    @Test
    void should_create_an_user_message_with_text_and_image_contents() throws IOException, URISyntaxException {
        var file = new File(ClassLoader.getSystemResource("alien.jpg").toURI());
        var userMessage = UserMessage.image("Describe this image", file);
        assertEquals(2, userMessage.content().size());
        assertEquals("Describe this image", userMessage.content().get(0).toString());
        var imageContent = ((ImageContent) userMessage.content().get(1)).imageUrl();
        assertTrue(imageContent.url().startsWith("data:image/jpeg;base64,"));
        assertEquals("auto", imageContent.detail());
    }

    @Test
    void should_throw_runtime_exception_when_image_file_does_not_exist() {
        var file = new File("non-existent-file.jpg");
        assertThrows(RuntimeException.class, () -> UserMessage.image("Describe this image", file));
    }

    @Test
    void should_throw_runtime_exeception_when_image_file_is_corrupted() throws IOException, URISyntaxException {
        var is = mock(InputStream.class);
        when(is.readAllBytes()).thenThrow(new IOException());
        assertThrows(RuntimeException.class, () -> UserMessage.image("Describe this image", is, "image/jpeg"));
    }
}
