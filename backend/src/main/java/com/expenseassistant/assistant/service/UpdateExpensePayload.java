package com.expenseassistant.assistant.service;

import com.expenseassistant.expense.dto.UpdateExpenseRequest;

public record UpdateExpensePayload(Long expenseId, UpdateExpenseRequest request) implements PendingActionPayload {}