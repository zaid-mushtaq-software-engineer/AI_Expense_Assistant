package com.expenseassistant.conversation.mapper;

import com.expenseassistant.conversation.dto.ConversationDetailResponse;
import com.expenseassistant.conversation.dto.ConversationSummaryResponse;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.ChatMessage;
import com.expenseassistant.conversation.entity.ChatMessageMetadata;
import com.expenseassistant.conversation.entity.Conversation;
import com.expenseassistant.conversation.entity.MessageRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    private static final int PREVIEW_MAX_LENGTH = 80;

    public ConversationSummaryResponse toSummary(Conversation conversation, String lastMessagePreview) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                lastMessagePreview);
    }

    public ConversationDetailResponse toDetail(Conversation conversation, long messageCount) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageCount);
    }

    public MessageResponse toMessageResponse(ChatMessage message) {
        List<String> toolsUsed = List.of();
        if (message.getRole() == MessageRole.ASSISTANT && message.getMetadata() != null) {
            toolsUsed = message.getMetadata().toolsUsed();
        }
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                toolsUsed);
    }

    public static String previewContent(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        if (trimmed.length() <= PREVIEW_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_MAX_LENGTH).stripTrailing() + "…";
    }

    public static ChatMessageMetadata buildAssistantMetadata(
            List<String> toolsUsed, String pendingActionId, List<String> toolErrors) {
        if ((toolsUsed == null || toolsUsed.isEmpty())
                && pendingActionId == null
                && (toolErrors == null || toolErrors.isEmpty())) {
            return null;
        }
        return new ChatMessageMetadata(
                toolsUsed == null ? List.of() : toolsUsed,
                pendingActionId,
                toolErrors == null ? List.of() : toolErrors);
    }
}