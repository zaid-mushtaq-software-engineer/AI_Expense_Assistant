package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expenseassistant.assistant.dto.ActionResultStatus;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.conversation.service.ConversationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantActionServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private PendingActionService pendingActionService;

    @Mock
    private ConversationService conversationService;

    private PendingActionFormatter formatter;
    private AssistantActionService service;

    @BeforeEach
    void setUp() {
        formatter = new PendingActionFormatter();
        service = new AssistantActionService(pendingActionService, formatter, conversationService);
    }

    @Test
    void confirmDelete_buildsMessage() {
        UUID id = UUID.randomUUID();
        String summary = "Delete ₹500 Dinner (FOOD) on 22 Nov 2026?";
        PendingAction action = new PendingAction(
                id,
                USER_ID,
                12L,
                PendingActionType.DELETE_EXPENSE,
                Instant.now(),
                summary,
                new DeleteExpensePayload(2L));
        when(pendingActionService.findOwned(USER_ID, id)).thenReturn(Optional.of(action));
        when(conversationService.addAssistantMessageInNewTransaction(
                        USER_ID, 12L, "Deleted: Delete ₹500 Dinner (FOOD) on 22 Nov 2026", null))
                .thenReturn(new MessageResponse(
                        9L,
                        MessageRole.ASSISTANT,
                        "Deleted: Delete ₹500 Dinner (FOOD) on 22 Nov 2026",
                        Instant.now(),
                        List.of()));

        var result = service.confirm(USER_ID, id);

        assertThat(result.status()).isEqualTo(ActionResultStatus.CONFIRMED);
        assertThat(result.message().content()).isEqualTo("Deleted: Delete ₹500 Dinner (FOOD) on 22 Nov 2026");
        verify(pendingActionService).confirm(USER_ID, id);
    }

    @Test
    void cancel_returnsCancelledMessage() {
        UUID id = UUID.randomUUID();
        PendingAction action = new PendingAction(
                id,
                USER_ID,
                12L,
                PendingActionType.DELETE_EXPENSE,
                Instant.now(),
                "summary",
                new DeleteExpensePayload(2L));
        when(pendingActionService.findOwned(USER_ID, id)).thenReturn(Optional.of(action));
        when(conversationService.addAssistantMessageInNewTransaction(
                        USER_ID, 12L, "Okay, I did not change anything.", null))
                .thenReturn(new MessageResponse(
                        10L,
                        MessageRole.ASSISTANT,
                        "Okay, I did not change anything.",
                        Instant.now(),
                        List.of()));

        var result = service.cancel(USER_ID, id);

        assertThat(result.status()).isEqualTo(ActionResultStatus.CANCELLED);
        assertThat(result.message().content()).isEqualTo("Okay, I did not change anything.");
        verify(pendingActionService).cancel(USER_ID, id);
    }
}