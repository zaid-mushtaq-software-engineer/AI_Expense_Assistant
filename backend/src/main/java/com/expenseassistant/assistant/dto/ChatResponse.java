package com.expenseassistant.assistant.dto;

import com.expenseassistant.conversation.dto.MessageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        Long conversationId,
        String conversationTitle,
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        PendingActionResponse pendingAction) {}