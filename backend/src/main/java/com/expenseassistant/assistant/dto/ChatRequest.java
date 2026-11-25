package com.expenseassistant.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(Long conversationId, @NotBlank @Size(min = 1, max = 2000) String message) {}