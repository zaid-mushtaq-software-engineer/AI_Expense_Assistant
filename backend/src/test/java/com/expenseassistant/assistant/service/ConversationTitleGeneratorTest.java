package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationTitleGeneratorTest {

    @Test
    void shortMessage_usesFullText() {
        assertThat(ConversationTitleGenerator.fromFirstMessage("Dinner expense"))
                .isEqualTo("Dinner expense");
    }

    @Test
    void longMessage_truncatesAtWordBoundary() {
        String longMessage =
                "I spent a lot on dinner yesterday and want to track it carefully in the app";
        String title = ConversationTitleGenerator.fromFirstMessage(longMessage);
        assertThat(title.length()).isLessThanOrEqualTo(50);
        assertThat(title).doesNotEndWith(" ");
    }
}