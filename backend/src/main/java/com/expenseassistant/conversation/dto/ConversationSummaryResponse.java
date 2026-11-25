package com.expenseassistant.conversation.dto;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long id, String title, Instant createdAt, Instant updatedAt, String lastMessagePreview) {}