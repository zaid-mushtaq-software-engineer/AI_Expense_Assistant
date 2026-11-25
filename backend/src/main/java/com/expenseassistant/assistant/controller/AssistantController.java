package com.expenseassistant.assistant.controller;

import com.expenseassistant.assistant.dto.ActionResultResponse;
import com.expenseassistant.assistant.dto.ChatRequest;
import com.expenseassistant.assistant.dto.ChatResponse;
import com.expenseassistant.assistant.service.AssistantActionService;
import com.expenseassistant.assistant.dto.ExtractExpenseRequest;
import com.expenseassistant.assistant.dto.ExpenseExtractionResponse;
import com.expenseassistant.assistant.service.AssistantService;
import com.expenseassistant.assistant.service.ExpenseExtractionService;
import com.expenseassistant.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantActionService assistantActionService;
    private final ExpenseExtractionService expenseExtractionService;
    private final CurrentUserProvider currentUserProvider;

    public AssistantController(
            AssistantService assistantService,
            AssistantActionService assistantActionService,
            ExpenseExtractionService expenseExtractionService,
            CurrentUserProvider currentUserProvider) {
        this.assistantService = assistantService;
        this.assistantActionService = assistantActionService;
        this.expenseExtractionService = expenseExtractionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        Long userId = currentUserProvider.requireUserId();
        return assistantService.chat(userId, request);
    }

    @PostMapping("/extract")
    public ExpenseExtractionResponse extract(@Valid @RequestBody ExtractExpenseRequest request) {
        Long userId = currentUserProvider.requireUserId();
        return expenseExtractionService.extract(userId, request.message());
    }

    @PostMapping("/actions/{actionId}/confirm")
    public ActionResultResponse confirmAction(@PathVariable UUID actionId) {
        Long userId = currentUserProvider.requireUserId();
        return assistantActionService.confirm(userId, actionId);
    }

    @PostMapping("/actions/{actionId}/cancel")
    public ActionResultResponse cancelAction(@PathVariable UUID actionId) {
        Long userId = currentUserProvider.requireUserId();
        return assistantActionService.cancel(userId, actionId);
    }
}