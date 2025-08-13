/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher;

public class StreamingToolFetcherTest {

    @Test
    void test_streaming_tool_fetcher() {

        StreamingToolFetcher fetcher = new StreamingToolFetcher(0);

        fetcher.setName("name");
        assertEquals("name", fetcher.build().function().name());
        fetcher.setName(null);
        assertEquals("name", fetcher.build().function().name());
        fetcher.setName("");
        assertEquals("name", fetcher.build().function().name());

        fetcher.appendArguments("test");
        assertEquals("test", fetcher.build().function().arguments());
        fetcher.appendArguments(null);
        assertEquals("test", fetcher.build().function().arguments());
    }
}