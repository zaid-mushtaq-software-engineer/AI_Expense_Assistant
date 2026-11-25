package com.expenseassistant.assistant.dto;

import com.expenseassistant.assistant.service.PendingActionType;
import java.time.Instant;
import java.util.UUID;

public record PendingActionResponse(UUID actionId, PendingActionType type, String summary, Instant expiresAt) {}