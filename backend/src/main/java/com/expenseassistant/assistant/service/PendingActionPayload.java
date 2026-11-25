package com.expenseassistant.assistant.service;

public sealed interface PendingActionPayload permits DeleteExpensePayload, UpdateExpensePayload {}