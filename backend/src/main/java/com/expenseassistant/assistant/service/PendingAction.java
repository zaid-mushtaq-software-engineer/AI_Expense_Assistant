package com.expenseassistant.assistant.service;

import java.time.Instant;
import java.util.UUID;

public record PendingAction(
        UUID id,
        Long userId,
        Long conversationId,
        PendingActionType type,
        Instant expiresAt,
        String summary,
        PendingActionPayload payload) {}