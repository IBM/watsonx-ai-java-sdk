/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.streaming.StreamingToolFetcher;

public class StreamingToolFetcherTest {

    @Test
    void should_retain_tool_properties_once_set_and_ignore_null_or_empty_updates() {

        StreamingToolFetcher fetcher = new StreamingToolFetcher("0", 0);

        fetcher.setId("id");
        assertEquals("id", fetcher.build().toolCall().id());
        fetcher.setId(null);
        assertEquals("id", fetcher.build().toolCall().id());
        fetcher.setId("");
        assertEquals("id", fetcher.build().toolCall().id());

        fetcher.setName("name");
        assertEquals("name", fetcher.build().toolCall().function().name());
        fetcher.setName(null);
        assertEquals("name", fetcher.build().toolCall().function().name());
        fetcher.setName("");
        assertEquals("name", fetcher.build().toolCall().function().name());

        fetcher.appendArguments("test");
        assertEquals("test", fetcher.build().toolCall().function().arguments());
        fetcher.appendArguments(null);
        assertEquals("test", fetcher.build().toolCall().function().arguments());
        fetcher.appendArguments("");
        assertEquals("test", fetcher.build().toolCall().function().arguments());
    }
}