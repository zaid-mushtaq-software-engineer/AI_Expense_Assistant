package com.expenseassistant.assistant.dto;

import com.expenseassistant.conversation.dto.MessageResponse;
import java.util.UUID;

public record ActionResultResponse(UUID actionId, ActionResultStatus status, MessageResponse message) {}