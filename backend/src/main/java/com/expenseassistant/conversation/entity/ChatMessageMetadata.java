package com.expenseassistant.conversation.entity;

import java.util.List;

public record ChatMessageMetadata(
        List<String> toolsUsed,
        String pendingActionId,
        List<String> toolErrors) {

    public ChatMessageMetadata {
        toolsUsed = toolsUsed == null ? List.of() : List.copyOf(toolsUsed);
        toolErrors = toolErrors == null ? List.of() : List.copyOf(toolErrors);
    }

    public static ChatMessageMetadata ofTools(List<String> toolsUsed) {
        return new ChatMessageMetadata(toolsUsed, null, List.of());
    }

    public static ChatMessageMetadata ofToolsAndPending(List<String> toolsUsed, String pendingActionId) {
        return new ChatMessageMetadata(toolsUsed, pendingActionId, List.of());
    }
}