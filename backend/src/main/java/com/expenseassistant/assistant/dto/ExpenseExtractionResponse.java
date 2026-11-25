package com.expenseassistant.assistant.dto;

import java.util.List;

public record ExpenseExtractionResponse(ExpenseDraft draft, List<String> missing, String question) {}