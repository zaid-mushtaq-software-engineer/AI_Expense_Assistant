package com.expenseassistant.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractExpenseRequest(@NotBlank @Size(min = 1, max = 2000) String message) {}