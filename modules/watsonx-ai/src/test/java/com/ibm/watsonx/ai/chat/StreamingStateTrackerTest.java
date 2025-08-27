/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.ibm.watsonx.ai.chat.util.StreamingStateTracker.State.NO_THINKING;
import static com.ibm.watsonx.ai.chat.util.StreamingStateTracker.State.RESPONSE;
import static com.ibm.watsonx.ai.chat.util.StreamingStateTracker.State.START;
import static com.ibm.watsonx.ai.chat.util.StreamingStateTracker.State.THINKING;
import static com.ibm.watsonx.ai.chat.util.StreamingStateTracker.State.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.util.StreamingStateTracker;
import com.ibm.watsonx.ai.chat.util.StreamingStateTracker.Result;

public class StreamingStateTrackerTest {

    @Test
    public void streaming_state_tracker_test_1() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think", "response"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("<think"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update(">I"));
        assertEquals(new Result(THINKING, Optional.of("'m ")), tracker.update("'m "));
        assertEquals(new Result(THINKING, Optional.of("thinking")), tracker.update("thinking"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("</"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("><"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("res"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("ponse"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update(">"));
        assertEquals(new Result(RESPONSE, Optional.of("Hell")), tracker.update("Hell"));
        assertEquals(new Result(RESPONSE, Optional.of("o")), tracker.update("o"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update("</"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update("response"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update(">"));
    }

    @Test
    public void streaming_state_tracker_test_2() {
        StreamingStateTracker tracker = new StreamingStateTracker(ExtractionTags.of("think"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("<"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update(">"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update("I"));
        assertEquals(new Result(THINKING, Optional.of("'m ")), tracker.update("'m "));
        assertEquals(new Result(THINKING, Optional.of("thinking")), tracker.update("thinking"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("</"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(RESPONSE, Optional.of("H")), tracker.update(">H"));
        assertEquals(new Result(RESPONSE, Optional.of("ell")), tracker.update("ell"));
        assertEquals(new Result(RESPONSE, Optional.of("o")), tracker.update("o"));
    }

    @Test
    public void streaming_state_tracker_test_3() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("<think"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update(">I"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update("I"));
        assertEquals(new Result(THINKING, Optional.of("'m ")), tracker.update("'m "));
        assertEquals(new Result(THINKING, Optional.of("<thinking")), tracker.update("<thinking"));
        assertEquals(new Result(THINKING, Optional.of("/>")), tracker.update("/>"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("</"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update("think>"));
        assertEquals(new Result(RESPONSE, Optional.of("H")), tracker.update("H"));
        assertEquals(new Result(RESPONSE, Optional.of("ell")), tracker.update("ell"));
        assertEquals(new Result(RESPONSE, Optional.of("o")), tracker.update("o"));
    }

    @Test
    public void streaming_state_tracker_test_4() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("<think"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update(">I"));
        assertEquals(new Result(THINKING, Optional.of("I")), tracker.update("I"));
        assertEquals(new Result(THINKING, Optional.of("'m ")), tracker.update("'m "));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("<think"));
        assertEquals(new Result(THINKING, Optional.of("<thinking")), tracker.update("ing"));
        assertEquals(new Result(THINKING, Optional.of("/>")), tracker.update("/>"));
        assertEquals(new Result(THINKING, Optional.of(".")), tracker.update(".</"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update(">"));
        assertEquals(new Result(RESPONSE, Optional.of("Hell")), tracker.update("Hell"));
        assertEquals(new Result(RESPONSE, Optional.of("o")), tracker.update("o"));
    }

    @Test
    public void streaming_state_tracker_test_5() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think", "response"));
        assertEquals(new Result(START, Optional.empty()), tracker.update(""));
        assertEquals(new Result(START, Optional.empty()), tracker.update("\\u003c"));
        assertEquals(new Result(START, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("\\u003e"));
        assertEquals(new Result(THINKING, Optional.of("The ")), tracker.update("The "));
        assertEquals(new Result(THINKING, Optional.of("transation")), tracker.update("transation"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("\\u003c/"));
        assertEquals(new Result(THINKING, Optional.empty()), tracker.update("think"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("\\u003e\\u003c"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("response"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update("\\u003e"));
        assertEquals(new Result(RESPONSE, Optional.of("###")), tracker.update("###"));
        assertEquals(new Result(RESPONSE, Optional.of("Translation")), tracker.update("Translation"));
        assertEquals(new Result(RESPONSE, Optional.of(" re")), tracker.update(" re"));
        assertEquals(new Result(RESPONSE, Optional.of("spons")), tracker.update("spons"));
        assertEquals(new Result(RESPONSE, Optional.of("e")), tracker.update("e"));
        assertEquals(new Result(RESPONSE, Optional.of("is")), tracker.update("is"));
        assertEquals(new Result(RESPONSE, Optional.of(".")), tracker.update(".\\u003c/"));
        assertEquals(new Result(RESPONSE, Optional.empty()), tracker.update("response"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update("\\u003e"));
        assertEquals(new Result(UNKNOWN, Optional.empty()), tracker.update(""));
    }

    @Test
    public void streaming_state_tracker_test_6() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think", "response"));
        assertEquals(new Result(START, Optional.empty()), tracker.update(""));
        assertEquals(new Result(START, Optional.empty()), tracker.update("\\u003c"));
        assertEquals(new Result(NO_THINKING, Optional.of("<thinks")), tracker.update("thinks"));
        assertEquals(new Result(NO_THINKING, Optional.of(">")), tracker.update("\\u003e"));
        assertEquals(new Result(NO_THINKING, Optional.of("The ")), tracker.update("The "));
        assertEquals(new Result(NO_THINKING, Optional.of("transation")), tracker.update("transation"));
        assertEquals(new Result(NO_THINKING, Optional.of("<")), tracker.update("<"));
        assertEquals(new Result(NO_THINKING, Optional.of("think")), tracker.update("think"));
        assertEquals(new Result(NO_THINKING, Optional.of("><")), tracker.update("><"));
        assertEquals(new Result(NO_THINKING, Optional.of("response")), tracker.update("response"));
        assertEquals(new Result(NO_THINKING, Optional.of(">")), tracker.update("\\u003e"));
        assertEquals(new Result(NO_THINKING, Optional.of("###")), tracker.update("###"));
        assertEquals(new Result(NO_THINKING, Optional.of("Translation")), tracker.update("Translation"));
        assertEquals(new Result(NO_THINKING, Optional.of(" re")), tracker.update(" re"));
        assertEquals(new Result(NO_THINKING, Optional.of("spons")), tracker.update("spons"));
        assertEquals(new Result(NO_THINKING, Optional.of("e")), tracker.update("e"));
        assertEquals(new Result(NO_THINKING, Optional.of("is")), tracker.update("is"));
        assertEquals(new Result(NO_THINKING, Optional.of(".</")), tracker.update(".\\u003c/"));
        assertEquals(new Result(NO_THINKING, Optional.of("response")), tracker.update("response"));
        assertEquals(new Result(NO_THINKING, Optional.of(">")), tracker.update("\\u003e"));
        assertEquals(new Result(NO_THINKING, Optional.empty()), tracker.update(""));
    }

    @Test
    public void streaming_state_tracker_test_7() {
        StreamingStateTracker tracker = new StreamingStateTracker(new ExtractionTags("think", "response"));
        assertEquals(new Result(NO_THINKING, Optional.of("He")), tracker.update("He"));
        assertEquals(new Result(NO_THINKING, Optional.of("llo")), tracker.update("llo"));
    }
}
