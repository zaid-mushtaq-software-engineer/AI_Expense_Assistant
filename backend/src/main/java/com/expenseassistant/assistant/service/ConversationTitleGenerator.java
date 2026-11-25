package com.expenseassistant.assistant.service;

import com.expenseassistant.conversation.service.ConversationService;
import org.springframework.util.StringUtils;

public final class ConversationTitleGenerator {

    private static final int MAX_TITLE_LENGTH = 50;

    private ConversationTitleGenerator() {}

    public static String fromFirstMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return ConversationService.PLACEHOLDER_TITLE;
        }
        String trimmed = message.strip();
        if (trimmed.length() <= MAX_TITLE_LENGTH) {
            return trimmed;
        }
        int cut = MAX_TITLE_LENGTH;
        int lastSpace = trimmed.lastIndexOf(' ', MAX_TITLE_LENGTH);
        if (lastSpace > 20) {
            cut = lastSpace;
        }
        return trimmed.substring(0, cut).stripTrailing();
    }
}