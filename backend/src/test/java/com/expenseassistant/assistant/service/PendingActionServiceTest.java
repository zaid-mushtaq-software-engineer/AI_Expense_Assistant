package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.expenseassistant.common.exception.PendingActionExpiredException;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingActionServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER = 2L;

    @Mock
    private ExpenseService expenseService;

    private PendingActionProperties properties;
    private PendingActionService service;

    @BeforeEach
    void setUp() {
        properties = new PendingActionProperties();
        properties.setTtlMinutes(10);
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
        service = new PendingActionService(expenseService, properties, clock);
    }

    @Test
    void createConfirmDelete_callsExpenseServiceOnce() {
        UUID id =
                service.create(USER_ID, null, PendingActionType.DELETE_EXPENSE, new DeleteExpensePayload(42L), "Delete?")
                        .id();

        service.confirm(USER_ID, id);

        verify(expenseService).delete(USER_ID, 42L);
        assertThatThrownBy(() -> service.confirm(USER_ID, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createConfirmUpdate_callsExpenseService() {
        UpdateExpenseRequest request =
                new UpdateExpenseRequest(new BigDecimal("650"), ExpenseCategory.FOOD, "Dinner", LocalDate.of(2026, 11, 24));
        UUID id = service.create(USER_ID, null, PendingActionType.UPDATE_EXPENSE, new UpdateExpensePayload(7L, request), "Update?")
                .id();

        service.confirm(USER_ID, id);

        verify(expenseService).update(USER_ID, 7L, request);
    }

    @Test
    void confirm_wrongUser_notFound() {
        UUID id = service.create(USER_ID, null, PendingActionType.DELETE_EXPENSE, new DeleteExpensePayload(1L), "Delete?")
                .id();

        assertThatThrownBy(() -> service.confirm(OTHER_USER, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(expenseService, never()).delete(OTHER_USER, 1L);
    }

    @Test
    void confirm_expired_raises410() {
        properties.setTtlMinutes(0);
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
        service = new PendingActionService(expenseService, properties, clock);

        UUID id = service.create(USER_ID, null, PendingActionType.DELETE_EXPENSE, new DeleteExpensePayload(5L), "Delete?")
                .id();

        assertThatThrownBy(() -> service.confirm(USER_ID, id))
                .isInstanceOf(PendingActionExpiredException.class);
        verify(expenseService, never()).delete(USER_ID, 5L);
    }

    @Test
    void cancel_removesWithoutServiceCall() {
        UUID id = service.create(USER_ID, null, PendingActionType.DELETE_EXPENSE, new DeleteExpensePayload(3L), "Delete?")
                .id();

        service.cancel(USER_ID, id);

        verify(expenseService, never()).delete(USER_ID, 3L);
        assertThatThrownBy(() -> service.confirm(USER_ID, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_wrongUser_notFound() {
        UUID id = service.create(USER_ID, null, PendingActionType.DELETE_EXPENSE, new DeleteExpensePayload(8L), "Delete?")
                .id();

        assertThatThrownBy(() -> service.cancel(OTHER_USER, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}