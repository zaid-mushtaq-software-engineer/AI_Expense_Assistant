package com.expenseassistant.assistant.service;

import java.time.Instant;
import java.util.UUID;

public record CreatedPendingAction(UUID id, Instant expiresAt, PendingActionType type, String summary) {}