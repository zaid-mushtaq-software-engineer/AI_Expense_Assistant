package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.ActionResultResponse;
import com.expenseassistant.assistant.dto.ActionResultStatus;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.conversation.service.ConversationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class AssistantActionService {

    private static final Logger log = LoggerFactory.getLogger(AssistantActionService.class);

    private final PendingActionService pendingActionService;
    private final PendingActionFormatter formatter;
    private final ConversationService conversationService;

    public AssistantActionService(
            PendingActionService pendingActionService,
            PendingActionFormatter formatter,
            ConversationService conversationService) {
        this.pendingActionService = pendingActionService;
        this.formatter = formatter;
        this.conversationService = conversationService;
    }

    public ActionResultResponse confirm(Long userId, UUID actionId) {
        PendingAction action = pendingActionService
                .findOwned(userId, actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found"));
        String summary = action.summary();
        pendingActionService.confirm(userId, actionId);
        String content = switch (action.type()) {
            case DELETE_EXPENSE -> formatter.confirmDeleteMessage(summary);
            case UPDATE_EXPENSE -> formatter.confirmUpdateMessage(summary);
        };
        MessageResponse message = persistResultMessage(userId, action, content);
        return new ActionResultResponse(actionId, ActionResultStatus.CONFIRMED, message);
    }

    public ActionResultResponse cancel(Long userId, UUID actionId) {
        PendingAction action = pendingActionService
                .findOwned(userId, actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found"));
        pendingActionService.cancel(userId, actionId);
        MessageResponse message =
                persistResultMessage(userId, action, "Okay, I did not change anything.");
        return new ActionResultResponse(actionId, ActionResultStatus.CANCELLED, message);
    }

    private MessageResponse persistResultMessage(Long userId, PendingAction action, String content) {
        Long conversationId = action.conversationId();
        if (conversationId == null) {
            log.warn("Pending action {} has no conversationId; result not saved to history", action.id());
            return new MessageResponse(null, MessageRole.ASSISTANT, content, Instant.now(), List.of());
        }
        return conversationService.addAssistantMessageInNewTransaction(userId, conversationId, content, null);
    }
}