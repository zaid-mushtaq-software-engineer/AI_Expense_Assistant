package com.expenseassistant.assistant.service;

public record DeleteExpensePayload(Long expenseId) implements PendingActionPayload {}