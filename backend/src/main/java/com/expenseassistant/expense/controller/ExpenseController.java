package com.expenseassistant.expense.controller;

import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.service.ExpenseService;
import com.expenseassistant.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseController(ExpenseService expenseService, CurrentUserProvider currentUserProvider) {
        this.expenseService = expenseService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        Long userId = currentUserProvider.requireUserId();
        ExpenseResponse body = expenseService.create(userId, request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/expenses/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }
}