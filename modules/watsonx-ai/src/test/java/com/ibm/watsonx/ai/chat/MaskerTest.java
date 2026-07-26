/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.ChatResponse.ModerationResult.Position;

public class MaskerTest {

    @Test
    void should_mask_single_output_match_with_asterisks() {
        var content = "Sure, your phone number is 3572865321.";
        var mod = new ModerationResult(0.8f, false, new Position(27, 37), "PhoneNumber", null);
        var response = ChatResponse.build().moderations(Map.of("pii", List.of(mod))).build();

        assertEquals("Sure, your phone number is **********.", Masker.mask(content, response));
    }

    @Test
    void should_mask_multiple_matches_in_correct_order() {
        var content = "Call 3572865321 or 5551234567 please.";
        var mod1 = new ModerationResult(0.9f, false, new Position(5, 15), "PhoneNumber", null);
        var mod2 = new ModerationResult(0.9f, false, new Position(19, 29), "PhoneNumber", null);
        var response = ChatResponse.build().moderations(Map.of("pii", List.of(mod1, mod2))).build();

        assertEquals("Call ********** or ********** please.", Masker.mask(content, response));
    }

    @Test
    void should_ignore_input_moderation_matches() {
        var content = "Sure, your phone number is 3572865321.";
        var mod = new ModerationResult(0.8f, true, new Position(27, 37), "PhoneNumber", null);
        var response = ChatResponse.build().moderations(Map.of("pii", List.of(mod))).build();

        assertEquals(content, Masker.mask(content, response));
    }

    @Test
    void should_use_custom_replacer_when_provided() {
        var content = "Sure, your phone number is 3572865321.";
        var mod = new ModerationResult(0.8f, false, new Position(27, 37), "PhoneNumber", null);
        var response = ChatResponse.build().moderations(Map.of("pii", List.of(mod))).build();

        assertEquals("Sure, your phone number is [PhoneNumber].",
            Masker.mask(content, response, m -> "[" + m.entity() + "]"));
    }

    @Test
    void should_return_original_content_when_no_moderations_present() {
        var content = "Nothing to mask here.";
        var response = ChatResponse.build().build();

        assertEquals(content, Masker.mask(content, response));
    }

    @Test
    void should_return_original_content_when_response_is_null() {
        var content = "Nothing to mask here.";
        assertEquals(content, Masker.mask(content, null));
    }

    @Test
    void should_return_null_when_content_is_null() {
        var response = ChatResponse.build().build();
        assertNull(Masker.mask(null, response));
    }

    @Test
    void should_skip_matches_with_invalid_positions() {
        var content = "Short text";
        var badMod = new ModerationResult(0.8f, false, new Position(50, 60), "PhoneNumber", null);
        var response = ChatResponse.build().moderations(Map.of("pii", List.of(badMod))).build();

        assertEquals(content, Masker.mask(content, response));
    }

    @Test
    void should_mask_matches_across_multiple_detectors() {
        var content = "You are stupid, phone: 3572865321.";
        var hap = new ModerationResult(0.9f, false, new Position(8, 14), "profanity", "stupid");
        var pii = new ModerationResult(0.8f, false, new Position(23, 33), "PhoneNumber", "3572865321");
        var response = ChatResponse.build()
            .moderations(Map.of("hap", List.of(hap), "pii", List.of(pii)))
            .build();

        assertEquals("You are ******, phone: **********.", Masker.mask(content, response));
    }
}
