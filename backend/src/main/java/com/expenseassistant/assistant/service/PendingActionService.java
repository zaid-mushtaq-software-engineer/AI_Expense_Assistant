package com.expenseassistant.assistant.service;

import com.expenseassistant.common.exception.PendingActionExpiredException;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.expense.service.ExpenseService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PendingActionService {

    private final ConcurrentHashMap<UUID, PendingAction> pending = new ConcurrentHashMap<>();
    private final ExpenseService expenseService;
    private final PendingActionProperties properties;
    private final Clock clock;

    public PendingActionService(
            ExpenseService expenseService, PendingActionProperties properties, Clock clock) {
        this.expenseService = expenseService;
        this.properties = properties;
        this.clock = clock;
    }

    public CreatedPendingAction create(
            Long userId,
            Long conversationId,
            PendingActionType type,
            PendingActionPayload payload,
            String summary) {
        UUID id = UUID.randomUUID();
        Instant expiresAt = clock.instant().plus(properties.getTtlMinutes(), ChronoUnit.MINUTES);
        pending.put(id, new PendingAction(id, userId, conversationId, type, expiresAt, summary, payload));
        return new CreatedPendingAction(id, expiresAt, type, summary);
    }

    public Optional<PendingAction> findOwned(Long userId, UUID actionId) {
        PendingAction action = pending.get(actionId);
        if (action == null || !action.userId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(action);
    }

    public void confirm(Long userId, UUID actionId) {
        PendingAction action = requireOwned(userId, actionId);
        if (isExpired(action)) {
            pending.remove(actionId);
            throw new PendingActionExpiredException();
        }
        pending.remove(actionId);
        dispatch(action, userId);
    }

    public void cancel(Long userId, UUID actionId) {
        PendingAction action = pending.get(actionId);
        if (action == null || !action.userId().equals(userId)) {
            throw new ResourceNotFoundException("Pending action not found");
        }
        pending.remove(actionId);
    }

    @Scheduled(fixedRate = 60_000)
    void cleanupExpired() {
        Instant now = clock.instant();
        pending.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private PendingAction requireOwned(Long userId, UUID actionId) {
        PendingAction action = pending.get(actionId);
        if (action == null || !action.userId().equals(userId)) {
            throw new ResourceNotFoundException("Pending action not found");
        }
        return action;
    }

    private boolean isExpired(PendingAction action) {
        return !action.expiresAt().isAfter(clock.instant());
    }

    private void dispatch(PendingAction action, Long userId) {
        switch (action.type()) {
            case DELETE_EXPENSE -> {
                DeleteExpensePayload payload = (DeleteExpensePayload) action.payload();
                expenseService.delete(userId, payload.expenseId());
            }
            case UPDATE_EXPENSE -> {
                UpdateExpensePayload payload = (UpdateExpensePayload) action.payload();
                expenseService.update(userId, payload.expenseId(), payload.request());
            }
        }
    }
}