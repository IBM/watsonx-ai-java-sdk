/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;

/**
 * Utility for applying client-side masking to text using {@link TextChatResponse#moderations()} results.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextChatResponse response = chatService.chat(request);
 * String content = response.toAssistantMessage().content();
 *
 * // Default: replace each matched range with asterisks of the same length.
 * String masked = Masker.mask(content, response);
 *
 * // Custom: replace each match with a label like "[PhoneNumber]".
 * String labelled = Masker.mask(content, response, m -> "[" + m.entity() + "]");
 *
 * // Mask and return an AssistantMessage directly.
 * AssistantMessage message = Masker.maskToMessage(response);
 *
 * // Custom masking returning an AssistantMessage.
 * AssistantMessage message = Masker.maskToMessage(response, m -> "[" + m.entity() + "]");
 * }</pre>
 */
public final class Masker {

    private Masker() {}

    /**
     * Masks the output moderation matches in the given content using asterisks ({@code *}) repeated for the length of each match.
     *
     * @param content the text to mask
     * @param response the {@link TextChatResponse} containing the moderation results
     * @return the masked content, or the original content if there is nothing to mask
     */
    public static String mask(String content, TextChatResponse response) {
        return mask(content, response, m -> "*".repeat(m.position().end() - m.position().start()));
    }

    /**
     * Masks the output moderation matches in the given content using a custom replacer.
     *
     * @param content the text to mask
     * @param response the {@link TextChatResponse} containing the moderation results
     * @param replacer a function that returns the replacement string for each matched {@link ModerationResult}
     * @return the masked content, or the original content if there is nothing to mask
     */
    public static String mask(String content, TextChatResponse response, Function<ModerationResult, String> replacer) {
        if (isNull(content) || isNull(response) || isNull(response.moderations()))
            return content;

        var matches = response.moderations().values().stream()
            .flatMap(List::stream)
            .filter(m -> !m.input() && nonNull(m.position()))
            .sorted(Comparator.comparingInt((ModerationResult m) -> m.position().start()).reversed())
            .toList();

        if (matches.isEmpty())
            return content;

        var sb = new StringBuilder(content);
        for (var m : matches) {
            int start = m.position().start();
            int end = m.position().end();
            if (start < 0 || end > sb.length() || start >= end)
                continue;
            sb.replace(start, end, replacer.apply(m));
        }
        return sb.toString();
    }

    /**
     * Masks the output moderation matches in the given response using asterisks ({@code *}) repeated for the length of each match, and returns the
     * result as an {@link AssistantMessage}.
     *
     * @param response the {@link TextChatResponse} to mask
     * @return an {@link AssistantMessage} with the masked content
     */
    public static AssistantMessage maskToMessage(TextChatResponse response) {
        return maskToMessage(response, m -> "*".repeat(m.position().end() - m.position().start()));
    }

    /**
     * Masks the output moderation matches in the given response using a custom replacer, and returns the result as an {@link AssistantMessage}.
     *
     * @param response the {@link TextChatResponse} to mask
     * @param replacer a function that returns the replacement string for each matched {@link ModerationResult}
     * @return an {@link AssistantMessage} with the masked content
     */
    public static AssistantMessage maskToMessage(TextChatResponse response, Function<ModerationResult, String> replacer) {
        var original = response.toAssistantMessage();
        var maskedContent = mask(original.content(), response, replacer);
        return new AssistantMessage(maskedContent, original.thinking(), original.name(), original.refusal(), original.toolCalls());
    }
}
