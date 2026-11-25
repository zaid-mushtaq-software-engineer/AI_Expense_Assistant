package com.expenseassistant.conversation.dto;

import com.expenseassistant.conversation.entity.MessageRole;
import java.time.Instant;
import java.util.List;

public record MessageResponse(
        Long id, MessageRole role, String content, Instant createdAt, List<String> toolsUsed) {

    public MessageResponse {
        toolsUsed = toolsUsed == null ? List.of() : List.copyOf(toolsUsed);
    }
}